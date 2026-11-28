package com.ajustor.fmab.tattoo;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.data.Engravings;
import com.ajustor.fmab.data.FmabCodecs;
import com.ajustor.fmab.data.Tattoos;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le tour du bras : sa page dépliée s'enroule autour du membre sans miroir ni trou, les anciens
 * avant-bras s'y reportent, et le rituel y pose plusieurs cercles côte à côte.
 */
class SleeveTest {
	private static final float EPS = 1e-5f;
	private static final Drawing CIRCLE = new Drawing(List.of(new Primitive.Circle(Drawing.CENTER_POINT, 12)));
	private static final Drawing OTHER = new Drawing(List.of(new Primitive.Circle(Drawing.CENTER_POINT, 8)));
	private static final Drawing THIRD = new Drawing(List.of(new Primitive.Circle(Drawing.CENTER_POINT, 4)));

	@Test
	void thePageWrapsTheWholeArmWithoutGaps() {
		for (boolean left : new boolean[]{false, true}) {
			for (boolean thin : new boolean[]{false, true}) {
				List<Sleeve.Piece> pieces = Sleeve.pieces(left, thin, 0);
				assertEquals(5, pieces.size());
				assertEquals(0, pieces.getFirst().u0(), EPS);
				assertEquals(1, pieces.getLast().u1(), EPS);
				for (int i = 0; i + 1 < pieces.size(); i++) {
					Sleeve.Piece a = pieces.get(i), b = pieces.get(i + 1);
					assertEquals(a.u1(), b.u0(), EPS, "la page continue d'un morceau à l'autre");
					assertEquals(a.x1(), b.x0(), EPS, "les morceaux se touchent à l'arête");
					assertEquals(a.z1(), b.z0(), EPS, "les morceaux se touchent à l'arête");
				}
				// La couture se referme au milieu de la face intérieure.
				assertEquals(pieces.getFirst().x0(), pieces.getLast().x1(), EPS);
				assertEquals(pieces.getFirst().z0(), pieces.getLast().z1(), EPS);
			}
		}
	}

	@Test
	void theOuterFaceIsInTheMiddleAndFacesOutward() {
		for (boolean left : new boolean[]{false, true}) {
			Sleeve.Piece outer = Sleeve.pieces(left, false, 0).get(2);
			assertEquals(Sleeve.Face.OUTER, outer.face());
			assertEquals(0.375, outer.u0(), EPS);
			assertEquals(0.625, outer.u1(), EPS);
			assertTrue(left ? outer.nx() > 0 : outer.nx() < 0, "la face extérieure regarde hors du corps");
			// Elle se lit dans le même sens que le carré de la face extérieure.
			BodySpot.Placement square = BodySpot.ARM.place(left, false);
			assertEquals(Math.signum(square.uz()), Math.signum(outer.z1() - outer.z0()));
		}
	}

	@Test
	void everyFaceReadsWithoutMirrorFromOutside() {
		for (boolean left : new boolean[]{false, true}) {
			for (Sleeve.Piece p : Sleeve.pieces(left, false, 0)) {
				// En avançant sur la page, vu de l'extérieur, on va vers la droite de qui regarde : la normale
				// tournée d'un quart de tour (le sens des carrés de BodySpot : u = (−nz, nx)).
				float ux = p.x1() - p.x0(), uz = p.z1() - p.z0();
				assertTrue(ux * -p.nz() + uz * p.nx() > 0, p.face() + (left ? " gauche" : " droit"));
			}
			// L'avant se lit vers +X, comme la paume ; l'arrière vers −X, comme le dos.
			Sleeve.Piece front = Sleeve.pieces(left, false, 0).stream()
					.filter(p -> p.face() == Sleeve.Face.FRONT).findFirst().orElseThrow();
			assertTrue(front.x1() > front.x0());
		}
	}

	@Test
	void theLiftPushesEveryPieceOutward() {
		List<Sleeve.Piece> flat = Sleeve.pieces(false, false, 0);
		List<Sleeve.Piece> lifted = Sleeve.pieces(false, false, BodySpot.SKIN_LIFT);
		for (int i = 0; i < flat.size(); i++) {
			Sleeve.Piece a = flat.get(i), b = lifted.get(i);
			assertEquals(BodySpot.SKIN_LIFT, (b.x0() - a.x0()) * a.nx() + (b.z0() - a.z0()) * a.nz(), EPS);
		}
	}

	@Test
	void theFaceUnderAColumnFollowsThePage() {
		assertEquals(Sleeve.Face.INNER, Sleeve.faceAt(false, 2));
		assertEquals(Sleeve.Face.BACK, Sleeve.faceAt(false, 16));
		assertEquals(Sleeve.Face.OUTER, Sleeve.faceAt(false, 32));
		assertEquals(Sleeve.Face.FRONT, Sleeve.faceAt(false, 48));
		assertEquals(Sleeve.Face.INNER, Sleeve.faceAt(false, 64));
		assertEquals(Sleeve.Face.FRONT, Sleeve.faceAt(true, 16));
		assertEquals(Sleeve.Face.BACK, Sleeve.faceAt(true, 48));
	}

	@Test
	void anOldForearmLandsOnTheOuterFaceBelowTheShoulder() {
		BodyArt forearm = BodyArt.ofCircle(CIRCLE, BodyArt.SKIN_INK);
		BodyArt sleeve = Sleeve.fromForearm(forearm);
		BodyArt.Stamp stamp = sleeve.stamps().getFirst();
		assertEquals(32, stamp.center().x(), 1e-9);
		assertEquals(22, stamp.center().y(), 1e-9);
		assertEquals(6.4, stamp.half(), 1e-9);
		assertEquals(CIRCLE, stamp.circle());
		assertTrue(sleeve.valid(Canvas.SLEEVE));
		BodyArt tiny = Sleeve.fromForearm(BodyArt.EMPTY.withStamp(
				new BodyArt.Stamp(CIRCLE, Drawing.CENTER_POINT, BodyArt.MIN_STAMP_HALF, 0)));
		assertEquals(BodyArt.MIN_STAMP_HALF, tiny.stamps().getFirst().half(), 1e-9, "un tampon trop petit grandit");
	}

	@Test
	void savedForearmsAreReadAsArms() {
		JsonObject old = new JsonObject();
		old.add("forearm_left", FmabCodecs.DRAWING.encodeStart(JsonOps.INSTANCE, CIRCLE).getOrThrow());
		Tattoos read = Tattoos.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow();
		assertEquals(Optional.of(CIRCLE), read.get(TattooSlot.ARM_LEFT));
		assertFalse(read.slots().containsKey("forearm_left"));

		Engravings engraved = new Engravings(Map.of("forearm", BodyArt.ofCircle(CIRCLE, BodyArt.STEEL_INK)));
		assertEquals(Optional.of(CIRCLE), engraved.get(TattooSlot.ARM_RIGHT).flatMap(BodyArt::circle));
		assertFalse(engraved.spots().containsKey("forearm"));
	}

	@Test
	void legacyIdsNameTheArms() {
		assertEquals(Optional.of(TattooSlot.ARM_LEFT), TattooSlot.legacy("forearm_left"));
		assertEquals(Optional.of(TattooSlot.ARM_RIGHT), TattooSlot.legacy("forearm_right"));
		assertEquals(Optional.empty(), TattooSlot.legacy("arm_left"));
		assertEquals(Optional.empty(), TattooSlot.byId("forearm_left"));
	}

	@Test
	void theRitualStampsSeveralCirclesAroundTheArmOnDifferentFaces() {
		Tattoos t = Tattoos.NONE.with(TattooSlot.ARM_RIGHT, CIRCLE).with(TattooSlot.ARM_RIGHT, OTHER)
				.with(TattooSlot.ARM_RIGHT, THIRD);
		BodyArt arm = t.art(TattooSlot.ARM_RIGHT).orElseThrow();
		assertEquals(List.of(CIRCLE, OTHER, THIRD), arm.circles());
		assertEquals(new Vec2(32, 16), arm.stamps().get(0).center(), "d'abord la face extérieure");
		assertNotEquals(arm.stamps().get(0).center(), arm.stamps().get(1).center());
		assertNotEquals(arm.stamps().get(1).center(), arm.stamps().get(2).center());
		assertTrue(arm.valid(Canvas.SLEEVE));
		// Ailleurs, un cercle remplace l'autre.
		Tattoos chest = Tattoos.NONE.with(TattooSlot.CHEST, CIRCLE).with(TattooSlot.CHEST, OTHER);
		assertEquals(List.of(OTHER), chest.art(TattooSlot.CHEST).orElseThrow().circles());
	}

	@Test
	void onlyTheArmsHoldSeveralCircles() {
		for (TattooSlot slot : TattooSlot.values()) {
			assertEquals(slot.sleeve() ? BodyArt.MAX_STAMPS : 1, slot.maxStamps(), slot.id());
			assertEquals(slot.sleeve() ? Canvas.SLEEVE : Canvas.SQUARE, slot.canvas(), slot.id());
		}
	}
}
