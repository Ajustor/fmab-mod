package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.Training;
import com.ajustor.fmab.registry.FmabAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

/** Le serveur ouvre le dialogue d'Izumi, avec l'état des épreuves de ce joueur. */
public record OpenIzumiPayload(int entityId, List<String> achieved, List<String> rewarded)
		implements CustomPacketPayload {
	public static final Type<OpenIzumiPayload> TYPE = new Type<>(Fmab.id("open_izumi"));

	public static final StreamCodec<ByteBuf, OpenIzumiPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, OpenIzumiPayload::entityId,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenIzumiPayload::achieved,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenIzumiPayload::rewarded,
			OpenIzumiPayload::new);

	public static OpenIzumiPayload of(Entity izumi, ServerPlayer player) {
		Training training = player.getAttachedOrCreate(FmabAttachments.TRAINING);
		return new OpenIzumiPayload(izumi.getId(), List.copyOf(training.achieved()), List.copyOf(training.rewarded()));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
