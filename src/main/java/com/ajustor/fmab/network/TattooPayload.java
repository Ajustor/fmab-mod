package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Le joueur choisit l'emplacement du tatouage, ou demande d'en effacer un ({@code erase}). */
public record TattooPayload(BlockPos circle, String slot, boolean erase) implements CustomPacketPayload {
	public static final Type<TattooPayload> TYPE = new Type<>(Fmab.id("tattoo"));

	public static final StreamCodec<ByteBuf, TattooPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, TattooPayload::circle,
			ByteBufCodecs.stringUtf8(32), TattooPayload::slot,
			ByteBufCodecs.BOOL, TattooPayload::erase,
			TattooPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
