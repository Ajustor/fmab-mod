package com.ajustor.fmab.data;

import com.ajustor.fmab.alchemy.exchange.Family;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

/**
 * Un fichier de valeurs d'échange : une famille et la masse de chaque item. Une clé commençant par
 * {@code #} désigne un tag d'items.
 *
 * <pre>{@code
 * { "family": "metal", "values": { "minecraft:iron_ingot": 9, "#c:nuggets/iron": 1 } }
 * }</pre>
 */
public record ExchangeGroup(Family family, Map<String, Integer> values) {
	public static final Codec<Family> FAMILY = Codec.STRING.xmap(Family::fromSerializedName, Family::serializedName);

	public static final Codec<ExchangeGroup> CODEC = RecordCodecBuilder.create(i -> i.group(
			FAMILY.fieldOf("family").forGetter(ExchangeGroup::family),
			Codec.unboundedMap(Codec.STRING, Codec.intRange(1, Integer.MAX_VALUE)).fieldOf("values")
					.forGetter(ExchangeGroup::values)
	).apply(i, ExchangeGroup::new));
}
