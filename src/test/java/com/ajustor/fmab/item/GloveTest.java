package com.ajustor.fmab.item;

import com.ajustor.fmab.alchemy.glyph.Rank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Un gant dit combien d'étages il porte, et combien le rang de celui qui l'enfile en tient. */
class GloveTest {
	@Test
	void stateGlovesHoldFiveStagesOnlyForAGateInitiate() {
		assertEquals(5, GloveItem.Kind.STATE.stages());
		assertEquals(Rank.STATE.maxStages(), GloveItem.Kind.STATE.stableStages(Rank.STATE), "trois pour un Alchimiste d'État");
		assertEquals(5, GloveItem.Kind.STATE.stableStages(Rank.GATE), "les cinq pour un Initié de la Porte");
	}

	@Test
	void aGloveNeverClaimsMoreThanItCarries() {
		for (GloveItem.Kind kind : GloveItem.Kind.values()) {
			for (Rank rank : Rank.values()) {
				int stable = kind.stableStages(rank);
				assertEquals(Math.min(kind.stages(), rank.maxStages()), stable, kind + " / " + rank);
			}
		}
	}
}
