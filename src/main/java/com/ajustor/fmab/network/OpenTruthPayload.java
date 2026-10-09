package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * La Vérité s'adresse à une âme errante : se faire rappeler dans un sceau, ou repartir de zéro.
 *
 * @param wipes repartir de zéro efface-t-il la progression (réglage du serveur) ?
 */
public record OpenTruthPayload(int entityId, boolean wipes) implements CustomPacketPayload {
	public static final Type<OpenTruthPayload> TYPE = new Type<>(Fmab.id("open_truth"));

	public static final StreamCodec<ByteBuf, OpenTruthPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, OpenTruthPayload::entityId,
			ByteBufCodecs.BOOL, OpenTruthPayload::wipes,
			OpenTruthPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
