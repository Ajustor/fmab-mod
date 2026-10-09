package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * La concentration se recharge au repos : lentement en marchant, vite accroupi et immobile.
 * Mise à jour une fois par seconde, ce qui limite aussi la synchronisation.
 */
public final class Concentration {
	private static final float PER_SECOND = 0.5f;
	private static final float RESTING_PER_SECOND = 2f;

	private Concentration() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				AlchemistData data = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
				if (data.concentration() >= data.maxConcentration()) {
					continue;
				}
				boolean resting = player.isShiftKeyDown() && player.getDeltaMovement().horizontalDistanceSqr() < 1e-4;
				player.setAttached(FmabAttachments.ALCHEMIST,
						data.withConcentration(data.concentration() + (resting ? RESTING_PER_SECOND : PER_SECOND)));
			}
		});
	}
}
