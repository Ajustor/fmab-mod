package com.ajustor.fmab.client;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.item.InscriptionItem;
import com.ajustor.fmab.transmutation.GreatCircle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * L'aide au placement d'un cercle géant. Tant que le joueur tient de quoi tracer (craie, peinture,
 * burin) et que le cercle choisi dans son carnet est déjà tracé à proximité, le plan du cercle géant
 * s'affiche : le tour prévu, les emplacements des relais (ceux qui restent à tracer luisent et lancent
 * une colonne de lumière pour qu'on les trouve de loin), et, au-dessus de la barre d'action, combien
 * il en reste et où est le plus proche.
 *
 * <p>Le plan se calcule sur ce que le client voit (voir {@link GreatCircle#plan}) ; c'est le serveur
 * qui décide, à l'activation, si le cercle géant tient.
 */
public final class GreatCirclePlanner {
	/** Le plan se recalcule toutes les demi-secondes. */
	private static final int EVERY = 10;

	private static GreatCircle.@Nullable Plan current;

	private GreatCirclePlanner() {
	}

	public static void register() {
		com.ajustor.fmab.platform.client.ClientEvents.END_TICK.register(GreatCirclePlanner::tick);
	}

	/** Le plan dont ce cercle est le centre, s'il y en a un à montrer. */
	public static GreatCircle.@Nullable Plan centeredOn(BlockPos circle) {
		GreatCircle.Plan plan = current;
		return plan != null && plan.center().equals(circle) ? plan : null;
	}

	private static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || player.tickCount % EVERY != 0) {
			if (player == null) {
				current = null;
			}
			return;
		}
		boolean drawing = player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof InscriptionItem
				|| player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof InscriptionItem;
		Drawing selected = drawing ? Notebooks.selected(player) : Drawing.EMPTY;
		if (selected.isEmpty()) {
			current = null;
			return;
		}
		List<BlockPos> copies = GreatCircle.copies(mc.level, player.blockPosition(), selected, GreatCircle.MAX_RADIUS);
		// Tout près d'un cercle sans relais, on trace un cercle ordinaire : rien à montrer.
		Optional<GreatCircle.Plan> plan = GreatCircle.plan(copies, p -> mc.level.getBlockEntity(p)
						instanceof TransmutationCircleBlockEntity circle ? circle.drawnAt() : 0, player.blockPosition())
				.filter(p -> p.radius() > 0);
		current = plan.orElse(null);
		plan.ifPresent(p -> player.sendOverlayMessage(message(p, player.blockPosition())));
	}

	/** Où en est le cercle géant, et où tracer le prochain relais. */
	static Component message(GreatCircle.Plan plan, BlockPos player) {
		int radius = (int) Math.round(plan.radius());
		if (plan.relays().isEmpty()) {
			return Component.translatable("great_circle.fmab.plan.first", radius);
		}
		Optional<GreatCircle.Spot> next = plan.next(player);
		if (plan.ready()) {
			return Component.translatable("great_circle.fmab.plan.ready", plan.relays().size(), radius);
		}
		if (next.isEmpty()) {
			return Component.translatable("great_circle.fmab.plan.uneven", plan.relays().size(), radius);
		}
		BlockPos at = next.get().pos();
		int distance = (int) Math.round(GreatCircle.distance(player, at));
		return Component.translatable("great_circle.fmab.plan.next", plan.relays().size(),
				GreatCircle.PLANNED_RELAYS, radius, distance, compass(player, at));
	}

	/** Le point cardinal où se trouve {@code to}, vu de {@code from}. */
	static Component compass(BlockPos from, BlockPos to) {
		double angle = Math.toDegrees(Math.atan2(to.getX() - from.getX(), from.getZ() - to.getZ()));
		String[] names = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};
		int index = Math.floorMod((int) Math.round(angle / 45), 8);
		return Component.translatable("great_circle.fmab.compass." + names[index]);
	}
}
