package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.DrawingCode;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Raster;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawingCodeTest {
	@Test
	void aCodeGivesBackTheSameDrawing() {
		List<Primitive> prims = new ArrayList<>();
		prims.add(new Primitive.Circle(Drawing.CENTER_POINT, 14));
		prims.add(new Primitive.Polygon(List.of(new Vec2(16, 2), new Vec2(28, 23), new Vec2(4, 23))));
		prims.add(new Primitive.Arc(new Vec2(16, 16), 3, 20, 320));
		prims.add(new Primitive.Dot(new Vec2(2, 16)));
		prims.addAll(TestGlyphs.drawn("terre", new Vec2(16, 11), 3, 0));
		Drawing d = new Drawing(prims);
		assertEquals(d, DrawingCode.decode(DrawingCode.encode(d)));
	}

	@Test
	void garbageIsRefused() {
		assertThrows(IllegalArgumentException.class, () -> DrawingCode.decode("pas-un-code"));
		assertThrows(IllegalArgumentException.class, () -> DrawingCode.decode(""));
	}

	@Test
	void theRasterFollowsTheStrokes() {
		Drawing d = new Drawing(List.of(new Primitive.Line(new Vec2(0, 16), new Vec2(32, 16))));
		boolean[] mask = Raster.rasterize(d, 64, 2);
		assertTrue(mask[32 * 64 + 10]);
		assertFalse(mask[10 * 64 + 10]);
	}
}
