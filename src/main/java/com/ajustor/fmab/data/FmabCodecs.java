package com.ajustor.fmab.data;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphJson;
import com.ajustor.fmab.alchemy.rules.Combination;
import com.ajustor.fmab.alchemy.rules.CombinationTable;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import java.util.function.Function;

/**
 * Codecs Minecraft des types du cœur alchimique. Le cœur parle JSON (c'est le format partagé avec
 * l'éditeur web) ; on passe donc par JsonOps plutôt que de décrire chaque champ deux fois.
 */
public final class FmabCodecs {
	private FmabCodecs() {
	}

	public static final Codec<Primitive> PRIMITIVE = viaJson(
			json -> GlyphJson.primitive(json.getAsJsonObject()),
			GlyphJson::toJson);

	public static final Codec<Drawing> DRAWING = PRIMITIVE.listOf(0, Drawing.MAX_PRIMITIVES)
			.xmap(Drawing::new, Drawing::primitives);

	public static final Codec<Glyph> GLYPH = viaJson(
			json -> GlyphJson.parse(json.getAsJsonObject()),
			GlyphJson::toJson);

	/** L'identifiant d'une combinaison est sa clé de registre ; il est recollé à la lecture. */
	public static final Codec<Combination> COMBINATION = viaJson(
			json -> CombinationTable.parse("", json.getAsJsonObject()),
			CombinationTable::toJson);

	private static <T> Codec<T> viaJson(Function<JsonElement, T> read, Function<T, JsonElement> write) {
		return Codec.PASSTHROUGH.comapFlatMap(
				dynamic -> {
					try {
						return DataResult.success(read.apply(dynamic.convert(JsonOps.INSTANCE).getValue()));
					} catch (RuntimeException e) {
						return DataResult.error(e::getMessage);
					}
				},
				value -> new Dynamic<>(JsonOps.INSTANCE, write.apply(value)));
	}
}
