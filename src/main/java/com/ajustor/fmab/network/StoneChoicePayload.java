package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Une transmutation humaine, Pierre en main : ouvrir sa Porte, ou donner une âme à l'être créé.
 *
 * @param souls les âmes de la Pierre tenue
 */
public record StoneChoicePayload(int souls) implements CustomPacketPayload {
	public static final Type<StoneChoicePayload> TYPE = new Type<>(Fmab.id("stone_choice"));
	public static final StreamCodec<ByteBuf, StoneChoicePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, StoneChoicePayload::souls, StoneChoicePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
