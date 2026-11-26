package com.ajustor.fmab.tattoo;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.data.FmabCodecs;
import com.ajustor.fmab.data.Tattoos;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les dessins sur le corps : ce qu'ils gardent, ce que le serveur refuse, comment ils se peignent, et
 * les tatouages d'avant les dessins qui se relisent en tampons.
 */
class BodyArtTest {
	private static final Drawing CIRCLE = new Drawing(List.of(new Primitive.Circle(Drawing.CENTER_POINT, 12)));
	private static final BodyArt.Stroke RED_LINE =
			new BodyArt.Stroke(new Primitive.Line(new Vec2(2, 2), new Vec2(30, 30)), 0xB0201A);

	@Test
	void drawingAndStampSurviveTheCodec() {
		BodyArt art = new BodyArt(List.of(RED_LINE),
				Optional.of(new BodyArt.Stamp(CIRCLE, new Vec2(10, 12), 6, 0x2C5CC8)));
		JsonElement json = BodyArt.CODEC.encodeStart(JsonOps.INSTANCE, art).getOrThrow();
		assertEquals(art, BodyArt.CODEC_OR_CIRCLE.parse(JsonOps.INSTANCE, json).getOrThrow());
	}

	@Test
	void anOldTattooReadsAsAFullStampInTattooInk() {
		JsonElement old = FmabCodecs.DRAWING.encodeStart(JsonOps.INSTANCE, CIRCLE).getOrThrow();
		BodyArt art = BodyArt.CODEC_OR_CIRCLE.parse(JsonOps.INSTANCE, old).getOrThrow();
		assertEquals(Optional.of(CIRCLE), art.circle());
		BodyArt.Stamp stamp = art.stamp().orElseThrow();
		assertEquals(Drawing.CENTER_POINT, stamp.center());
		assertEquals(BodyArt.FULL_STAMP_HALF, stamp.half());
		assertEquals(BodyArt.SKIN_INK, stamp.color());
		assertTrue(art.strokes().isEmpty());
	}

	@Test
	void theRitualStampsOverTheDrawingsAlreadyThere() {
		Tattoos drawn = Tattoos.NONE.withArt(TattooSlot.CHEST, new BodyArt(List.of(RED_LINE), Optional.empty()));
		assertTrue(drawn.get(TattooSlot.CHEST).isEmpty(), "un dessin seul n'est pas un cercle");
		Tattoos stamped = drawn.with(TattooSlot.CHEST, CIRCLE);
		assertEquals(Optional.of(CIRCLE), stamped.get(TattooSlot.CHEST));
		assertEquals(List.of(RED_LINE), stamped.art(TattooSlot.CHEST).orElseThrow().strokes());
	}

	@Test
	void anEmptyDrawingLeavesTheSpotBare() {
		assertTrue(Tattoos.NONE.withArt(TattooSlot.BACK, BodyArt.EMPTY).slots().isEmpty());
		assertTrue(BodyArt.EMPTY.withStamp(new BodyArt.Stamp(Drawing.EMPTY, Drawing.CENTER_POINT, 8, 0)).isEmpty());
	}

	@Test
	void theStampKeepsItsCircleAndOnlyMovesWhereItIsDrawn() {
		BodyArt.Stamp stamp = new BodyArt.Stamp(CIRCLE, new Vec2(8, 24), 8, 0);
		Primitive.Circle placed = (Primitive.Circle) stamp.placed().getFirst();
		assertEquals(8, placed.center().x(), 1e-9);
		assertEquals(24, placed.center().y(), 1e-9);
		// Demi-taille 8 sur 16 : le rayon de 12 devient 6.
		assertEquals(6, placed.radius(), 1e-9);
		assertEquals(CIRCLE, stamp.circle());
	}

	@Test
	void theServerRefusesWhatLeavesTheGridOrWeighsTooMuch() {
		assertTrue(new BodyArt(List.of(RED_LINE), Optional.empty()).valid());
		BodyArt outside = new BodyArt(List.of(new BodyArt.Stroke(
				new Primitive.Line(new Vec2(-1, 2), new Vec2(3, 4)), 0)), Optional.empty());
		assertFalse(outside.valid());
		BodyArt nan = new BodyArt(List.of(new BodyArt.Stroke(new Primitive.Dot(new Vec2(Double.NaN, 2)), 0)),
				Optional.empty());
		assertFalse(nan.valid());
		BodyArt tinyStamp = BodyArt.EMPTY.withStamp(new BodyArt.Stamp(CIRCLE, Drawing.CENTER_POINT, 1, 0));
		assertFalse(tinyStamp.valid(), "un tampon trop petit ne se lit plus");
		List<Vec2> points = new ArrayList<>();
		for (int i = 0; i < 40; i++) {
			points.add(new Vec2(i % 32, i % 7));
		}
		List<BodyArt.Stroke> heavy = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			heavy.add(new BodyArt.Stroke(new Primitive.Polyline(points), 0));
		}
		assertFalse(new BodyArt(heavy, Optional.empty()).valid(), "plus de points qu'un paquet n'en porte");
	}

	@Test
	void strokesArePaintedOverTheStampInTheirOwnColors() {
		int size = 64;
		BodyArt art = new BodyArt(List.of(RED_LINE),
				Optional.of(new BodyArt.Stamp(new Drawing(List.of(new Primitive.Line(new Vec2(2, 30), new Vec2(30, 2)))),
						Drawing.CENTER_POINT, BodyArt.FULL_STAMP_HALF, 0x2C5CC8)));
		int[] pixels = art.paint(size);
		// Les deux diagonales se croisent au centre : le trait, tracé après le tampon, passe dessus.
		assertEquals(0xFFB0201A, pixels[(size / 2) * size + size / 2]);
		// Ailleurs sur la diagonale du tampon, sa couleur ; le fond reste transparent.
		int x = size * 3 / 4, y = size / 4;
		assertEquals(0xFF2C5CC8, pixels[y * size + x]);
		assertEquals(0, pixels[0]);
	}
}
