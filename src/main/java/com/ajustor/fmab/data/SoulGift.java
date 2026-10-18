package com.ajustor.fmab.data;

import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.Restoration;

import java.util.List;
import java.util.Optional;

/**
 * Offrir une âme pour rendre à un allié ce que la Porte lui a pris, étape par étape : le donneur
 * choisit ce qu'il rend et ce qu'il paie ; l'allié accepte ; le donneur qui donne son âme ou sa Porte
 * confirme une seconde fois. Rien n'est payé avant {@link Stage#DONE}, et on n'y arrive qu'une fois.
 *
 * <p>Comme dans l'histoire des Elric : à un allié de chair, on rend un membre, la vue ou ses organes ;
 * à une âme fixée dans une armure (Alphonse), seulement son corps, que paient la Porte du donneur
 * (Edward, qui y perd l'alchimie), son âme (Hohenheim) ou les âmes d'une Pierre. À qui a donné sa Porte,
 * on peut la rendre ({@code gate}) : seule une Pierre la paie, au plus haut prix.
 *
 * @param gate le don rend sa Porte à l'allié (alors {@code part} est vide)
 */
public record SoulGift(Stage stage, Optional<BodyPart> part, boolean gate, Optional<Payment> payment) {
	/** L'identifiant de la Porte parmi ce qu'on peut rendre (à côté de ceux des {@link BodyPart}). */
	public static final String GATE_ID = "gate";

	/** Ce qu'on peut rendre à un allié de chair : un membre, la vue, les organes. */
	public static final List<BodyPart> RESTORABLE = SoulOffer.OFFERABLE;

	public enum Stage {
		/** Le donneur choisit ce qu'il rend et ce qu'il paie. */
		CHOOSING,
		/** L'allié accepte ou refuse. */
		ASKING,
		/** L'allié a accepté ; le donneur qui donne son âme ou sa Porte confirme. */
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
		STONE,
		/**
		 * Sa Porte : il ne pratiquera plus jamais l'alchimie. Le prix d'Edward pour le corps d'Alphonse :
		 * il ne paie que le corps entier.
		 */
		GATE
	}

	public static SoulGift start() {
		return new SoulGift(Stage.CHOOSING, Optional.empty(), false, Optional.empty());
	}

	/**
	 * Ce qu'on peut rendre à l'allié : ce qu'il a perdu de sa chair ; à une âme fixée dans une armure,
	 * seulement son corps (elle n'a plus de membres de chair à qui rendre quoi que ce soit).
	 */
	public static List<BodyPart> restorable(GateState ally) {
		if (ally.soulBound()) {
			return ally.inArmor() ? List.of(BodyPart.BODY) : List.of();
		}
		return RESTORABLE.stream().filter(ally::lost).toList();
	}

	/**
	 * Le donneur peut-il payer ainsi ? Sa propre âme, s'il a encore un corps ; la Pierre, si elle a
	 * assez d'âmes pour cette partie ; sa Porte, pour un corps entier, s'il l'a encore.
	 *
	 * @param gateless le donneur a déjà donné sa Porte
	 */
	public static boolean affordable(Payment payment, BodyPart part, GateState donor, int stoneSouls, boolean gateless) {
		return switch (payment) {
			case OWN_SOUL -> !donor.soulBound();
			case STONE -> stoneSouls >= Restoration.cost(part);
			case GATE -> part == BodyPart.BODY && !gateless;
		};
	}

	public boolean open() {
		return stage != Stage.DONE && stage != Stage.CANCELLED;
	}

	public SoulGift choose(BodyPart value, Payment by, GateState ally, GateState donor, int stoneSouls, boolean gateless) {
		if (stage != Stage.CHOOSING || !restorable(ally).contains(value)
				|| !affordable(by, value, donor, stoneSouls, gateless)) {
			return this;
		}
		return new SoulGift(Stage.ASKING, Optional.of(value), false, Optional.of(by));
	}

	/**
	 * Rendre sa Porte à l'allié qui l'a donnée : seule la Pierre paie, et il lui faut {@code cost} âmes.
	 *
	 * @param allyGateless l'allié a donné sa Porte
	 */
	public SoulGift chooseGate(boolean allyGateless, int stoneSouls, int cost) {
		if (stage != Stage.CHOOSING || !allyGateless || stoneSouls < cost) {
			return this;
		}
		return new SoulGift(Stage.ASKING, Optional.empty(), true, Optional.of(Payment.STONE));
	}

	/**
	 * La réponse de l'allié. Refusée, le rituel s'éteint ; acceptée, il est payé (avec la Pierre), ou
	 * attend la seconde confirmation du donneur (avec son âme ou sa Porte, qu'on ne reprend pas).
	 */
	public SoulGift answer(boolean accepted) {
		if (stage != Stage.ASKING) {
			return this;
		}
		if (!accepted) {
			return new SoulGift(Stage.CANCELLED, part, gate, payment);
		}
		return new SoulGift(payment.orElseThrow() == Payment.STONE ? Stage.DONE : Stage.CONFIRMING, part, gate, payment);
	}

	/** La seconde confirmation du donneur qui donne son âme ou sa Porte. */
	public SoulGift confirm() {
		return stage == Stage.CONFIRMING ? new SoulGift(Stage.DONE, part, gate, payment) : this;
	}

	public SoulGift cancel() {
		return open() ? new SoulGift(Stage.CANCELLED, part, gate, payment) : this;
	}
}
