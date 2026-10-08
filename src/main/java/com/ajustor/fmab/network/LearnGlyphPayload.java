package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Le joueur a étudié la page d'un glyphe dans le Traité. */
public record LearnGlyphPayload(String glyph) implements CustomPacketPayload {
	public static final Type<LearnGlyphPayload> TYPE = new Type<>(Fmab.id("learn_glyph"));

	public static final StreamCodec<ByteBuf, LearnGlyphPayload> CODEC =
			ByteBufCodecs.stringUtf8(128).map(LearnGlyphPayload::new, LearnGlyphPayload::glyph);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
