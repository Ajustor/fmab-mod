package com.ajustor.fmab.tattoo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les vrais cercles posés sur le corps (gants, tatouages) : chacun tient sur la face de son membre,
 * se lit sans miroir vu de l'extérieur, et se range du bon côté de la couche des vêtements.
 */
class BodySpotTest {
	private static final float PX = 1f / 16;
	private static final float EPS = 1e-5f;
	/** La couche des vêtements d'une peau (manches, veste) est gonflée d'un quart de pixel. */
	private static final float CLOTHES = 0.25f * PX;

	@Test
	void everyCircleSitsOnItsFace() {
		for (BodySpot spot : BodySpot.values()) {
			for (boolean left : new boolean[]{false, true}) {
				for (boolean thin : new boolean[]{false, true}) {
					BodySpot.Placement at = spot.place(left, thin);
					Box box = box(spot, left, thin);
					String what = spot + (left ? " gauche" : " droit") + (thin ? " fin" : "");
					// Le carré reste dans la face, sur ses deux axes.
					float h = at.half();
					assertTrue(at.y() - h >= box.y0 - EPS && at.y() + h <= box.y1 + EPS, what + " : hauteur");
					if (at.ux() != 0) {
						assertTrue(at.x() - h >= box.x0 - EPS && at.x() + h <= box.x1 + EPS, what + " : largeur");
					} else {
						assertTrue(at.z() - h >= box.z0 - EPS && at.z() + h <= box.z1 + EPS, what + " : profondeur");
					}
					// Il se tient juste au-dessus de la face que regarde sa normale.
					float surface = at.nx() > 0 ? box.x1 : at.nx() < 0 ? box.x0 : at.nz() > 0 ? box.z1 : box.z0;
					float coord = at.nx() != 0 ? at.x() : at.z();
					float lift = (coord - surface) * (at.nx() + at.nz());
					assertTrue(lift > 0, what + " : sous la peau");
					if (spot.tattoo()) {
						assertTrue(lift < CLOTHES, what + " : un tatouage doit rester sous les vêtements");
					} else {
						assertTrue(lift > CLOTHES, what + " : un gant brode par-dessus la manche");
					}
				}
			}
		}
	}

	@Test
	void everyCircleReadsWithoutMirrorFromOutside() {
		// Pour qui regarde la face de l'extérieur (le long de −normale, le haut vers −Y), sa droite est
		// (−nz, 0, nx) : c'est là que doit aller la page.
		for (BodySpot spot : BodySpot.values()) {
			for (boolean left : new boolean[]{false, true}) {
				BodySpot.Placement at = spot.place(left, false);
				assertEquals(-at.nz(), at.ux(), EPS, spot + " : sens de la page");
				assertEquals(at.nx(), at.uz(), EPS, spot + " : sens de la page");
			}
		}
	}

	@Test
	void armCirclesFaceOutwardAndPalmsForward() {
		assertTrue(BodySpot.HAND_BACK.place(false, false).nx() < 0, "dos de la main droite : vers −X");
		assertTrue(BodySpot.HAND_BACK.place(true, false).nx() > 0, "dos de la main gauche : vers +X");
		assertTrue(BodySpot.ARM.place(true, true).nx() > 0, "bras gauche : vers +X");
		assertTrue(BodySpot.PALM.place(false, false).nz() < 0, "paume : vers l'avant (−Z)");
		assertTrue(BodySpot.CHEST.place(false, false).nz() < 0, "poitrine : vers l'avant (−Z)");
		assertTrue(BodySpot.BACK.place(false, false).nz() > 0, "dos : vers l'arrière (+Z)");
	}

	@Test
	void theSleeveStaysAboveTheGlove() {
		// Un gant couvre les quatre dernières lignes du bras (y de 6 à 10) : le tour du bras tatoué s'arrête avant.
		assertTrue(Sleeve.BOTTOM <= 6 * PX + EPS);
		BodySpot.Placement outer = BodySpot.ARM.place(false, false);
		assertTrue(outer.y() + outer.half() <= 6 * PX + EPS);
	}

	@Test
	void everyTattooSlotHasItsSpot() {
		assertEquals(BodySpot.PALM, BodySpot.of(TattooSlot.PALM_LEFT));
		assertEquals(BodySpot.PALM, BodySpot.of(TattooSlot.PALM_RIGHT));
		assertEquals(BodySpot.ARM, BodySpot.of(TattooSlot.ARM_LEFT));
		assertEquals(BodySpot.ARM, BodySpot.of(TattooSlot.ARM_RIGHT));
		assertEquals(BodySpot.CHEST, BodySpot.of(TattooSlot.CHEST));
		assertEquals(BodySpot.BACK, BodySpot.of(TattooSlot.BACK));
	}

	/** Les cubes du modèle de joueur, dans le repère de leur membre. */
	private record Box(float x0, float x1, float y0, float y1, float z0, float z1) {
	}

	private static Box box(BodySpot spot, boolean left, boolean thin) {
		if (spot == BodySpot.CHEST || spot == BodySpot.BACK) {
			return new Box(-4 * PX, 4 * PX, 0, 12 * PX, -2 * PX, 2 * PX);
		}
		float width = thin ? 3 : 4;
		float x0 = left ? -1 * PX : (-width + 1) * PX;
		return new Box(x0, x0 + width * PX, -2 * PX, 10 * PX, -2 * PX, 2 * PX);
	}
}
