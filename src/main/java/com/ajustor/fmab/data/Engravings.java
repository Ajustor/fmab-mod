package com.ajustor.fmab.data;

import com.ajustor.fmab.tattoo.BodyArt;
import com.ajustor.fmab.tattoo.Sleeve;
import com.ajustor.fmab.tattoo.TattooSlot;
import com.mojang.serialization.Codec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Les gravures d'un bras d'automail (dessins et cercles), par endroit du bras
 * ({@link TattooSlot#spot()} : {@code palm}, {@code arm}). La gravure est dans l'acier : elle suit
 * la pièce quand on la retire, d'un bras à l'autre, et ne disparaît que si la pièce lâche. Une pièce
 * gravée avant le tour des bras ({@code forearm}) se relit gravée au même endroit du tour du bras.
 */
public record Engravings(Map<String, BodyArt> spots) {
	public static final Engravings NONE = new Engravings(Map.of());
	private static final String LEGACY_FOREARM = "forearm";
	private static final String ARM = "arm";

	public static final Codec<Engravings> CODEC = Codec.unboundedMap(Codec.STRING, BodyArt.CODEC)
			.xmap(Engravings::new, Engravings::spots);

	public Engravings {
		Map<String, BodyArt> kept = new HashMap<>();
		spots.forEach((spot, art) -> {
			if (!art.isEmpty()) {
				kept.put(spot, art);
			}
		});
		BodyArt forearm = kept.remove(LEGACY_FOREARM);
		if (forearm != null) {
			kept.putIfAbsent(ARM, Sleeve.fromForearm(forearm));
		}
		spots = Map.copyOf(kept);
	}

	public Optional<BodyArt> get(TattooSlot slot) {
		return slot.spot().map(spots::get);
	}

	public Engravings with(TattooSlot slot, BodyArt art) {
		String spot = slot.spot().orElseThrow(() -> new IllegalArgumentException(slot + " n'est pas sur un bras"));
		Map<String, BodyArt> out = new HashMap<>(spots);
		out.put(spot, art);
		return new Engravings(out);
	}

	public Engravings without(TattooSlot slot) {
		Map<String, BodyArt> out = new HashMap<>(spots);
		slot.spot().ifPresent(out::remove);
		return new Engravings(out);
	}

	public boolean isEmpty() {
		return spots.isEmpty();
	}
}
