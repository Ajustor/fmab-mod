package com.ajustor.fmab.world;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La recherche de la place d'un lieu unique : les cases de la grille, de l'origine vers le large. */
class UniqueSiteTest {
	@Test
	void eachRingHoldsTheCellsAtThatDistance() {
		assertEquals(1, UniqueSite.ring(0).size(), "l'origine seule");
		assertEquals(8, UniqueSite.ring(1).size());
		assertEquals(16, UniqueSite.ring(2).size());
	}

	@Test
	void ringsCoverTheGridWithoutOverlap() {
		Set<List<Integer>> seen = new HashSet<>();
		for (int ring = 0; ring <= 5; ring++) {
			for (int[] cell : UniqueSite.ring(ring)) {
				assertTrue(seen.add(List.of(cell[0], cell[1])), "une case n'est visitée qu'une fois");
			}
		}
		assertEquals(11 * 11, seen.size(), "toutes les cases jusqu'à cinq anneaux");
	}

	@Test
	void closerChunksComeFirst() {
		assertTrue(UniqueSite.distanceFromOrigin(new ChunkPos(3, 4)) < UniqueSite.distanceFromOrigin(new ChunkPos(-6, 1)));
	}
}
