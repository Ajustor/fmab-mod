package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le cercle de la Pierre philosophale s'éveille : jusqu'où doit-il porter ?
 *
 * @param suggested le rayon que donne le cercle tracé (sa portée)
 * @param max       le plus grand rayon qu'on puisse demander
 */
public record StoneRadiusPayload(int suggested, int max) implements CustomPacketPayload {
	public static final Type<StoneRadiusPayload> TYPE = new Type<>(Fmab.id("stone_radius"));
	public static final StreamCodec<ByteBuf, StoneRadiusPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, StoneRadiusPayload::suggested,
			ByteBufCodecs.VAR_INT, StoneRadiusPayload::max,
			StoneRadiusPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
