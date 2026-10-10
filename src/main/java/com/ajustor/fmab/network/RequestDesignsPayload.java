package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Le joueur ouvre sa page de conception : le serveur lui envoie ce qu'il sait recomposer. */
public record RequestDesignsPayload() implements CustomPacketPayload {
	public static final RequestDesignsPayload INSTANCE = new RequestDesignsPayload();
	public static final Type<RequestDesignsPayload> TYPE = new Type<>(Fmab.id("request_designs"));
	public static final StreamCodec<ByteBuf, RequestDesignsPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
