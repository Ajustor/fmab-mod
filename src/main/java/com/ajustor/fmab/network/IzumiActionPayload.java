package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ce que le joueur demande à Izumi : {@code spar} pour un combat d'entraînement, ou l'identifiant
 * d'une épreuve dont il vient rendre compte.
 */
public record IzumiActionPayload(int entityId, String action) implements CustomPacketPayload {
	public static final String SPAR = "spar";
	public static final Type<IzumiActionPayload> TYPE = new Type<>(Fmab.id("izumi_action"));

	public static final StreamCodec<ByteBuf, IzumiActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, IzumiActionPayload::entityId,
			ByteBufCodecs.stringUtf8(64), IzumiActionPayload::action,
			IzumiActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
