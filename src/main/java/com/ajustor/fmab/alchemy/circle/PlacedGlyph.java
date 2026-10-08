package com.ajustor.fmab.alchemy.circle;

import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.Glyph;

/** Glyphe reconnu dans un cercle, avec sa position par rapport au centre. */
public record PlacedGlyph(Glyph glyph, Vec2 position, double rotationDeg) {
}
