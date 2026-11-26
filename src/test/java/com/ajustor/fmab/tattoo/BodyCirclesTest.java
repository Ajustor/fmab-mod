package com.ajustor.fmab.tattoo;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.data.Engravings;
import com.ajustor.fmab.gate.BodyPart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ce qui porte le cercle d'un emplacement : la peau se tatoue, l'automail se grave, un bras perdu ne
 * porte rien. La gravure est rangée par endroit du bras, pas par côté : elle suit la pièce d'un bras
 * à l'autre.
 */
class BodyCirclesTest {
	private static final Drawing CIRCLE = new Drawing(List.of());

	@Test
	void skinIsTattooedSteelIsEngravedNothingElse() {
		assertEquals(BodyCircles.Surface.SKIN, BodyCircles.surface(false, false, false));
		// Un automail sur un bras intact n'arrive pas, mais c'est la peau qui compte.
		assertEquals(BodyCircles.Surface.SKIN, BodyCircles.surface(false, true, false));
		assertEquals(BodyCircles.Surface.STEEL, BodyCircles.surface(true, true, false));
		assertEquals(BodyCircles.Surface.BROKEN, BodyCircles.surface(true, true, true));
		assertEquals(BodyCircles.Surface.NONE, BodyCircles.surface(true, false, false));
	}

	@Test
	void armSlotsKnowTheirArmAndSpot() {
		assertEquals(Optional.of(BodyPart.LEFT_ARM), TattooSlot.PALM_LEFT.arm());
		assertEquals(Optional.of(BodyPart.RIGHT_ARM), TattooSlot.FOREARM_RIGHT.arm());
		assertEquals(Optional.empty(), TattooSlot.BACK.arm());
		assertEquals(Optional.empty(), TattooSlot.CHEST.spot());
		for (TattooSlot slot : TattooSlot.values()) {
			assertEquals(slot.arm().isPresent(), slot.spot().isPresent(), slot.id());
			// Le côté de l'emplacement et celui de son bras s'accordent.
			slot.arm().ifPresent(arm -> assertEquals(slot.isLeft(), arm == BodyPart.LEFT_ARM, slot.id()));
		}
	}

	@Test
	void engravingFollowsThePieceFromOneArmToTheOther() {
		Engravings engraved = Engravings.NONE.with(TattooSlot.PALM_LEFT, CIRCLE);
		assertSame(CIRCLE, engraved.get(TattooSlot.PALM_RIGHT).orElseThrow());
		assertTrue(engraved.get(TattooSlot.FOREARM_LEFT).isEmpty());
		assertTrue(engraved.without(TattooSlot.PALM_RIGHT).isEmpty());
		assertFalse(engraved.isEmpty());
	}

	@Test
	void theTorsoIsNeverEngraved() {
		assertThrows(IllegalArgumentException.class, () -> Engravings.NONE.with(TattooSlot.BACK, CIRCLE));
		assertTrue(Engravings.NONE.get(TattooSlot.CHEST).isEmpty());
	}
}
