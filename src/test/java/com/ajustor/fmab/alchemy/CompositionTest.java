package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.exchange.Composition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** De quoi un objet est fait, d'après ses recettes : la base de la recomposition d'après modèle. */
class CompositionTest {
	private static final Map<String, Composition> BASE = Map.of(
			"iron_ingot", Composition.of("iron", 9),
			"oak_planks", Composition.of("wood", 2),
			"birch_planks", Composition.of("wood", 2),
			"cobblestone", Composition.of("earth", 1));

	private static Composition.Recipe<String> recipe(String result, int count, List<String>... ingredients) {
		return new Composition.Recipe<>(List.of(ingredients), result, count);
	}

	@Test
	void aPickaxeIsThreeIngotsAndTwoSticks() {
		var values = Composition.compute(BASE, List.of(
				recipe("stick", 4, List.of("oak_planks", "birch_planks"), List.of("oak_planks", "birch_planks")),
				recipe("iron_pickaxe", 1, List.of("iron_ingot"), List.of("iron_ingot"), List.of("iron_ingot"),
						List.of("stick"), List.of("stick"))), 8);
		assertEquals(1.0, values.get("stick").masses().get("wood"), 1e-9);
		Composition pickaxe = values.get("iron_pickaxe");
		assertEquals(Map.of("iron", 27, "wood", 2), pickaxe.cost());
		assertEquals("iron", pickaxe.principal());
	}

	@Test
	void recipesAreFollowedWhateverTheirOrder() {
		// La pioche est déclarée avant le bâton : un second tour la compose.
		var values = Composition.compute(BASE, List.of(
				recipe("stone_pickaxe", 1, List.of("cobblestone"), List.of("cobblestone"), List.of("cobblestone"),
						List.of("stick"), List.of("stick")),
				recipe("stick", 4, List.of("oak_planks"), List.of("oak_planks"))), 8);
		assertEquals(Map.of("earth", 3, "wood", 2), values.get("stone_pickaxe").cost());
		assertEquals("earth", values.get("stone_pickaxe").principal());
	}

	@Test
	void anIngredientWithoutValueLeavesTheObjectWithoutOne() {
		var values = Composition.compute(BASE, List.of(
				recipe("bow", 1, List.of("stick"), List.of("string")),
				recipe("stick", 4, List.of("oak_planks"), List.of("oak_planks"))), 8);
		assertFalse(values.containsKey("bow"));
	}

	@Test
	void theCheapestRecipeWinsAndBaseValuesStay() {
		var values = Composition.compute(BASE, List.of(
				recipe("chain", 1, List.of("iron_ingot"), List.of("iron_ingot")),
				recipe("chain", 1, List.of("iron_ingot")),
				// Une recette qui produirait un lingot ne change pas sa valeur de base.
				recipe("iron_ingot", 1, List.of("cobblestone"))), 8);
		assertEquals(9.0, values.get("chain").total(), 1e-9);
		assertEquals(Composition.of("iron", 9), values.get("iron_ingot"));
	}

	@Test
	void breakingDownLosesWhatTheSmallestPieceCannotHold() {
		Composition half = new Composition(Map.of("iron", 4.5, "wood", 0.5));
		assertEquals(Map.of("iron", 4, "wood", 0), half.yield());
		assertEquals(Map.of("iron", 5, "wood", 1), half.cost());
	}
}
