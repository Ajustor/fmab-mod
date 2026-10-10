package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La page de conception : les objets que l'alchimiste a compris, avec ce que chacun coûte (le
 * client n'a pas les recettes pour le calculer), et celui auquel il pense en ce moment.
 */
public record OpenDesignsPayload(List<Entry> entries, String current) implements CustomPacketPayload {
	public static final Type<OpenDesignsPayload> TYPE = new Type<>(Fmab.id("open_designs"));

	/** Un objet compris et sa masse par élément. */
	public record Entry(String item, Map<String, Integer> cost) {
		public static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Entry::item,
				ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), Entry::cost,
				Entry::new);
	}

	public static final StreamCodec<ByteBuf, OpenDesignsPayload> CODEC = StreamCodec.composite(
			Entry.CODEC.apply(ByteBufCodecs.list()), OpenDesignsPayload::entries,
			ByteBufCodecs.STRING_UTF8, OpenDesignsPayload::current,
			OpenDesignsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
