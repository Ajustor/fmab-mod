package com.ajustor.fmab.transmutation;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreatCircleTest {
	private static final BlockPos CENTER = new BlockPos(100, 64, -40);

	private static List<BlockPos> around(int count, int radius, double from, double to) {
		List<BlockPos> out = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			double a = from + (to - from) * i / count;
			out.add(CENTER.offset((int) Math.round(Math.cos(a) * radius), 0, (int) Math.round(Math.sin(a) * radius)));
		}
		return out;
	}

	@Test
	void relaysAllAroundFormAGreatCircle() {
		var ring = GreatCircle.of(CENTER, around(6, 40, 0, 2 * Math.PI));
		assertTrue(ring.isPresent());
		assertEquals(40, ring.get().radius(), 0.5);
		assertEquals(6, ring.get().relays().size());
	}

	@Test
	void aTriangleIsEnough() {
		assertTrue(GreatCircle.of(CENTER, around(3, 30, 0, 2 * Math.PI)).isPresent());
	}

	@Test
	void twoRelaysDoNotCloseACircle() {
		assertFalse(GreatCircle.of(CENTER, around(2, 30, 0, 2 * Math.PI)).isPresent());
	}

	@Test
	void anArcIsNotACircle() {
		// Cinq relais serrés sur un demi-tour : le cercle reste ouvert.
		assertFalse(GreatCircle.of(CENTER, around(5, 30, 0, Math.PI)).isPresent());
	}

	@Test
	void relaysMustStandAtTheSameDistance() {
		List<BlockPos> relays = around(6, 30, 0, 2 * Math.PI);
		relays.set(2, CENTER.offset(-60, 0, 10));
		assertFalse(GreatCircle.of(CENTER, relays).isPresent());
	}

	@Test
	void neighbouringCirclesAreNotAGreatCircle() {
		assertFalse(GreatCircle.of(CENTER, around(6, 4, 0, 2 * Math.PI)).isPresent());
	}

	@Test
	void relaysAreOrderedAroundTheCenter() {
		List<BlockPos> relays = new ArrayList<>(around(8, 50, 0, 2 * Math.PI));
		java.util.Collections.reverse(relays);
		var ring = GreatCircle.of(CENTER, relays).orElseThrow();
		assertEquals(relays.getLast(), ring.relays().getFirst());
	}

	@Test
	void theStoneCircleReachesItsRelays() {
		var ring = GreatCircle.of(CENTER, around(6, 120, 0, 2 * Math.PI)).orElseThrow();
		// Au-delà de la limite d'un rayon demandé : un cercle géant couvre une ville.
		assertTrue(com.ajustor.fmab.stone.StoneCircle.greatRadius(ring) > com.ajustor.fmab.stone.StoneCircle.MAX_RADIUS);
		assertTrue(com.ajustor.fmab.stone.StoneCircle.greatRadius(ring) >= 120);
	}

	@Test
	void theCircumferenceHasNoHoles() {
		List<BlockPos> ring = GreatCircle.circumference(CENTER, 20);
		for (BlockPos p : ring) {
			assertEquals(20, GreatCircle.distance(CENTER, p), 0.75);
			// Chaque bloc du tour touche le suivant, de côté ou par un coin : un mur sans brèche.
			long neighbours = ring.stream().filter(q -> !q.equals(p)
					&& Math.abs(q.getX() - p.getX()) <= 1 && Math.abs(q.getZ() - p.getZ()) <= 1).count();
			assertTrue(neighbours >= 2, "un trou dans le tour près de " + p);
		}
	}

	@Test
	void aLoneCircleShowsWhereTheFirstRelayGoes() {
		BlockPos player = CENTER.offset(20, 0, 0);
		var plan = GreatCircle.plan(List.of(CENTER), player).orElseThrow();
		assertEquals(CENTER, plan.center());
		assertEquals(20, plan.radius(), 0.5);
		assertEquals(GreatCircle.PLANNED_RELAYS, plan.spots().size());
		// Le premier emplacement est sous les pieds du joueur.
		assertEquals(player, plan.spots().getFirst().pos());
		assertFalse(plan.ready());
	}

	@Test
	void tooCloseToTheCenterThereIsNoGreatCircleYet() {
		var plan = GreatCircle.plan(List.of(CENTER), CENTER.offset(3, 0, 0)).orElseThrow();
		assertEquals(0, plan.radius());
	}

	@Test
	void withTwoCopiesTheCenterIsTheOneThePlayerLeft() {
		BlockPos relay = CENTER.offset(0, 0, 24);
		// Le joueur vient de tracer le relais : il est à côté.
		var plan = GreatCircle.plan(List.of(relay, CENTER), relay.offset(1, 0, 1)).orElseThrow();
		assertEquals(CENTER, plan.center());
		assertEquals(1, plan.spots().stream().filter(GreatCircle.Spot::filled).count());
	}

	@Test
	void inATriangleTheFirstDrawnIsTheCenter() {
		// Le centre et deux relais à 60° : un triangle équilatéral, où chacun pourrait être le centre.
		BlockPos a = CENTER.offset(16, 0, 0);
		BlockPos b = CENTER.offset(8, 0, 14);
		var plan = GreatCircle.plan(List.of(a, b, CENTER), p -> p.equals(CENTER) ? 100 : 200, a).orElseThrow();
		assertEquals(CENTER, plan.center());
		assertEquals(2, plan.relays().size());
	}

	@Test
	void thePlanFillsUpAsRelaysAreDrawn() {
		List<BlockPos> circles = new ArrayList<>(around(3, 30, 0, 2 * Math.PI));
		circles.add(CENTER);
		var plan = GreatCircle.plan(circles, CENTER.offset(30, 0, 0)).orElseThrow();
		assertEquals(CENTER, plan.center());
		// Trois relais tous les 120° : un emplacement sur deux est tracé, et le triangle tient déjà.
		assertEquals(3, plan.spots().stream().filter(GreatCircle.Spot::filled).count());
		assertTrue(plan.ready());
		BlockPos next = plan.next(CENTER.offset(30, 0, 0)).orElseThrow().pos();
		assertFalse(circles.contains(next));
	}

	@Test
	void sixRelaysFillThePlan() {
		List<BlockPos> circles = new ArrayList<>(around(6, 30, 0, 2 * Math.PI));
		circles.add(CENTER);
		var plan = GreatCircle.plan(circles, CENTER.offset(0, 0, 50)).orElseThrow();
		assertTrue(plan.spots().stream().allMatch(GreatCircle.Spot::filled));
		assertTrue(plan.next(CENTER).isEmpty());
	}
}
