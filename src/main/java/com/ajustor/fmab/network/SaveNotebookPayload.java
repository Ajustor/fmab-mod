package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.NotebookContents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

/** Le client a modifié son carnet : le serveur remplace le contenu de l'objet tenu en main. */
public record SaveNotebookPayload(InteractionHand hand, NotebookContents contents) implements CustomPacketPayload {
	public static final Type<SaveNotebookPayload> TYPE = new Type<>(Fmab.id("save_notebook"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SaveNotebookPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL.map(b -> b ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
					h -> h == InteractionHand.OFF_HAND),
			SaveNotebookPayload::hand,
			NotebookContents.STREAM_CODEC, SaveNotebookPayload::contents,
			SaveNotebookPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
