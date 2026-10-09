package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * La roue des cercles (ou une touche de passage) sélectionne une page du carnet.
 *
 * @param clap joindre aussitôt les mains avec ce cercle (Initiés de la Porte)
 */
public record SelectCirclePayload(int index, boolean clap) implements CustomPacketPayload {
	public static final Type<SelectCirclePayload> TYPE = new Type<>(Fmab.id("select_circle"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SelectCirclePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SelectCirclePayload::index,
			ByteBufCodecs.BOOL, SelectCirclePayload::clap,
			SelectCirclePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
