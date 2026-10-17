package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.ajustor.fmab.platform.event.ServerEvents;

/**
 * La concentration de l'alchimiste : sa réserve d'énergie, de 0 au maximum de son rang. Elle se
 * recharge au repos : lentement en marchant, vite accroupi et immobile ; mise à jour une fois par
 * seconde. Elle a son propre attachement ({@link FmabAttachments#CONCENTRATION}) : la recharge
 * n'envoie au client que ce nombre, pas tout ce que l'alchimiste sait.
 */
public final class Concentration {
	private static final float PER_SECOND = 0.5f;
	private static final float RESTING_PER_SECOND = 2f;

	private Concentration() {
	}

	/** Le maximum, selon le rang. */
	public static float max(Player player) {
		AlchemistData data = FmabAttachments.ALCHEMIST.get(player);
		return (data == null ? AlchemistData.NEW : data).maxConcentration();
	}

	/** La réserve du joueur ; pleine s'il n'a encore rien dépensé, jamais au-delà du maximum de son rang. */
	public static float of(Player player) {
		Float value = FmabAttachments.CONCENTRATION.get(player);
		float max = max(player);
		return value == null ? max : Math.min(value, max);
	}

	/** Pose la réserve, gardée entre 0 et le maximum. */
	public static void set(Player player, float value) {
		FmabAttachments.CONCENTRATION.set(player, Math.clamp(value, 0, max(player)));
	}

	public static void register() {
		ServerEvents.END_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				float now = of(player);
				if (now >= max(player)) {
					continue;
				}
				boolean resting = player.isShiftKeyDown() && player.getDeltaMovement().horizontalDistanceSqr() < 1e-4;
				set(player, now + (resting ? RESTING_PER_SECOND : PER_SECOND));
			}
		});
	}
}
