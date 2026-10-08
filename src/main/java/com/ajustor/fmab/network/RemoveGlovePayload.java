package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Retirer le gant d'une main pour le remettre dans l'inventaire. */
public record RemoveGlovePayload(boolean left) implements CustomPacketPayload {
	public static final Type<RemoveGlovePayload> TYPE = new Type<>(Fmab.id("remove_glove"));

	public static final StreamCodec<ByteBuf, RemoveGlovePayload> CODEC =
			ByteBufCodecs.BOOL.map(RemoveGlovePayload::new, RemoveGlovePayload::left);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
