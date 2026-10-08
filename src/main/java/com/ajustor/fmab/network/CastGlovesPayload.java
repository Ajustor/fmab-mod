package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Touche des gants : lancer leur cercle, ou joindre les mains pour lancer les deux. */
public record CastGlovesPayload(boolean combine) implements CustomPacketPayload {
	public static final Type<CastGlovesPayload> TYPE = new Type<>(Fmab.id("cast_gloves"));

	public static final StreamCodec<ByteBuf, CastGlovesPayload> CODEC =
			ByteBufCodecs.BOOL.map(CastGlovesPayload::new, CastGlovesPayload::combine);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
