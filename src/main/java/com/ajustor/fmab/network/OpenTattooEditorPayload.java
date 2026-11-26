package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le serveur ouvre l'éditeur de dessins sur le corps. Ce que porte chaque emplacement et le carnet
 * sont déjà sur le client (attachements synchronisés).
 *
 * @param engrave ouvert au burin (graver l'acier des automails) plutôt qu'à l'encre (dessiner sur la peau)
 */
public record OpenTattooEditorPayload(boolean engrave) implements CustomPacketPayload {
	public static final Type<OpenTattooEditorPayload> TYPE = new Type<>(Fmab.id("open_tattoo_editor"));

	public static final StreamCodec<ByteBuf, OpenTattooEditorPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, OpenTattooEditorPayload::engrave,
			OpenTattooEditorPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
