package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.TollChooser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le péage de la Vérité et ce que la Porte retient du joueur. */
class GateTest {
	@Test
	void aSevereFailureCostsTheWholeBody() {
		assertEquals(BodyPart.BODY, TollChooser.choose(Set.of(), 0, true, 0.5));
	}

	@Test
	void whenNothingIsLeftToTakeTheBodyGoes() {
		Set<BodyPart> all = EnumSet.of(BodyPart.ORGANS, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG, BodyPart.LEFT_ARM,
				BodyPart.RIGHT_ARM, BodyPart.SIGHT);
		assertEquals(BodyPart.BODY, TollChooser.choose(all, 0, false, 0.1));
	}

	@Test
	void theTruthNeverTakesTwiceTheSameThing() {
		Set<BodyPart> lost = EnumSet.of(BodyPart.LEFT_LEG);
		for (double roll = 0; roll < 1; roll += 0.01) {
			BodyPart part = TollChooser.choose(lost, 3, false, roll);
			assertNotEquals(BodyPart.LEFT_LEG, part);
			assertNotEquals(BodyPart.BODY, part);
		}
	}

	@Test
	void ambitionMakesTheTollHeavier() {
		Map<BodyPart, Integer> modest = histogram(0);
		Map<BodyPart, Integer> greedy = histogram(10);
		assertTrue(modest.get(BodyPart.ORGANS) > modest.get(BodyPart.SIGHT), modest.toString());
		assertTrue(greedy.get(BodyPart.SIGHT) > greedy.get(BodyPart.ORGANS), greedy.toString());
		assertTrue(greedy.get(BodyPart.RIGHT_ARM) > modest.get(BodyPart.RIGHT_ARM));
	}

	private static Map<BodyPart, Integer> histogram(int ambition) {
		Map<BodyPart, Integer> out = new EnumMap<>(BodyPart.class);
		for (BodyPart p : BodyPart.values()) {
			out.put(p, 0);
		}
		for (int i = 0; i < 1000; i++) {
			out.merge(TollChooser.choose(Set.of(), ambition, false, i / 1000.0), 1, Integer::sum);
		}
		return out;
	}

	@Test
	void aSoulLosesItsArmorAndDriftsBeforeTheGate() {
		GateState gate = GateState.NONE.pay(BodyPart.ORGANS).pay(BodyPart.BODY);
		assertEquals(2, gate.openings());
		assertTrue(gate.soulBound());
		assertTrue(gate.inArmor());
		GateState adrift = gate.payWithSeal();
		assertEquals(3, adrift.openings());
		assertTrue(adrift.adrift());
		assertFalse(adrift.inArmor());
		assertTrue(adrift.lost(BodyPart.ORGANS));
		assertTrue(adrift.withAdrift(false).inArmor());
	}

	@Test
	void thePhilosophersStoneGivesTheBodyBack() {
		GateState soul = GateState.NONE.pay(BodyPart.LEFT_ARM).pay(BodyPart.BODY);
		GateState healed = soul.restore(BodyPart.BODY);
		assertFalse(healed.soulBound());
		assertFalse(healed.adrift());
		assertTrue(healed.lost(BodyPart.LEFT_ARM), "la Pierre ne rend que le corps");
		assertEquals(soul.openings(), healed.openings());
	}

	@Test
	void theGateStateSurvivesSaving() {
		GateState gate = GateState.NONE.pay(BodyPart.RIGHT_ARM).pay(BodyPart.BODY).withAdrift(true)
				.withVisit(new GateState.Visit("minecraft:overworld", new BlockPos(3, 64, -7), 2, false, 120));
		var json = GateState.CODEC.encodeStart(JsonOps.INSTANCE, gate).getOrThrow();
		GateState back = GateState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		assertEquals(gate, back);
		assertEquals(Optional.of(120), back.visit().map(GateState.Visit::ticks));
	}
}
