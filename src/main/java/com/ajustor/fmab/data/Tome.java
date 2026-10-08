package com.ajustor.fmab.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * Contenu d'un tome d'alchimie : son titre (clé de traduction) et les glyphes qu'il enseigne à qui
 * le lit.
 */
public record Tome(String titleKey, List<String> glyphs) {
	public static final Codec<Tome> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("title").forGetter(Tome::titleKey),
			Codec.STRING.listOf().fieldOf("glyphs").forGetter(Tome::glyphs)
	).apply(i, Tome::new));

	public static final StreamCodec<ByteBuf, Tome> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, Tome::titleKey,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Tome::glyphs,
			Tome::new);

	public Tome {
		glyphs = List.copyOf(glyphs);
	}
}
