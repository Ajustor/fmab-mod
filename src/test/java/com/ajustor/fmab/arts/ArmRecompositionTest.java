package com.ajustor.fmab.arts;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * La matière que prend le bras de Scar pour recomposer : les blocs touchés, du plus proche au plus
 * lointain, seulement ceux qui apportent ce qui manque, et rien du tout s'ils ne suffisent pas.
 */
class ArmRecompositionTest {
	private static final Map<String, Double> DIRT = Map.of("earth", 1.0);
	private static final Map<String, Double> IRON_BLOCK = Map.of("iron", 9.0);
	private static final Map<String, Double> PLANKS = Map.of("wood", 1.0);
	private static final Map<String, Double> IRON_NUGGETISH = Map.of("iron", 1.0);

	@Test
	void theNearestUsefulBlocksPayAndTheRestStays() {
		// Une pioche de fer : trois de fer, un de bois. La terre ne sert à rien, le second bloc de fer non plus.
		List<Map<String, Double>> blocks = List.of(DIRT, IRON_BLOCK, PLANKS, IRON_BLOCK);
		assertEquals(Optional.of(List.of(1, 2)), ArmRecomposition.pick(blocks, Map.of("iron", 3, "wood", 1)));
	}

	@Test
	void smallBlocksAddUpFromTheNearest() {
		List<Map<String, Double>> blocks = List.of(IRON_NUGGETISH, IRON_NUGGETISH, IRON_NUGGETISH, IRON_BLOCK);
		assertEquals(Optional.of(List.of(0, 1, 2)), ArmRecomposition.pick(blocks, Map.of("iron", 3)));
	}

	@Test
	void notEnoughMatterTakesNothing() {
		assertEquals(Optional.empty(), ArmRecomposition.pick(List.of(PLANKS, DIRT), Map.of("iron", 3, "wood", 1)));
	}

	@Test
	void whatTheBlocksHoldIsCountedElementByElement() {
		Map<String, Double> total = ArmRecomposition.available(List.of(IRON_BLOCK, PLANKS, PLANKS));
		assertEquals(9.0, total.get("iron"), 1e-9);
		assertEquals(2.0, total.get("wood"), 1e-9);
	}
}
