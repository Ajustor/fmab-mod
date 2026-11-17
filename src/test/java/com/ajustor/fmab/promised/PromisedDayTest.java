package com.ajustor.fmab.promised;

import com.ajustor.fmab.TestBootstrap;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le Jour promis : quand l'éclipse éveille le cercle, et l'abri des points de sang scellés. */
class PromisedDayTest {
	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void aSealedCrestShelters() {
		Set<BlockPos> sealed = Set.of(new BlockPos(100, 64, 100));
		assertTrue(PromisedDay.sheltered(new BlockPos(110, 70, 110), sealed), "à quelques pas");
		assertTrue(PromisedDay.sheltered(new BlockPos(100, 0, 100 + (int) PromisedDay.SHELTER), sealed), "au bord");
		assertFalse(PromisedDay.sheltered(new BlockPos(100, 64, 100 + (int) PromisedDay.SHELTER + 1), sealed), "au-delà");
		assertFalse(PromisedDay.sheltered(BlockPos.ZERO, Set.of()), "sans point scellé, aucun abri");
	}

	@Test
	void theCircleAwakensOnlyOnceEnoughGatesAreOpened() {
		assertFalse(NationalCircle.wakes(false, false, 0, 5), "personne n'a ouvert sa Porte");
		assertFalse(NationalCircle.wakes(false, false, 4, 5), "il en manque un");
		assertTrue(NationalCircle.wakes(false, false, 5, 5), "cinq sacrifices : le cercle s'éveille");
		assertTrue(NationalCircle.wakes(false, false, 0, 0), "sans exigence, chaque éclipse l'éveille");
		assertFalse(NationalCircle.wakes(true, false, 9, 5), "brisé, il ne s'éveille plus");
		assertFalse(NationalCircle.wakes(false, true, 9, 5), "Père tombé, plus de Jour promis");
	}

	@Test
	void aGateOpenedTwiceCountsOnce() {
		NationalCircle circle = new NationalCircle();
		java.util.UUID ed = java.util.UUID.randomUUID();
		circle.sacrifice(ed);
		circle.sacrifice(ed);
		assertEquals(1, circle.sacrifices(), "un joueur, un sacrifice");
		assertFalse(circle.awake(2));
		circle.sacrifice(java.util.UUID.randomUUID());
		assertTrue(circle.awake(2));
	}
}
