package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * La réponse d'un joueur au sacrifice d'un membre (voir {@link SoulOfferPayload}).
 *
 * @param step   l'étape à laquelle il répond
 * @param part   ce que le donneur choisit (au premier choix ; vide sinon)
 * @param accept oui, ou non (un écran fermé vaut non)
 */
public record SoulOfferAnswerPayload(int step, String part, boolean accept) implements CustomPacketPayload {
	public static final Type<SoulOfferAnswerPayload> TYPE = new Type<>(Fmab.id("soul_offer_answer"));
	public static final StreamCodec<ByteBuf, SoulOfferAnswerPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SoulOfferAnswerPayload::step,
			ByteBufCodecs.STRING_UTF8, SoulOfferAnswerPayload::part,
			ByteBufCodecs.BOOL, SoulOfferAnswerPayload::accept,
			SoulOfferAnswerPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
