package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** La touche de l'âme en armure : se figer, s'éveiller, ou relâcher celui qui est dedans. */
public record ShellKeyPayload() implements CustomPacketPayload {
	public static final ShellKeyPayload INSTANCE = new ShellKeyPayload();
	public static final Type<ShellKeyPayload> TYPE = new Type<>(Fmab.id("shell_key"));
	public static final StreamCodec<ByteBuf, ShellKeyPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
