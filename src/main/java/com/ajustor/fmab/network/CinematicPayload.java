package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
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
	/** L'épilogue : ce que deviennent les personnages, une fois Père tombé et Hohenheim parti. */
	public static final String EPILOGUE = "epilogue";
	/** Ses pages, de {@code cinematic.fmab.epilogue.1} à la dernière, puis « Fin ». */
	public static final int EPILOGUE_PAGES = 6;
	public static final int EPILOGUE_PAGE_TICKS = 100;
	public static final int EPILOGUE_TICKS = (EPILOGUE_PAGES + 1) * EPILOGUE_PAGE_TICKS;
	/** Nina : ce que Tucker a fait de sa fille, en quelques pages ({@code cinematic.fmab.nina.N}). */
	public static final String NINA = "nina";
	public static final int NINA_PAGES = 4;
	public static final int NINA_TICKS = (NINA_PAGES + 1) * EPILOGUE_PAGE_TICKS;

	public static final Type<CinematicPayload> TYPE = new Type<>(Fmab.id("cinematic"));

	public static final StreamCodec<ByteBuf, CinematicPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, CinematicPayload::kind,
			ByteBufCodecs.VAR_INT, CinematicPayload::ticks,
			CinematicPayload::new);

	/** Joue la cinématique chez ce joueur. */
	public static void play(ServerPlayer player, String kind, int ticks) {
		FmabNetwork.send(player, new CinematicPayload(kind, ticks));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
