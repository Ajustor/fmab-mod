package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.NotebookContents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Le client a modifié son carnet : le serveur en remplace le contenu. */
public record SaveNotebookPayload(NotebookContents contents) implements CustomPacketPayload {
	public static final Type<SaveNotebookPayload> TYPE = new Type<>(Fmab.id("save_notebook"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SaveNotebookPayload> CODEC =
			NotebookContents.STREAM_CODEC.map(SaveNotebookPayload::new, SaveNotebookPayload::contents);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
