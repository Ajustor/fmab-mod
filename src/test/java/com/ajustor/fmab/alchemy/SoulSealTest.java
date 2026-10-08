package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.DrawingCode;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.SoulSeal;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Chaque joueur a son sceau de sang : toujours le même, différent de celui des autres. */
class SoulSealTest {
	@Test
	void aPlayerAlwaysHasTheSameSeal() {
		UUID id = UUID.fromString("376eb988-e6dd-3105-bb50-572ead49ce98");
		assertEquals(SoulSeal.of(id), SoulSeal.of(id));
		// Il survit à l'export et à l'import par code, comme une page de carnet.
		assertEquals(SoulSeal.of(id), DrawingCode.decode(DrawingCode.encode(SoulSeal.of(id))));
	}

	@Test
	void sealsDifferFromOnePlayerToAnother() {
		Random random = new Random(42);
		Set<Drawing> seen = new HashSet<>();
		for (int i = 0; i < 200; i++) {
			seen.add(SoulSeal.of(new UUID(random.nextLong(), random.nextLong())));
		}
		assertTrue(seen.size() > 150, "trop de sceaux identiques : " + seen.size() + " / 200");
	}

	@Test
	void aSealFitsOnTheNotebookPage() {
		Random random = new Random(7);
		for (int i = 0; i < 100; i++) {
			for (Primitive p : SoulSeal.of(new UUID(random.nextLong(), random.nextLong())).primitives()) {
				for (Vec2 v : p.sample(0.5)) {
					assertTrue(v.x() >= 0 && v.x() <= Drawing.GRID && v.y() >= 0 && v.y() <= Drawing.GRID, v.toString());
				}
			}
		}
	}
}
