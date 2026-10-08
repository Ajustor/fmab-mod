package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le joueur demande à Winry de réparer ou de retirer l'automail d'un membre.
 *
 * @param part   le membre ({@code right_arm}…)
 * @param repair vrai pour réparer, faux pour retirer
 */
public record WinryActionPayload(int entityId, String part, boolean repair) implements CustomPacketPayload {
	public static final Type<WinryActionPayload> TYPE = new Type<>(Fmab.id("winry_action"));

	public static final StreamCodec<ByteBuf, WinryActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, WinryActionPayload::entityId,
			ByteBufCodecs.STRING_UTF8, WinryActionPayload::part,
			ByteBufCodecs.BOOL, WinryActionPayload::repair,
			WinryActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
