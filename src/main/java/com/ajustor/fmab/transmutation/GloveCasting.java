package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.item.GloveItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Lancer le cercle d'un gant : sur la surface visée, à portée de main (ou à distance pour les gants
 * à silex), comme si on l'y avait tracé. Il faut avoir la main libre.
 */
public final class GloveCasting {
	/** Un rebond abîme le gant bien plus qu'un usage normal. */
	private static final int REBOUND_WEAR = 8;
	/** Délai entre deux frappes de gantelet, en ticks. */
	private static final int STRIKE_COOLDOWN = 20;

	private GloveCasting() {
	}

	/**
	 * @param combine joindre les mains : les cercles des deux gants agissent l'un après l'autre
	 */
	public static void cast(ServerPlayer player, boolean combine) {
		if (!player.getMainHandItem().isEmpty()) {
			player.sendOverlayMessage(Component.translatable("transmutation.fmab.hand_not_free"));
			return;
		}
		Gloves gloves = player.getAttachedOrCreate(FmabAttachments.GLOVES);
		boolean right = inscribed(gloves.right());
		boolean left = inscribed(gloves.left());
		if (!right && !left) {
			player.sendOverlayMessage(Component.translatable("transmutation.fmab.no_glove_circle"));
			return;
		}
		// La main droite d'abord ; la gauche suit quand on joint les mains.
		if (!castWith(player, gloves, !right, null, null) || !combine || !right || !left) {
			return;
		}
		castWith(player, player.getAttachedOrCreate(FmabAttachments.GLOVES), true, null, null);
	}

	/**
	 * Le joueur frappe-t-il avec un gantelet gravé, main libre ? Valable des deux côtés : le client
	 * doit lui aussi renoncer à casser le bloc.
	 */
	public static boolean canStrike(Player player) {
		return strikingHand(player) != null;
	}

	/** Un gantelet frappe un bloc : son cercle s'applique sur la face frappée. */
	public static void strike(ServerPlayer player, BlockPos pos, Direction face) {
		Boolean left = strikingHand(player);
		if (left == null) {
			return;
		}
		Gloves gloves = player.getAttachedOrCreate(FmabAttachments.GLOVES);
		ItemStack glove = gloves.get(left);
		if (player.getCooldowns().isOnCooldown(glove)) {
			return;
		}
		player.getCooldowns().addCooldown(glove, STRIKE_COOLDOWN);
		castWith(player, gloves, left, pos, face);
	}

	/** Main du gantelet gravé (vrai pour la gauche), ou null s'il n'y en a pas ou que la main est prise. */
	private static Boolean strikingHand(Player player) {
		if (!player.getMainHandItem().isEmpty()) {
			return null;
		}
		Gloves gloves = player.getAttached(FmabAttachments.GLOVES);
		if (gloves == null) {
			return null;
		}
		for (boolean left : new boolean[]{false, true}) {
			ItemStack glove = gloves.get(left);
			if (glove.getItem() instanceof GloveItem item && item.kind().striking() && inscribed(glove)) {
				return left;
			}
		}
		return null;
	}

	/**
	 * @return vrai si l'énergie est partie (le second gant d'une combinaison peut suivre)
	 */
	private static boolean castWith(ServerPlayer player, Gloves gloves, boolean left, BlockPos target, Direction face) {
		ItemStack glove = gloves.get(left);
		if (!(glove.getItem() instanceof GloveItem item)) {
			return false;
		}
		ServerLevel level = player.level();
		if (target == null) {
			double reach = item.kind().reach() > 0 ? item.kind().reach() : player.blockInteractionRange();
			HitResult hit = player.pick(reach, 1, false);
			if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) {
				player.sendOverlayMessage(Component.translatable("transmutation.fmab.no_surface"));
				return false;
			}
			target = block.getBlockPos();
			face = block.getDirection();
		}
		if (item.kind().reach() > 0) {
			// Le claquement de doigts : l'étincelle part du gant.
			level.playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1, 1.4f);
		}
		Drawing drawing = glove.get(FmabComponents.GLOVE_CIRCLE);
		Transmutation.Result result = Transmutation.activate(level, target.relative(face),
				CircleFrame.forFace(face, player.getDirection()), drawing, player, false, item.kind().stages());
		int wear = switch (result) {
			case INERT, TIRED -> 0;
			case REBOUND -> REBOUND_WEAR;
			case NOTHING, DONE -> 1;
		};
		if (wear > 0 && !player.isCreative()) {
			ItemStack worn = glove.copy();
			worn.hurtAndBreak(wear, level, player, broken -> player.sendOverlayMessage(
					Component.translatable("transmutation.fmab.glove_broken")));
			Gloves now = player.getAttachedOrCreate(FmabAttachments.GLOVES);
			player.setAttached(FmabAttachments.GLOVES, now.with(left, worn.isEmpty() ? ItemStack.EMPTY : worn));
		}
		return result != Transmutation.Result.INERT && result != Transmutation.Result.TIRED;
	}

	private static boolean inscribed(ItemStack glove) {
		return glove.getItem() instanceof GloveItem && glove.has(FmabComponents.GLOVE_CIRCLE);
	}
}
