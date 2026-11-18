package com.ajustor.fmab.transmutation;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La forme des éclairs : des traits continus qui montent depuis le cercle, et qui crépitent. */
class BoltShapeTest {
	@Test
	void theSameSeedDrawsTheSameBolts() {
		assertEquals(BoltShape.of(42, 2, 5), BoltShape.of(42, 2, 5));
		assertNotEquals(BoltShape.of(42, 2, 5), BoltShape.of(43, 2, 5), "une nouvelle graine, une nouvelle forme : l'éclair crépite");
	}

	@Test
	void eachBoltRisesFromTheCircleInOneUnbrokenLine() {
		for (long seed = 0; seed < 200; seed++) {
			double radius = seed % 4;
			List<BoltShape.Segment> trunk = BoltShape.of(seed, radius, 6).segments().stream().filter(s -> !s.branch()).toList();
			int roots = 0;
			for (int i = 0; i < trunk.size(); i++) {
				BoltShape.Segment s = trunk.get(i);
				assertTrue(s.to().y > s.from().y, "un éclair monte : " + s);
				assertTrue(s.to().y - s.from().y <= BoltShape.MAX_RISE + 1e-9, "un trait ne bondit pas : " + s);
				if (s.from().y == 0) {
					roots++;
					assertTrue(Math.hypot(s.from().x, s.from().z) <= radius + 1e-9, "il part du cercle : " + s);
				} else {
					assertEquals(trunk.get(i - 1).to(), s.from(), "le tronc ne se rompt pas");
				}
			}
			assertEquals(6, roots, "autant d'éclairs que demandé");
		}
	}

	@Test
	void branchesGrowFromTheTrunkAndAreThinner() {
		int branches = 0;
		for (long seed = 0; seed < 200; seed++) {
			List<BoltShape.Segment> all = BoltShape.of(seed, 1.5, 4).segments();
			List<Vec3> trunkPoints = all.stream().filter(s -> !s.branch()).map(BoltShape.Segment::from).toList();
			for (int i = 0; i < all.size(); i++) {
				BoltShape.Segment s = all.get(i);
				if (!s.branch()) {
					continue;
				}
				branches++;
				assertTrue(s.width() < BoltShape.ROOT_WIDTH, "une branche est plus fine que la racine du tronc");
				BoltShape.Segment before = all.get(i - 1);
				assertTrue(before.branch() ? before.to().equals(s.from()) : trunkPoints.contains(s.from()),
						"une branche part du tronc, sans se rompre");
			}
		}
		assertTrue(branches > 0, "des éclairs se ramifient");
	}

	@Test
	void aBoltFromAPointStartsAtThatPoint() {
		BoltShape.Segment first = BoltShape.of(7, 0, 1).segments().getFirst();
		assertEquals(0, first.from().length(), 1e-12);
	}

	@Test
	void biggerCirclesCrackleWithMoreBoltsForLonger() {
		assertTrue(TransmutationLightning.bolts(4, 1) > TransmutationLightning.bolts(1, 1));
		assertEquals(1, TransmutationLightning.bolts(0.1, 0.1), "même un cercle qui fait long feu lâche un éclair");
		assertEquals(TransmutationLightning.MAX_BOLTS, TransmutationLightning.bolts(200, 2), "un cercle géant reste lisible");
		assertEquals(TransmutationLightning.DURATION, TransmutationLightning.lifetime(1));
		assertEquals(3, TransmutationLightning.lifetime(0.1));
	}
}
