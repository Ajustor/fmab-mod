package com.ajustor.fmab.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

/**
 * Un alliage : un métal fait de plusieurs éléments, dans les proportions données. Le laiton des
 * autres mods (Create…) est du cuivre et du zinc ; une plaque de laiton se défait donc en cuivre et
 * en zinc, et se recompose avec eux. Une clé commençant par {@code #} désigne un tag d'items ; la
 * masse de chaque objet se partage entre les éléments selon leurs parts.
 *
 * <pre>{@code
 * { "parts": { "copper": 1, "zinc": 1 }, "values": { "#c:ingots/brass": 9 } }
 * }</pre>
 */
public record AlloyGroup(Map<String, Integer> parts, Map<String, Integer> values) {
	public static final Codec<AlloyGroup> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.unboundedMap(Codec.STRING, Codec.intRange(1, Integer.MAX_VALUE)).fieldOf("parts")
					.forGetter(AlloyGroup::parts),
			Codec.unboundedMap(Codec.STRING, Codec.intRange(1, Integer.MAX_VALUE)).fieldOf("values")
					.forGetter(AlloyGroup::values)
	).apply(i, AlloyGroup::new));
}
