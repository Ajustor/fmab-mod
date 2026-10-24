package com.ajustor.fmab.transmutation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les formes des outils de l'alchimie : quel arbre tombe, où passe un tunnel, un pont, un terrassement. */
class WorkShapesTest {
	private static final BlockPos ROOT = new BlockPos(0, 64, 0);
	private static final java.util.function.Predicate<BlockPos> ANYWHERE = p -> true;

	/** Un chêne : un tronc de cinq, une branche en diagonale, des feuilles autour de la cime. */
	private static Set<BlockPos> oak() {
		Set<BlockPos> logs = new HashSet<>();
		for (int y = 0; y < 5; y++) {
			logs.add(ROOT.above(y));
		}
		logs.add(ROOT.above(5).east());
		return logs;
	}

	private static Set<BlockPos> crown(Set<BlockPos> logs) {
		Set<BlockPos> leaves = new HashSet<>();
		for (BlockPos log : logs) {
			if (log.getY() >= ROOT.getY() + 3) {
				for (Direction d : Direction.values()) {
					if (!logs.contains(log.relative(d))) {
						leaves.add(log.relative(d));
					}
				}
			}
		}
		return leaves;
	}

	@Test
	void aWholeTreeFallsBranchesIncludedFromAnyOfItsLogs() {
		Set<BlockPos> logs = oak();
		Set<BlockPos> leaves = crown(logs);
		assertEquals(logs, WorkShapes.tree(ROOT, logs::contains, leaves::contains, ANYWHERE), "depuis le pied");
		assertEquals(logs, WorkShapes.tree(ROOT.above(5).east(), logs::contains, leaves::contains, ANYWHERE), "depuis la branche");
	}

	@Test
	void aBeamOrALogCabinIsNotATree() {
		Set<BlockPos> logs = oak();
		assertEquals(Set.of(), WorkShapes.tree(ROOT, logs::contains, p -> false, ANYWHERE),
				"sans feuilles poussées d'elles-mêmes, ce sont des rondins posés");
		assertEquals(Set.of(), WorkShapes.tree(ROOT.west(), logs::contains, crown(logs)::contains, ANYWHERE),
				"pas de bûche, pas d'arbre");
	}

	/**
	 * En forêt dense, les branches des arbres voisins se touchent : tout le bosquet ne fait qu'un. Avant,
	 * passé 256 bûches, le cercle n'abattait rien du tout ; il abat maintenant ce qui est près de lui.
	 */
	@Test
	void aDenseForestIsFelledNearTheCircleInsteadOfNotAtAll() {
		Set<BlockPos> logs = new HashSet<>();
		for (int x = 0; x < 40; x++) {
			for (int y = 0; y < 8; y++) {
				logs.add(ROOT.offset(x, y, 0));
			}
		}
		Set<BlockPos> felled = WorkShapes.tree(ROOT, logs::contains, p -> true, WorkShapes.felling(ROOT, 10));
		assertEquals(11 * 8, felled.size(), "les troncs à dix blocs, pas le reste de la forêt");
		assertTrue(felled.stream().allMatch(p -> p.getX() - ROOT.getX() <= 10));
		assertEquals(Set.of(), WorkShapes.tree(ROOT.offset(20, 0, 0), logs::contains, p -> true, WorkShapes.felling(ROOT, 10)),
				"une bûche hors de la zone ne compte pas");
	}

	@Test
	void evenAHugeForestStopsAtTheLimit() {
		Set<BlockPos> felled = WorkShapes.tree(ROOT, p -> true, p -> true, ANYWHERE);
		assertEquals(WorkShapes.TREE_LIMIT, felled.size());
	}

	@Test
	void aTunnelIsThreeByThreeAndGoesStraightAhead() {
		List<BlockPos> bore = WorkShapes.tunnel(ROOT, Direction.NORTH, 8, 0);
		assertEquals(8 * 9, bore.size());
		assertEquals(ROOT.west(), bore.getFirst(), "le plus près d'abord");
		assertTrue(bore.contains(ROOT.north(7).above(2)), "jusqu'à la tête, et un bloc au-dessus");
		assertTrue(bore.stream().allMatch(p -> p.getY() >= ROOT.getY()), "au sol, il part au ras du sol");
		List<BlockPos> wall = WorkShapes.tunnel(ROOT, Direction.EAST, 4, -1);
		assertTrue(wall.contains(ROOT.below()) && wall.contains(ROOT.above()), "sur un mur, centré sur le cercle");
		List<BlockPos> shaft = WorkShapes.tunnel(ROOT, Direction.UP, 3, 0);
		assertTrue(shaft.contains(ROOT.above(2).north().east()) && shaft.stream().allMatch(p -> p.getY() >= ROOT.getY()),
				"au plafond, un puits qui monte");
	}

	@Test
	void aBridgeIsLevelAndThreeWide() {
		List<BlockPos> deck = WorkShapes.bridge(ROOT, Direction.SOUTH, 8);
		assertEquals(24, deck.size());
		assertTrue(deck.stream().allMatch(p -> p.getY() == ROOT.getY()), "de niveau");
		assertTrue(deck.contains(ROOT.south(8).east()) && deck.contains(ROOT.south().west()));
		assertTrue(!deck.contains(ROOT), "pas sur le cercle");
	}

	@Test
	void levellingPlanesFromTheTopAndFillsFromTheBottom() {
		WorkShapes.Leveling plan = WorkShapes.leveling(ROOT, 63, 3, 6, 3);
		assertEquals(7 * 7 * 6, plan.cut().size());
		assertEquals(7 * 7 * 4, plan.fill().size());
		assertEquals(69, plan.cut().getFirst().getY(), "on rabote par le haut : rien ne reste en l'air");
		assertEquals(60, plan.fill().getFirst().getY(), "on comble par le bas : le sable ne tombe pas");
		assertEquals(63, plan.fill().getLast().getY(), "jusqu'au niveau du sol qui porte le cercle");
		assertTrue(plan.cut().stream().allMatch(p -> p.getY() > 63));
	}
}
