package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ce que l'âme errante répond à la Vérité.
 *
 * @param restart vrai pour repartir de zéro, faux pour être rappelée dans un de ses sceaux
 */
public record TruthChoicePayload(int entityId, boolean restart) implements CustomPacketPayload {
	public static final Type<TruthChoicePayload> TYPE = new Type<>(Fmab.id("truth_choice"));

	public static final StreamCodec<ByteBuf, TruthChoicePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TruthChoicePayload::entityId,
			ByteBufCodecs.BOOL, TruthChoicePayload::restart,
			TruthChoicePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
