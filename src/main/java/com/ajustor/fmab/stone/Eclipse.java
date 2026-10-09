package com.ajustor.fmab.stone;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * L'éclipse, rappel du Jour promis : tous les huit jours, vers midi, la lune passe devant le soleil.
 * C'est le seul moment où l'on peut devenir une Pierre philosophale vivante.
 */
public final class Eclipse {
	public static final int PERIOD_DAYS = 8;
	/** De onze heures à quatorze heures, à peu près (midi = 6000). */
	private static final int START = 5000;
	private static final int END = 8000;

	private Eclipse() {
	}

	/** L'éclipse a-t-elle lieu en ce moment dans ce monde (il faut un soleil : l'Overworld) ? */
	public static boolean now(Level level) {
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}
		return at(level.getOverworldClockTime());
	}

	/** L'éclipse a-t-elle lieu à cette heure de l'horloge du monde (en ticks depuis le premier jour) ? */
	public static boolean at(long time) {
		long day = Math.floorDiv(time, 24000L);
		long hour = Math.floorMod(time, 24000L);
		return Math.floorMod(day, PERIOD_DAYS) == PERIOD_DAYS - 1 && hour >= START && hour < END;
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ServerLevel overworld = server.overworld();
			long hour = Math.floorMod(overworld.getOverworldClockTime(), 24000L);
			if (hour != START && hour != END) {
				return;
			}
			boolean starting = hour == START && now(overworld);
			boolean ending = hour == END && Math.floorMod(Math.floorDiv(overworld.getOverworldClockTime(), 24000L),
					PERIOD_DAYS) == PERIOD_DAYS - 1;
			if (!starting && !ending) {
				return;
			}
			for (ServerPlayer p : overworld.players()) {
				p.sendSystemMessage(Component.translatable(starting ? "eclipse.fmab.begins" : "eclipse.fmab.ends"));
			}
		});
	}
}
