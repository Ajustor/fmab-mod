package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Offrir une âme pour rendre à un allié ce que la Porte lui a pris (voir {@code SoulGifts}) : ce que le
 * serveur demande à l'un des deux joueurs.
 *
 * @param step       {@link #CHOOSE} (le donneur choisit ce qu'il rend et ce qu'il paie), {@link #PROPOSE}
 *                   (l'allié accepte ou refuse) ou {@link #CONFIRM} (le donneur qui donne son âme confirme)
 * @param other      le nom de l'autre joueur
 * @param part       la partie rendue (vide au premier choix)
 * @param payment    {@code own_soul}, {@code stone} ou {@code gate} (vide au premier choix)
 * @param parts      ce que l'allié a perdu (au premier choix)
 * @param stoneSouls les âmes de la Pierre que tient le donneur (0 sans Pierre)
 * @param ownSoul    le donneur a encore une âme à donner (un corps)
 * @param gate       le donneur a encore sa Porte à donner (pour un corps entier)
 */
public record SoulGiftPayload(int step, String other, String part, String payment, List<String> parts, int stoneSouls,
		boolean ownSoul, boolean gate) implements CustomPacketPayload {
	public static final int CHOOSE = 0;
	public static final int PROPOSE = 1;
	public static final int CONFIRM = 2;

	public static final Type<SoulGiftPayload> TYPE = new Type<>(Fmab.id("soul_gift"));
	public static final StreamCodec<ByteBuf, SoulGiftPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SoulGiftPayload::step,
			ByteBufCodecs.STRING_UTF8, SoulGiftPayload::other,
			ByteBufCodecs.STRING_UTF8, SoulGiftPayload::part,
			ByteBufCodecs.STRING_UTF8, SoulGiftPayload::payment,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(8)), SoulGiftPayload::parts,
			ByteBufCodecs.VAR_INT, SoulGiftPayload::stoneSouls,
			ByteBufCodecs.BOOL, SoulGiftPayload::ownSoul,
			ByteBufCodecs.BOOL, SoulGiftPayload::gate,
			SoulGiftPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
