package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Le rayon que l'alchimiste donne à son cercle de la Pierre ; zéro s'il renonce. */
public record StoneRadiusChosenPayload(int radius) implements CustomPacketPayload {
	public static final Type<StoneRadiusChosenPayload> TYPE = new Type<>(Fmab.id("stone_radius_chosen"));
	public static final StreamCodec<ByteBuf, StoneRadiusChosenPayload> CODEC =
			ByteBufCodecs.VAR_INT.map(StoneRadiusChosenPayload::new, StoneRadiusChosenPayload::radius);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
