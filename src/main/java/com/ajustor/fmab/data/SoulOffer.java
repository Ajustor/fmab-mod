package com.ajustor.fmab.data;

import com.ajustor.fmab.gate.BodyPart;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Le sacrifice d'un membre pour lier l'âme d'un autre à une armure, étape par étape : le donneur
 * choisit ce qu'il offre ; les âmes errantes sont sollicitées l'une après l'autre ; celle qui accepte,
 * le donneur la voit et confirme une seconde fois. Rien n'est payé avant {@link Stage#DONE}, et on n'y
 * arrive qu'une fois.
 *
 * @param part      ce que le donneur offre
 * @param recipient l'âme à qui on le propose (ou qui a accepté)
 * @param refused   les âmes qui ont refusé : on ne les redemande pas
 */
public record SoulOffer(Stage stage, Optional<BodyPart> part, Optional<UUID> recipient, Set<UUID> refused) {
	/** Ce qu'on peut offrir : un morceau de soi qu'on a encore, de chair. */
	public static final List<BodyPart> OFFERABLE = List.of(BodyPart.RIGHT_ARM, BodyPart.LEFT_ARM, BodyPart.RIGHT_LEG,
			BodyPart.LEFT_LEG, BodyPart.SIGHT, BodyPart.ORGANS);

	public enum Stage {
		/** Le donneur choisit ce qu'il offre. */
		CHOOSING,
		/** Une âme errante décide. */
		ASKING,
		/** Une âme a accepté : le donneur la voit, et confirme le sacrifice. */
		CONFIRMING,
		/** Payé et lié. */
		DONE,
		/** Abandonné, sans rien payer. */
		CANCELLED
	}

	public SoulOffer {
		refused = Set.copyOf(refused);
	}

	public static SoulOffer start() {
		return new SoulOffer(Stage.CHOOSING, Optional.empty(), Optional.empty(), Set.of());
	}

	/** Ce que ce donneur peut offrir : ce qui lui reste de chair (rien s'il n'est plus qu'une âme). */
	public static List<BodyPart> offerable(GateState gate) {
		if (gate.soulBound()) {
			return List.of();
		}
		return OFFERABLE.stream().filter(part -> !gate.lost(part)).toList();
	}

	public boolean open() {
		return stage != Stage.DONE && stage != Stage.CANCELLED;
	}

	public SoulOffer choose(BodyPart value, GateState donor) {
		if (stage != Stage.CHOOSING || !offerable(donor).contains(value)) {
			return this;
		}
		return new SoulOffer(Stage.ASKING, Optional.of(value), Optional.empty(), refused);
	}

	/** La prochaine âme à solliciter, dans l'ordre d'attente : la première qui n'a pas déjà refusé. */
	public Optional<UUID> next(List<UUID> waiting) {
		return waiting.stream().filter(soul -> !refused.contains(soul)).findFirst();
	}

	public SoulOffer ask(UUID soul) {
		if (stage != Stage.ASKING || recipient.isPresent()) {
			return this;
		}
		return new SoulOffer(stage, part, Optional.of(soul), refused);
	}

	/** La réponse de l'âme sollicitée (une autre âme ne peut pas répondre à sa place). */
	public SoulOffer answer(UUID soul, boolean accepted) {
		if (stage != Stage.ASKING || !recipient.equals(Optional.of(soul))) {
			return this;
		}
		if (accepted) {
			return new SoulOffer(Stage.CONFIRMING, part, recipient, refused);
		}
		Set<UUID> more = new HashSet<>(refused);
		more.add(soul);
		return new SoulOffer(Stage.ASKING, part, Optional.empty(), more);
	}

	/** La seconde confirmation du donneur : le sacrifice est fait. Une seule fois. */
	public SoulOffer confirm() {
		return stage == Stage.CONFIRMING ? new SoulOffer(Stage.DONE, part, recipient, refused) : this;
	}

	public SoulOffer cancel() {
		return open() ? new SoulOffer(Stage.CANCELLED, part, recipient, refused) : this;
	}
}
