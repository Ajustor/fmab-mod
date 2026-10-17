package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Le sacrifice d'un membre pour lier une âme (voir {@code SoulOffering}) : ce que le serveur demande à
 * l'un des deux joueurs.
 *
 * @param step  {@link #CHOOSE} (le donneur choisit ce qu'il offre), {@link #PROPOSE} (l'âme errante
 *              accepte ou refuse) ou {@link #CONFIRM} (le donneur confirme, une seconde fois)
 * @param other le nom de l'autre joueur (vide au premier choix)
 * @param part  la partie offerte ({@code BodyPart}, en minuscules ; vide au premier choix)
 * @param parts ce que le donneur peut offrir (au premier choix)
 */
public record SoulOfferPayload(int step, String other, String part, List<String> parts) implements CustomPacketPayload {
	public static final int CHOOSE = 0;
	public static final int PROPOSE = 1;
	public static final int CONFIRM = 2;

	public static final Type<SoulOfferPayload> TYPE = new Type<>(Fmab.id("soul_offer"));
	public static final StreamCodec<ByteBuf, SoulOfferPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SoulOfferPayload::step,
			ByteBufCodecs.STRING_UTF8, SoulOfferPayload::other,
			ByteBufCodecs.STRING_UTF8, SoulOfferPayload::part,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(8)), SoulOfferPayload::parts,
			SoulOfferPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
