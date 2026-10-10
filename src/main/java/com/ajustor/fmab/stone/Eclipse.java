package com.ajustor.fmab.stone;

import com.ajustor.fmab.network.CinematicPayload;
import com.ajustor.fmab.promised.NationalCircle;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
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
	/** La montée et la descente de l'éclipse, en ticks. */
	private static final int RAMP = 200;
	/**
	 * L'instant de la dernière annonce : un cycle jour/nuit figé pile à cette heure ne la répète pas
	 * à chaque tick.
	 */
	private static long announced = Long.MIN_VALUE;

	private Eclipse() {
	}

	/** L'éclipse a-t-elle lieu en ce moment dans ce monde (il faut un soleil : l'Overworld) ? */
	public static boolean now(Level level) {
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}
		return at(level.getOverworldClockTime());
	}

	/**
	 * L'intensité de l'éclipse, de 0 à 1 : elle monte pendant ses dix premières secondes et
	 * redescend pendant ses dix dernières (pour que le ciel s'assombrisse en douceur).
	 */
	public static float strength(Level level) {
		if (!now(level)) {
			return 0;
		}
		long hour = Math.floorMod(level.getOverworldClockTime(), 24000L);
		return Math.min(1, Math.min(hour - START, END - hour) / (float) RAMP);
	}

	/** L'éclipse a-t-elle lieu à cette heure de l'horloge du monde (en ticks depuis le premier jour) ? */
	public static boolean at(long time) {
		long day = Math.floorDiv(time, 24000L);
		long hour = Math.floorMod(time, 24000L);
		return Math.floorMod(day, PERIOD_DAYS) == PERIOD_DAYS - 1 && hour >= START && hour < END;
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> announced = Long.MIN_VALUE);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ServerLevel overworld = server.overworld();
			long clock = overworld.getOverworldClockTime();
			if (clock == announced) {
				return;
			}
			long hour = Math.floorMod(clock, 24000L);
			if (hour == 0 && Math.floorMod(Math.floorDiv(overworld.getOverworldClockTime(), 24000L), PERIOD_DAYS)
					== PERIOD_DAYS - 1) {
				// Le matin du jour de l'éclipse, on le sent venir.
				announced = clock;
				for (ServerPlayer p : overworld.players()) {
					p.sendSystemMessage(Component.translatable("eclipse.fmab.today").withStyle(ChatFormatting.GOLD));
				}
				return;
			}
			if (hour != START && hour != END) {
				return;
			}
			boolean starting = hour == START && now(overworld);
			boolean ending = hour == END && Math.floorMod(Math.floorDiv(overworld.getOverworldClockTime(), 24000L),
					PERIOD_DAYS) == PERIOD_DAYS - 1;
			if (!starting && !ending) {
				return;
			}
			announced = clock;
			NationalCircle circle = NationalCircle.get(server);
			for (ServerPlayer p : overworld.players()) {
				p.sendSystemMessage(Component.translatable(starting ? "eclipse.fmab.begins" : "eclipse.fmab.ends"));
				if (starting && !circle.broken() && !circle.fatherFallen()) {
					// Le Jour promis : le cercle national s'éveille, et Père avec lui.
					p.sendSystemMessage(Component.translatable("promised.fmab.day_begins",
							NationalCircle.POINTS - circle.sealedCount()).withStyle(ChatFormatting.DARK_RED));
					CinematicPayload.play(p, CinematicPayload.PROMISED_DAY, 120);
				}
			}
		});
	}
}
