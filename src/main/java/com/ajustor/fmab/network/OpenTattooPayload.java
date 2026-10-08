package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.registry.FmabAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Le serveur ouvre le rituel : le cercle qui sera tatoué et les emplacements déjà pris. Les tracés
 * des tatouages, eux, sont déjà sur le client (attachement synchronisé).
 */
public record OpenTattooPayload(BlockPos circle, List<String> occupied) implements CustomPacketPayload {
	public static final Type<OpenTattooPayload> TYPE = new Type<>(Fmab.id("open_tattoo"));

	public static final StreamCodec<ByteBuf, OpenTattooPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OpenTattooPayload::circle,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenTattooPayload::occupied,
			OpenTattooPayload::new);

	public static OpenTattooPayload of(ServerPlayer player, BlockPos circle) {
		return new OpenTattooPayload(circle,
				List.copyOf(player.getAttachedOrCreate(FmabAttachments.TATTOOS).slots().keySet()));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
