package com.ajustor.fmab.gate;

import com.ajustor.fmab.item.PhilosopherStoneItem;
import com.ajustor.fmab.network.StoneChoicePayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.stone.PhilosopherStones;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Une transmutation humaine, Pierre philosophale en main : le cercle attend que l'alchimiste
 * choisisse. Ouvrir sa Porte, ou donner une âme de la Pierre à l'être qui naît du cercle.
 */
public final class StoneChoice {
	public static final String GATE = "gate";
	public static final String BEING = "being";
	/** Le cercle n'attend pas plus d'une minute. */
	private static final int PATIENCE = 20 * 60;
	/** Il faut rester près de son cercle pour choisir. */
	private static final double REACH = 8;

	private record Pending(ResourceKey<Level> dimension, BlockPos circle, long since) {
	}

	private static final Map<UUID, Pending> PENDING = new HashMap<>();

	private StoneChoice() {
	}

	/** Le cercle s'est éveillé : l'alchimiste a une minute pour choisir. */
	static void ask(ServerPlayer caster, BlockPos circle) {
		ServerLevel level = caster.level();
		PENDING.put(caster.getUUID(), new Pending(level.dimension(), circle.immutable(), level.getGameTime()));
		int souls = PhilosopherStones.held(caster).map(PhilosopherStoneItem::souls).orElse(0);
		ServerPlayNetworking.send(caster, new StoneChoicePayload(souls));
	}

	/** Ce que l'alchimiste a choisi ; tout autre réponse (l'écran fermé) laisse le cercle s'éteindre. */
	public static void answer(ServerPlayer player, String choice) {
		Pending pending = PENDING.remove(player.getUUID());
		ServerLevel level = player.level();
		if (pending == null || level.dimension() != pending.dimension()
				|| level.getGameTime() - pending.since() > PATIENCE
				|| player.position().distanceTo(Vec3.atCenterOf(pending.circle())) > REACH
				|| player.getAttachedOrCreate(FmabAttachments.GATE).visit().isPresent()) {
			if (GATE.equals(choice) || BEING.equals(choice)) {
				player.sendOverlayMessage(Component.translatable("gate.fmab.choice.too_late"));
			}
			return;
		}
		switch (choice) {
			case GATE -> HumanTransmutation.openGate(player, level, pending.circle());
			case BEING -> HumanTransmutation.createBeing(player, level, pending.circle());
			default -> player.sendOverlayMessage(Component.translatable("gate.fmab.choice.declined"));
		}
	}
}
