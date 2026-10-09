package com.ajustor.fmab.data;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ce que le joueur sait et peut en alchimie. Immuable : chaque changement remplace l'attachement,
 * ce qui déclenche la synchronisation vers le client.
 *
 * @param known         identifiants des glyphes compris
 * @param concentration réserve d'énergie, de 0 au maximum du rang ({@link #maxConcentration()})
 * @param mastery       maîtrise par école ({@code earth}, {@code metal}...)
 * @param granted       nœuds de savoir accordés par un livre ou un maître, sans attendre la maîtrise
 * @param familiarity   usages de chaque glyphe tracé sans être compris ; à {@link #USES_TO_LEARN},
 *                      il est compris
 */
public record AlchemistData(Rank rank, Set<String> known, float concentration, Map<String, Integer> mastery,
		Set<String> granted, Map<String, Integer> familiarity) {
	/** La réserve d'un Apprenti, au départ. */
	public static final float MAX_CONCENTRATION = 20;
	/** À force de tracer un glyphe qu'on ne comprend pas, on finit par le comprendre. */
	public static final int USES_TO_LEARN = 5;
	public static final AlchemistData NEW =
			new AlchemistData(Rank.APPRENTICE, Set.of(), MAX_CONCENTRATION, Map.of(), Set.of(), Map.of());

	private static final Codec<Rank> RANK = Codec.STRING.xmap(Rank::fromSerializedName, Rank::serializedName);
	private static final StreamCodec<ByteBuf, Rank> RANK_STREAM =
			ByteBufCodecs.STRING_UTF8.map(Rank::fromSerializedName, Rank::serializedName);
	private static final Codec<Set<String>> STRING_SET =
			Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf);
	private static final StreamCodec<ByteBuf, Set<String>> STRING_SET_STREAM =
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(Set::copyOf, List::copyOf);

	public static final Codec<AlchemistData> CODEC = RecordCodecBuilder.create(i -> i.group(
			RANK.optionalFieldOf("rank", Rank.APPRENTICE).forGetter(AlchemistData::rank),
			STRING_SET.optionalFieldOf("known", Set.of()).forGetter(AlchemistData::known),
			Codec.FLOAT.optionalFieldOf("concentration", MAX_CONCENTRATION).forGetter(AlchemistData::concentration),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("mastery", Map.of())
					.forGetter(AlchemistData::mastery),
			STRING_SET.optionalFieldOf("granted", Set.of()).forGetter(AlchemistData::granted),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("familiarity", Map.of())
					.forGetter(AlchemistData::familiarity)
	).apply(i, AlchemistData::new));

	public static final StreamCodec<ByteBuf, AlchemistData> STREAM_CODEC = StreamCodec.composite(
			RANK_STREAM, AlchemistData::rank,
			STRING_SET_STREAM, AlchemistData::known,
			ByteBufCodecs.FLOAT, AlchemistData::concentration,
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), AlchemistData::mastery,
			STRING_SET_STREAM, AlchemistData::granted,
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
			AlchemistData::familiarity,
			AlchemistData::new);

	public AlchemistData {
		known = Set.copyOf(known);
		mastery = Map.copyOf(mastery);
		granted = Set.copyOf(granted);
		familiarity = Map.copyOf(familiarity);
	}

	public AlchemistData learn(String glyph) {
		Set<String> more = new HashSet<>(known);
		more.add(glyph);
		Map<String, Integer> rest = new HashMap<>(familiarity);
		rest.remove(glyph);
		return new AlchemistData(rank, more, concentration, mastery, granted, rest);
	}

	public AlchemistData forgetAll() {
		return new AlchemistData(rank, Set.of(), concentration, mastery, granted, Map.of());
	}

	/** La réserve de concentration, qui grandit avec le rang. */
	public float maxConcentration() {
		return rank.maxConcentration();
	}

	public AlchemistData withConcentration(float value) {
		return new AlchemistData(rank, known, Math.clamp(value, 0, maxConcentration()), mastery, granted,
				familiarity);
	}

	public AlchemistData withRank(Rank value) {
		return new AlchemistData(value, known, concentration, mastery, granted, familiarity);
	}

	public AlchemistData addMastery(String school, int amount) {
		Map<String, Integer> more = new HashMap<>(mastery);
		more.merge(school, amount, Integer::sum);
		return new AlchemistData(rank, known, concentration, more, granted, familiarity);
	}

	public AlchemistData grant(String node) {
		Set<String> more = new HashSet<>(granted);
		more.add(node);
		return new AlchemistData(rank, known, concentration, mastery, more, familiarity);
	}

	/** Usages d'un glyphe pas encore compris. */
	public int uses(String glyph) {
		return familiarity.getOrDefault(glyph, 0);
	}

	/** Un usage de plus d'un glyphe pas encore compris ; au dernier, il est compris. */
	public AlchemistData practiceGlyph(String glyph) {
		if (known.contains(glyph)) {
			return this;
		}
		int uses = uses(glyph) + 1;
		if (uses >= USES_TO_LEARN) {
			return learn(glyph);
		}
		Map<String, Integer> more = new HashMap<>(familiarity);
		more.put(glyph, uses);
		return new AlchemistData(rank, known, concentration, mastery, granted, more);
	}
}
