package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Une cinématique à jouer à l'écran : bandes noires, fondus, et pour certaines des images (l'œil de
 * la Porte, le torrent du savoir, le titre du Jour promis). Le client la joue pendant {@code ticks}.
 */
public record CinematicPayload(String kind, int ticks) implements CustomPacketPayload {
	/** Tiré vers la Porte, depuis le cercle : les bras noirs, puis le noir. */
	public static final String GATE_PULL = "gate_pull";
	/** Devant la Porte, toute la visite durant : les bandes noires. */
	public static final String GATE = "gate";
	/** Le savoir déferle : l'œil s'ouvre, le torrent des symboles, puis le blanc. */
	public static final String GATE_KNOWLEDGE = "gate_knowledge";
	/** L'éclipse du Jour promis : le titre, le ciel rouge. */
	public static final String PROMISED_DAY = "promised_day";
	/** Père tombe : les bras de la Vérité l'emportent, éclair blanc. */
	public static final String FATHER_FALL = "father_fall";

	public static final Type<CinematicPayload> TYPE = new Type<>(Fmab.id("cinematic"));

	public static final StreamCodec<ByteBuf, CinematicPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, CinematicPayload::kind,
			ByteBufCodecs.VAR_INT, CinematicPayload::ticks,
			CinematicPayload::new);

	/** Joue la cinématique chez ce joueur. */
	public static void play(ServerPlayer player, String kind, int ticks) {
		ServerPlayNetworking.send(player, new CinematicPayload(kind, ticks));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
