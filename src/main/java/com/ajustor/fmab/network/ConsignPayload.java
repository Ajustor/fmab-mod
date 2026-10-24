package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.item.Notes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Consigner son savoir : les glyphes qu'un alchimiste écrit dans ses notes (voir {@link Notes}). */
public record ConsignPayload(List<String> glyphs) implements CustomPacketPayload {
	public static final Type<ConsignPayload> TYPE = new Type<>(Fmab.id("consign"));

	public static final StreamCodec<ByteBuf, ConsignPayload> CODEC =
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(64)).map(ConsignPayload::new, ConsignPayload::glyphs);

	public ConsignPayload {
		glyphs = List.copyOf(glyphs);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
