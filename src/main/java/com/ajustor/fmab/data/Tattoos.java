package com.ajustor.fmab.data;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.tattoo.BodyArt;
import com.ajustor.fmab.tattoo.TattooSlot;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Ce que la peau porte, par emplacement ({@link TattooSlot#id()}) : dessins et cercles tatoués.
 * Permanents, gardés à la mort. Les sauvegardes d'avant les dessins (un cercle par emplacement) se
 * relisent en cercles posés en tampon.
 */
public record Tattoos(Map<String, BodyArt> slots) {
	public static final Tattoos NONE = new Tattoos(Map.of());

	public static final Codec<Tattoos> CODEC = Codec.unboundedMap(Codec.STRING, BodyArt.CODEC_OR_CIRCLE)
			.xmap(Tattoos::new, Tattoos::slots);

	public static final StreamCodec<RegistryFriendlyByteBuf, Tattoos> STREAM_CODEC =
			ByteBufCodecs.fromCodecWithRegistries(CODEC);

	public Tattoos {
		Map<String, BodyArt> kept = new HashMap<>();
		slots.forEach((slot, art) -> {
			if (!art.isEmpty()) {
				kept.put(slot, art);
			}
		});
		slots = Map.copyOf(kept);
	}

	/** Le cercle alchimique tatoué à cet emplacement, s'il y en a un. */
	public Optional<Drawing> get(TattooSlot slot) {
		return art(slot).flatMap(BodyArt::circle);
	}

	/** Tout ce que la peau porte à cet emplacement. */
	public Optional<BodyArt> art(TattooSlot slot) {
		return Optional.ofNullable(slots.get(slot.id()));
	}

	/** Tatoue ce cercle à cet emplacement, en tampon plein cadre, par-dessus les dessins qui y sont. */
	public Tattoos with(TattooSlot slot, Drawing circle) {
		BodyArt.Stamp stamped = BodyArt.ofCircle(circle, BodyArt.SKIN_INK).stamp().orElseThrow();
		return withArt(slot, art(slot).orElse(BodyArt.EMPTY).withStamp(stamped));
	}

	public Tattoos withArt(TattooSlot slot, BodyArt art) {
		Map<String, BodyArt> out = new HashMap<>(slots);
		out.put(slot.id(), art);
		return new Tattoos(out);
	}

	public Tattoos without(TattooSlot slot) {
		Map<String, BodyArt> out = new HashMap<>(slots);
		out.remove(slot.id());
		return new Tattoos(out);
	}
}
