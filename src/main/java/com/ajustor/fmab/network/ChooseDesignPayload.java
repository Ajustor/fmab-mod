package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** L'objet auquel l'alchimiste pense désormais en recomposant ; vide pour n'y plus penser. */
public record ChooseDesignPayload(String item) implements CustomPacketPayload {
	public static final Type<ChooseDesignPayload> TYPE = new Type<>(Fmab.id("choose_design"));
	public static final StreamCodec<ByteBuf, ChooseDesignPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, ChooseDesignPayload::item, ChooseDesignPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
