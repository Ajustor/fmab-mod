package com.ajustor.fmab.data;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ce que le joueur sait et peut en alchimie. Immuable : chaque changement remplace l'attachement,
 * ce qui déclenche la synchronisation vers le client.
 *
 * @param known         identifiants des glyphes compris
 * @param concentration réserve d'énergie, de 0 à {@link #MAX_CONCENTRATION}
 */
public record AlchemistData(Rank rank, Set<String> known, float concentration) {
	public static final float MAX_CONCENTRATION = 20;
	public static final AlchemistData NEW = new AlchemistData(Rank.APPRENTICE, Set.of(), MAX_CONCENTRATION);

	private static final Codec<Rank> RANK = Codec.STRING.xmap(Rank::fromSerializedName, Rank::serializedName);
	private static final StreamCodec<ByteBuf, Rank> RANK_STREAM =
			ByteBufCodecs.STRING_UTF8.map(Rank::fromSerializedName, Rank::serializedName);

	public static final Codec<AlchemistData> CODEC = RecordCodecBuilder.create(i -> i.group(
			RANK.optionalFieldOf("rank", Rank.APPRENTICE).forGetter(AlchemistData::rank),
			Codec.STRING.listOf().<Set<String>>xmap(Set::copyOf, List::copyOf).optionalFieldOf("known", Set.of())
					.forGetter(AlchemistData::known),
			Codec.FLOAT.optionalFieldOf("concentration", MAX_CONCENTRATION).forGetter(AlchemistData::concentration)
	).apply(i, AlchemistData::new));

	public static final StreamCodec<ByteBuf, AlchemistData> STREAM_CODEC = StreamCodec.composite(
			RANK_STREAM, AlchemistData::rank,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).<Set<String>>map(Set::copyOf, List::copyOf),
			AlchemistData::known,
			ByteBufCodecs.FLOAT, AlchemistData::concentration,
			AlchemistData::new);

	public AlchemistData {
		known = Set.copyOf(known);
	}

	public AlchemistData learn(String glyph) {
		Set<String> more = new HashSet<>(known);
		more.add(glyph);
		return new AlchemistData(rank, more, concentration);
	}

	public AlchemistData withConcentration(float value) {
		return new AlchemistData(rank, known, Math.clamp(value, 0, MAX_CONCENTRATION));
	}

	public AlchemistData withRank(Rank value) {
		return new AlchemistData(value, known, concentration);
	}
}
