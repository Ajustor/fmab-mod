package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Le marché de la Vérité : ce qu'elle a pris, ce que coûte chaque partie, et les âmes des Pierres
 * de l'alchimiste. {@link #CLOSE} referme l'écran.
 *
 * @param parts les parties perdues ({@code left_arm}…)
 * @param costs leur prix en âmes, dans le même ordre
 */
public record BargainPayload(List<String> parts, List<Integer> costs, int souls) implements CustomPacketPayload {
	public static final Type<BargainPayload> TYPE = new Type<>(Fmab.id("bargain"));
	public static final BargainPayload CLOSE = new BargainPayload(List.of(), List.of(), -1);
	public static final StreamCodec<ByteBuf, BargainPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), BargainPayload::parts,
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), BargainPayload::costs,
			ByteBufCodecs.VAR_INT, BargainPayload::souls,
			BargainPayload::new);

	public BargainPayload {
		parts = List.copyOf(parts);
		costs = List.copyOf(costs);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
