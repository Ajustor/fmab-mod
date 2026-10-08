package com.ajustor.fmab.alchemy.exchange;

import java.util.Locale;

/** Famille de matière : une transmutation n'en sort jamais sans Pierre philosophale. */
public enum Family {
	MINERAL,
	METAL,
	ORGANIC,
	CRYSTAL;

	public String serializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static Family fromSerializedName(String name) {
		return valueOf(name.toUpperCase(Locale.ROOT));
	}
}
