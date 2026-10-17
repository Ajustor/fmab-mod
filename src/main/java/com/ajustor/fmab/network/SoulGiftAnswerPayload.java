package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * La réponse d'un joueur à l'offre d'une âme (voir {@link SoulGiftPayload}).
 *
 * @param step    l'étape à laquelle il répond
 * @param part    ce que le donneur rend (au premier choix ; vide sinon)
 * @param payment ce qu'il paie, {@code own_soul} ou {@code stone} (au premier choix ; vide sinon)
 * @param accept  oui, ou non (un écran fermé vaut non)
 */
public record SoulGiftAnswerPayload(int step, String part, String payment, boolean accept) implements CustomPacketPayload {
	public static final Type<SoulGiftAnswerPayload> TYPE = new Type<>(Fmab.id("soul_gift_answer"));
	public static final StreamCodec<ByteBuf, SoulGiftAnswerPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SoulGiftAnswerPayload::step,
			ByteBufCodecs.STRING_UTF8, SoulGiftAnswerPayload::part,
			ByteBufCodecs.STRING_UTF8, SoulGiftAnswerPayload::payment,
			ByteBufCodecs.BOOL, SoulGiftAnswerPayload::accept,
			SoulGiftAnswerPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
