package com.ajustor.fmab.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Optional;

/**
 * Contenu d'un tome d'alchimie : son titre (clé de traduction), les glyphes qu'il enseigne à qui le
 * lit et, pour des notes qu'un alchimiste a consignées pour un autre, le nom de leur auteur.
 */
public record Tome(String titleKey, List<String> glyphs, Optional<String> author) {
	public static final Codec<Tome> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("title").forGetter(Tome::titleKey),
			Codec.STRING.listOf().fieldOf("glyphs").forGetter(Tome::glyphs),
			Codec.STRING.optionalFieldOf("author").forGetter(Tome::author)
	).apply(i, Tome::new));

	public static final StreamCodec<ByteBuf, Tome> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, Tome::titleKey,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Tome::glyphs,
			ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), Tome::author,
			Tome::new);

	/** Le titre des notes qu'un alchimiste consigne de sa main. */
	public static final String NOTES = "tome.fmab.notes";

	public Tome {
		glyphs = List.copyOf(glyphs);
	}

	public Tome(String titleKey, List<String> glyphs) {
		this(titleKey, glyphs, Optional.empty());
	}

	/** Les notes de cet alchimiste : les glyphes qu'il a choisi d'y consigner. */
	public static Tome notes(String author, List<String> glyphs) {
		return new Tome(NOTES, glyphs, Optional.of(author));
	}
}
