package com.ajustor.fmab.data;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.tattoo.TattooSlot;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Les cercles tatoués, par emplacement ({@link TattooSlot#id()}). Permanents, gardés à la mort. */
public record Tattoos(Map<String, Drawing> slots) {
	public static final Tattoos NONE = new Tattoos(Map.of());

	public static final Codec<Tattoos> CODEC = Codec.unboundedMap(Codec.STRING, FmabCodecs.DRAWING)
			.xmap(Tattoos::new, Tattoos::slots);

	public static final StreamCodec<RegistryFriendlyByteBuf, Tattoos> STREAM_CODEC =
			ByteBufCodecs.fromCodecWithRegistries(CODEC);

	public Tattoos {
		slots = Map.copyOf(slots);
	}

	public Optional<Drawing> get(TattooSlot slot) {
		return Optional.ofNullable(slots.get(slot.id()));
	}

	public Tattoos with(TattooSlot slot, Drawing drawing) {
		Map<String, Drawing> out = new HashMap<>(slots);
		out.put(slot.id(), drawing);
		return new Tattoos(out);
	}

	public Tattoos without(TattooSlot slot) {
		Map<String, Drawing> out = new HashMap<>(slots);
		out.remove(slot.id());
		return new Tattoos(out);
	}
}
