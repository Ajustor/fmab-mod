package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le serveur ouvre l'atelier de Winry. Le client connaît déjà ses automails et ses membres perdus
 * (attachements synchronisés) : il ne faut que l'identité de Winry.
 */
public record OpenWinryPayload(int entityId) implements CustomPacketPayload {
	public static final Type<OpenWinryPayload> TYPE = new Type<>(Fmab.id("open_winry"));

	public static final StreamCodec<ByteBuf, OpenWinryPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, OpenWinryPayload::entityId,
			OpenWinryPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
