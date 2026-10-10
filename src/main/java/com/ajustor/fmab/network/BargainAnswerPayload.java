package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** La partie que l'alchimiste rachète à la Vérité ; vide pour s'en aller. */
public record BargainAnswerPayload(String part) implements CustomPacketPayload {
	public static final Type<BargainAnswerPayload> TYPE = new Type<>(Fmab.id("bargain_answer"));
	public static final StreamCodec<ByteBuf, BargainAnswerPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, BargainAnswerPayload::part, BargainAnswerPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
