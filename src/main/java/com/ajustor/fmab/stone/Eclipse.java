package com.ajustor.fmab.stone;

import com.ajustor.fmab.network.CinematicPayload;
import com.ajustor.fmab.promised.NationalCircle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import com.ajustor.fmab.platform.event.ServerEvents;

/**
 * L'éclipse, rappel du Jour promis : tous les huit jours, vers midi, la lune passe devant le soleil.
 * C'est le seul moment où l'on peut devenir une Pierre philosophale vivante.
 *
 * <p>Le jour de l'éclipse, la lune est dans le ciel dès le lever : elle attend le soleil sur sa route,
 * s'en approche toute la matinée, le mord vers dix heures, le couvre de onze heures à quatorze heures,
 * puis ressort de l'autre côté et s'en éloigne jusqu'au coucher ({@link #separation}, {@link #coverage}).
 */
public final class Eclipse {
	public static final int PERIOD_DAYS = 8;
	/** De onze heures à quatorze heures, à peu près (midi = 6000). */
	private static final int START = 5000;
	private static final int END = 8000;
	/** Le coucher du soleil, puis celui de la lune de l'éclipse, qui le suit de 24° sur sa route. */
	private static final int SUNSET = 12000;
	private static final int MOONSET = 14600;
	/** L'avance de la lune sur le soleil au lever, et son retard au coucher, en radians (24°). */
	public static final float DAWN_SEPARATION = (float) Math.toRadians(24);
	/**
	 * Les demi-largeurs du soleil et de la lune vus du sol, en radians : le cœur carré du soleil (8 pixels
	 * de sa texture de 32, sur un carré de 30 à 100 blocs) et la lune de l'éclipse, un peu plus grande
	 * pour le couvrir tout entier.
	 */
	public static final float SUN_HALF = (float) Math.atan(3.75 / 100);
	public static final float MOON_HALF = (float) Math.atan(4.5 / 100);
	/** L'heure de la dernière annonce : un cycle jour/nuit figé pile à cette heure ne la répète pas à chaque tick. */
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
	 * L'intensité de l'éclipse, de 0 à 1 : elle suit la part du soleil que la lune couvre, et ne
	 * s'assombrit vraiment qu'aux derniers instants avant la totalité, comme une vraie éclipse.
	 */
	public static float strength(Level level) {
		if (level.dimension() != Level.OVERWORLD) {
			return 0;
		}
		float covered = coverage(level.getOverworldClockTime());
		return covered * covered;
	}

	/** Est-ce le jour de l'éclipse (à n'importe quelle heure) ? */
	public static boolean day(long time) {
		return Math.floorMod(Math.floorDiv(time, 24000L), PERIOD_DAYS) == PERIOD_DAYS - 1;
	}

	/**
	 * L'avance de la lune sur le soleil, le long de sa route dans le ciel, en radians : positive le
	 * matin (la lune est devant, là où le soleil va), nulle pendant la totalité, négative l'après-midi.
	 * {@link Float#NaN} hors du jour de l'éclipse et une fois la lune couchée : c'est alors la lune
	 * ordinaire.
	 */
	public static float separation(long time) {
		long hour = Math.floorMod(time, 24000L);
		if (!day(time) || hour > MOONSET) {
			return Float.NaN;
		}
		if (hour < START) {
			return DAWN_SEPARATION * (START - hour) / START;
		}
		if (hour < END) {
			return 0;
		}
		return -DAWN_SEPARATION * (hour - END) / (SUNSET - END);
	}

	/** La part du soleil couverte par la lune, de 0 à 1 (deux carrés qui glissent l'un sur l'autre). */
	public static float coverage(long time) {
		float separation = separation(time);
		if (Float.isNaN(separation)) {
			return 0;
		}
		float d = Math.abs(separation);
		float overlap = Math.min(d + MOON_HALF, SUN_HALF) - Math.max(d - MOON_HALF, -SUN_HALF);
		return Math.clamp(overlap / (2 * SUN_HALF), 0, 1);
	}

	/** L'éclipse a-t-elle lieu à cette heure de l'horloge du monde (en ticks depuis le premier jour) ? */
	public static boolean at(long time) {
		long hour = Math.floorMod(time, 24000L);
		return day(time) && hour >= START && hour < END;
	}

	/** Ce que les joueurs apprennent de l'éclipse : le matin du jour, puis son début et sa fin. */
	public enum Announcement { NONE, TODAY, BEGINS, ENDS }

	/**
	 * L'annonce due à cette heure de l'horloge du monde.
	 *
	 * @param last l'heure de la dernière annonce : elle ne se répète pas
	 */
	public static Announcement announcement(long clock, long last) {
		if (clock == last) {
			return Announcement.NONE;
		}
		long hour = Math.floorMod(clock, 24000L);
		if (!day(clock)) {
			return Announcement.NONE;
		}
		if (hour == 0) {
			return Announcement.TODAY;
		}
		if (hour == START) {
			return Announcement.BEGINS;
		}
		return hour == END ? Announcement.ENDS : Announcement.NONE;
	}

	public static void register() {
		ServerEvents.STOPPED.register(server -> announced = Long.MIN_VALUE);
		ServerEvents.END_TICK.register(server -> {
			ServerLevel overworld = server.overworld();
			long clock = overworld.getOverworldClockTime();
			Announcement announcement = announcement(clock, announced);
			if (announcement == Announcement.NONE) {
				return;
			}
			announced = clock;
			if (announcement == Announcement.TODAY) {
				// Le matin du jour de l'éclipse, on le sent venir.
				for (ServerPlayer p : overworld.players()) {
					p.sendSystemMessage(Component.translatable("eclipse.fmab.today").withStyle(ChatFormatting.GOLD));
				}
				return;
			}
			boolean starting = announcement == Announcement.BEGINS;
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
