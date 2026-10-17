package com.ajustor.fmab.data;

import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.Restoration;

import java.util.List;
import java.util.Optional;

/**
 * Offrir une âme pour rendre à un allié ce que la Porte lui a pris, étape par étape : le donneur
 * choisit ce qu'il rend et ce qu'il paie (sa propre âme, ou des âmes de sa Pierre) ; l'allié accepte ;
 * le donneur qui donne sa propre âme confirme une seconde fois. Rien n'est payé avant
 * {@link Stage#DONE}, et on n'y arrive qu'une fois.
 */
public record SoulGift(Stage stage, Optional<BodyPart> part, Optional<Payment> payment) {
	/** Ce qu'on peut rendre : un membre, la vue, les organes (pas le corps entier). */
	public static final List<BodyPart> RESTORABLE = SoulOffer.OFFERABLE;

	public enum Stage {
		/** Le donneur choisit ce qu'il rend et ce qu'il paie. */
		CHOOSING,
		/** L'allié accepte ou refuse. */
		ASKING,
		/** L'allié a accepté ; le donneur qui donne son âme confirme. */
		CONFIRMING,
		/** Payé et rendu. */
		DONE,
		/** Abandonné, sans rien payer. */
		CANCELLED
	}

	/** Ce que paie le donneur. */
	public enum Payment {
		/** Sa propre âme : la Vérité prend tout son corps, il se réveille dans une armure. */
		OWN_SOUL,
		/** Des âmes de la Pierre philosophale qu'il tient, au prix de {@link Restoration#cost}. */
		STONE
	}

	public static SoulGift start() {
		return new SoulGift(Stage.CHOOSING, Optional.empty(), Optional.empty());
	}

	/** Ce que l'allié a perdu et qu'on peut lui rendre. */
	public static List<BodyPart> restorable(GateState ally) {
		return RESTORABLE.stream().filter(ally::lost).toList();
	}

	/**
	 * Le donneur peut-il payer ainsi ? Sa propre âme, s'il a encore un corps ; la Pierre, si elle a
	 * assez d'âmes pour cette partie.
	 */
	public static boolean affordable(Payment payment, BodyPart part, GateState donor, int stoneSouls) {
		return switch (payment) {
			case OWN_SOUL -> !donor.soulBound();
			case STONE -> stoneSouls >= Restoration.cost(part);
		};
	}

	public boolean open() {
		return stage != Stage.DONE && stage != Stage.CANCELLED;
	}

	public SoulGift choose(BodyPart value, Payment by, GateState ally, GateState donor, int stoneSouls) {
		if (stage != Stage.CHOOSING || !restorable(ally).contains(value) || !affordable(by, value, donor, stoneSouls)) {
			return this;
		}
		return new SoulGift(Stage.ASKING, Optional.of(value), Optional.of(by));
	}

	/**
	 * La réponse de l'allié. Refusée, le rituel s'éteint ; acceptée, il est payé (avec la Pierre), ou
	 * attend la seconde confirmation du donneur (avec son âme).
	 */
	public SoulGift answer(boolean accepted) {
		if (stage != Stage.ASKING) {
			return this;
		}
		if (!accepted) {
			return new SoulGift(Stage.CANCELLED, part, payment);
		}
		return new SoulGift(payment.orElseThrow() == Payment.OWN_SOUL ? Stage.CONFIRMING : Stage.DONE, part, payment);
	}

	/** La seconde confirmation du donneur qui donne son âme. */
	public SoulGift confirm() {
		return stage == Stage.CONFIRMING ? new SoulGift(Stage.DONE, part, payment) : this;
	}

	public SoulGift cancel() {
		return open() ? new SoulGift(Stage.CANCELLED, part, payment) : this;
	}
}
