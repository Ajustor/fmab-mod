package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Ce que l'alchimiste a choisi : {@code gate}, {@code being:<forme>} ({@code being:dog}…), ou rien. */
public record StoneChosenPayload(String choice) implements CustomPacketPayload {
	public static final Type<StoneChosenPayload> TYPE = new Type<>(Fmab.id("stone_chosen"));
	public static final StreamCodec<ByteBuf, StoneChosenPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, StoneChosenPayload::choice, StoneChosenPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
