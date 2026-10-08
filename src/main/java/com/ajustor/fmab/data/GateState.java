package com.ajustor.fmab.data;

import com.ajustor.fmab.gate.BodyPart;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Ce que la Porte a coûté au joueur, et la visite en cours s'il est dans l'Espace blanc.
 *
 * @param openings nombre de fois où il a ouvert sa Porte
 * @param lost     ce que la Vérité a pris (et qu'elle porte désormais)
 * @param adrift   l'âme a perdu son armure : elle erre devant la Porte jusqu'à ce qu'un cercle d'âme
 *                 l'appelle dans une autre
 * @param visit    la visite en cours, jusqu'au retour
 */
public record GateState(int openings, Set<BodyPart> lost, boolean adrift, Optional<Visit> visit) {
	public static final GateState NONE = new GateState(0, Set.of(), false, Optional.empty());

	private static final Codec<BodyPart> PART = Codec.STRING.xmap(BodyPart::fromSerializedName,
			BodyPart::serializedName);

	/**
	 * Une visite de l'Espace blanc.
	 *
	 * @param dimension où revenir (identifiant de la dimension)
	 * @param origin    le cercle d'où l'on est parti
	 * @param ambition  ce qui a été tenté
	 * @param severe    échec grave : le corps entier sera pris
	 * @param ticks     temps écoulé depuis l'arrivée ; négatif tant qu'on n'y est pas
	 */
	public record Visit(String dimension, BlockPos origin, int ambition, boolean severe, int ticks) {
		public static final Codec<Visit> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("dimension").forGetter(Visit::dimension),
				BlockPos.CODEC.fieldOf("origin").forGetter(Visit::origin),
				Codec.INT.fieldOf("ambition").forGetter(Visit::ambition),
				Codec.BOOL.fieldOf("severe").forGetter(Visit::severe),
				Codec.INT.fieldOf("ticks").forGetter(Visit::ticks)
		).apply(i, Visit::new));

		public Visit tick() {
			return new Visit(dimension, origin, ambition, severe, ticks + 1);
		}
	}

	public static final Codec<GateState> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("openings", 0).forGetter(GateState::openings),
			PART.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("lost", Set.of())
					.forGetter(GateState::lost),
			Codec.BOOL.optionalFieldOf("adrift", false).forGetter(GateState::adrift),
			Visit.CODEC.optionalFieldOf("visit").forGetter(GateState::visit)
	).apply(i, GateState::new));

	/** Le client n'a besoin que de ce qui a été pris (vue, mains) : la visite reste au serveur. */
	public static final StreamCodec<ByteBuf, GateState> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, GateState::openings,
			ByteBufCodecs.STRING_UTF8.map(BodyPart::fromSerializedName, BodyPart::serializedName)
					.apply(ByteBufCodecs.list()).map(Set::copyOf, List::copyOf), GateState::lost,
			ByteBufCodecs.BOOL, GateState::adrift,
			GateState::clientView);

	public GateState {
		lost = lost.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(lost));
	}

	private static GateState clientView(int openings, Set<BodyPart> lost, boolean adrift) {
		return new GateState(openings, lost, adrift, Optional.empty());
	}

	public boolean lost(BodyPart part) {
		return lost.contains(part);
	}

	/** Le corps est perdu : l'âme vit dans une armure, ou erre devant la Porte. */
	public boolean soulBound() {
		return lost.contains(BodyPart.BODY);
	}

	/** L'âme habite une armure. */
	public boolean inArmor() {
		return soulBound() && !adrift;
	}

	public GateState withVisit(Visit value) {
		return new GateState(openings, lost, adrift, Optional.ofNullable(value));
	}

	public GateState withAdrift(boolean value) {
		return new GateState(openings, lost, value, visit);
	}

	/** La Vérité a pris son péage : une ouverture de plus, et ce qu'elle a pris. */
	public GateState pay(BodyPart part) {
		Set<BodyPart> more = EnumSet.noneOf(BodyPart.class);
		more.addAll(lost);
		more.add(part);
		return new GateState(openings + 1, more, adrift, visit);
	}

	/** Ce que la Vérité avait pris revient (la Pierre philosophale a payé). */
	public GateState restore(BodyPart part) {
		Set<BodyPart> rest = EnumSet.noneOf(BodyPart.class);
		rest.addAll(lost);
		rest.remove(part);
		return new GateState(openings, rest, part == BodyPart.BODY ? false : adrift, visit);
	}

	/** Une ouverture de plus, sans rien à prendre que le sceau : l'âme perd son armure. */
	public GateState payWithSeal() {
		return new GateState(openings + 1, lost, true, visit);
	}
}
