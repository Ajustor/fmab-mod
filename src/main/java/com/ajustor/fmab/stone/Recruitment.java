package com.ajustor.fmab.stone;

import org.jspecify.annotations.Nullable;

/**
 * L'offre de Père aux âmes noires et aux Pierres vivantes. Sous {@link Karma#RECRUITABLE}, ou pour qui
 * est devenu une Pierre vivante, la première visite des homonculus n'est pas un combat : Lust vient
 * proposer d'entrer à son service. Accepter, c'est un noyau de Pierre
 * en acompte et la paix avec les homonculus ; refuser (la frapper, ou la laisser sans réponse), c'est
 * la traque, comme pour qui porte une Pierre vivante.
 */
public final class Recruitment {
	public static final String ACCEPTED = "accepted";
	public static final String REFUSED = "refused";
	/** Le temps que Lust laisse pour répondre, en ticks. */
	public static final int PATIENCE = 20 * 60;
	/** Au-delà de cette distance, s'éloigner d'elle, c'est répondre non. */
	public static final double WALK_AWAY = 48;
	/** Ce que coûte au karma l'entrée au service de Père. */
	public static final int ACCEPT_KARMA = -10;
	/** Ce que lui rapporte un refus. */
	public static final int REFUSE_KARMA = 10;

	/** Ce que les homonculus viennent faire chez un joueur. */
	public enum Visit {
		/** Rien : ils le laissent tranquille. */
		NONE,
		/** Lust vient faire l'offre de Père. */
		OFFER,
		/** Un homonculus vient le chercher, hostile. */
		HUNT
	}

	private Recruitment() {
	}

	/**
	 * La visite due à un joueur : une recrue n'est plus inquiétée ; une âme noire ou une Pierre vivante
	 * qui n'a pas encore répondu reçoit l'offre ; qui a refusé, âme noire ou Pierre vivante, est traqué.
	 */
	public static Visit visit(int karma, int souls, @Nullable String answer) {
		if (ACCEPTED.equals(answer)) {
			return Visit.NONE;
		}
		boolean dark = karma <= Karma.RECRUITABLE;
		if ((dark || souls > 0) && answer == null) {
			return Visit.OFFER;
		}
		return dark || souls > 0 ? Visit.HUNT : Visit.NONE;
	}

	/** Lust attend-elle encore une réponse, après tant de ticks, le joueur à cette distance ? */
	/**
	 * La paix promise à qui a accepté l'offre de Père : les homonculus ne s'en prennent pas à lui, sauf
	 * s'il les frappe le premier.
	 *
	 * @param answer   sa réponse à l'offre ({@link #ACCEPTED}, {@link #REFUSED} ou rien)
	 * @param struckUs il vient de frapper cet homonculus
	 */
	public static boolean spared(@Nullable String answer, boolean struckUs) {
		return ACCEPTED.equals(answer) && !struckUs;
	}

	public static boolean stillWaiting(int waited, double distance) {
		return waited < PATIENCE && distance <= WALK_AWAY;
	}
}
