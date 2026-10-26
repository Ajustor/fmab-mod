package com.ajustor.fmab.stone;

import com.ajustor.fmab.TestBootstrap;
import com.ajustor.fmab.data.TransmutationPose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'absorption tenue : la proie reste paralysée jusqu'à la fin de la prise, et le bras tendu garde son
 * début d'une pulsation à l'autre au lieu de reprendre son élan.
 */
class ParalysisTest {
	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void thePreyIsHeldUntilTheGripEnds() {
		assertTrue(Paralysis.holds(110, 100));
		assertTrue(Paralysis.holds(110, 109));
		assertFalse(Paralysis.holds(110, 110));
	}

	@Test
	void aHeldReachKeepsItsStart() {
		TransmutationPose first = TransmutationPose.kept(null, TransmutationPose.Kind.REACH, 100, 10);
		TransmutationPose next = TransmutationPose.kept(first, TransmutationPose.Kind.REACH, 104, 10);
		assertEquals(100, next.start(), "la pulsation suivante ne relance pas le geste");
		assertEquals(114, next.until());
	}

	@Test
	void anotherOrEndedGestureStartsAnew() {
		TransmutationPose reach = TransmutationPose.kept(null, TransmutationPose.Kind.REACH, 100, 10);
		assertEquals(120, TransmutationPose.kept(reach, TransmutationPose.Kind.REACH, 120, 10).start(),
				"un geste fini repart de zéro");
		assertEquals(104, TransmutationPose.kept(reach, TransmutationPose.Kind.HOLD, 104, 10).start(),
				"un autre geste prend son propre élan");
	}
}
