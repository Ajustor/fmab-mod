package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.tattoo.BodyArt;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** L'éditeur envoie le dessin d'un emplacement ; le serveur le vérifie avant de le poser. */
public record TattooArtPayload(String slot, BodyArt art) implements CustomPacketPayload {
	public static final Type<TattooArtPayload> TYPE = new Type<>(Fmab.id("tattoo_art"));
	/** Un dessin au plus lourd ({@link BodyArt#MAX_POINTS} points, un cercle plein de traits). */
	public static final int MAX_SIZE = 1 << 18;

	public static final StreamCodec<RegistryFriendlyByteBuf, TattooArtPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(32), TattooArtPayload::slot,
			ByteBufCodecs.fromCodecWithRegistries(BodyArt.CODEC), TattooArtPayload::art,
			TattooArtPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
