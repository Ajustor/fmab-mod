package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.data.FmabCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le Traité recopie un cercle d'exemple dans le carnet du joueur.
 *
 * @param name nom de la page ; {@code @clé} pour un nom traduit à l'affichage
 */
public record AddToNotebookPayload(String name, Drawing drawing) implements CustomPacketPayload {
	public static final Type<AddToNotebookPayload> TYPE = new Type<>(Fmab.id("add_to_notebook"));

	public static final StreamCodec<RegistryFriendlyByteBuf, AddToNotebookPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(64), AddToNotebookPayload::name,
			ByteBufCodecs.fromCodecWithRegistries(FmabCodecs.DRAWING), AddToNotebookPayload::drawing,
			AddToNotebookPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
