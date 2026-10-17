package com.ajustor.fmab.world;

/**
 * Ce que le reste du mod peut dire des lieux uniques (voir UniqueSite) : pendant un
 * {@code /place structure}, un lieu se pose où on le demande.
 */
public final class UniqueSites {
	private static final ThreadLocal<Boolean> FORCED = ThreadLocal.withInitial(() -> false);

	private UniqueSites() {
	}

	public static void forced(boolean value) {
		FORCED.set(value);
	}

	static boolean forced() {
		return FORCED.get();
	}
}
