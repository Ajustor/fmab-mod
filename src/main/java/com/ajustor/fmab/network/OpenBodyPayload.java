package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** L'onglet « Corps » de l'inventaire : le serveur ouvre la page du corps. */
public record OpenBodyPayload() implements CustomPacketPayload {
	public static final OpenBodyPayload INSTANCE = new OpenBodyPayload();
	public static final Type<OpenBodyPayload> TYPE = new Type<>(Fmab.id("open_body"));
	public static final StreamCodec<ByteBuf, OpenBodyPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
