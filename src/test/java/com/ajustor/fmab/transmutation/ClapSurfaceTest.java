package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mains jointes : l'air, le feu et l'eau se tirent de l'air ambiant, sans surface ; tout élément qui
 * puise dans la matière en réclame une.
 */
class ClapSurfaceTest {
	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void airFireAndWaterNeedNoSurface() {
		assertFalse(GloveCasting.needsSurface(List.of(Set.of("air"))));
		assertFalse(GloveCasting.needsSurface(List.of(Set.of("air", "fire"), Set.of("water"))));
	}

	@Test
	void matterNeedsASurface() {
		assertTrue(GloveCasting.needsSurface(List.of(Set.of("earth"))));
		assertTrue(GloveCasting.needsSurface(List.of(Set.of("earth", "air"))), "un pont de terre puise dans le sol");
		assertTrue(GloveCasting.needsSurface(List.of(Set.of("fire"), Set.of("iron"))), "un seul effet de matière suffit");
	}
}
