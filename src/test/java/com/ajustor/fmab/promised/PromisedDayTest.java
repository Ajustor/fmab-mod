package com.ajustor.fmab.promised;

import com.ajustor.fmab.TestBootstrap;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pendant le Jour promis, un point de sang scellé abrite l'alchimie autour de lui. */
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
}
