package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.tattoo.BodyCircles;
import com.ajustor.fmab.tattoo.TattooSlot;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

/**
 * Le serveur ouvre le rituel : le cercle qui sera tatoué (ou gravé), les emplacements déjà pris, et
 * l'outil en main. Les tracés
 * des tatouages et des gravures, eux, sont déjà sur le client (attachements synchronisés).
 */
public record OpenTattooPayload(BlockPos circle, List<String> occupied, boolean engrave)
		implements CustomPacketPayload {
	public static final Type<OpenTattooPayload> TYPE = new Type<>(Fmab.id("open_tattoo"));

	public static final StreamCodec<ByteBuf, OpenTattooPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OpenTattooPayload::circle,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenTattooPayload::occupied,
			ByteBufCodecs.BOOL, OpenTattooPayload::engrave,
			OpenTattooPayload::new);

	/**
	 * Les emplacements pris : par un tatouage, ou par une gravure de l'automail qui remplace le bras.
	 *
	 * @param engrave ouvert au burin (graver l'acier) plutôt qu'à l'encre (tatouer la peau)
	 */
	public static OpenTattooPayload of(ServerPlayer player, BlockPos circle, boolean engrave) {
		return new OpenTattooPayload(circle, Arrays.stream(TattooSlot.values())
				.filter(slot -> BodyCircles.full(player, slot))
				.map(TattooSlot::id)
				.toList(), engrave);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
