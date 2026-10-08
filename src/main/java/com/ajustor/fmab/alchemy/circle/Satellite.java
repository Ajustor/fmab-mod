package com.ajustor.fmab.alchemy.circle;

import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.GlyphLayer;

import java.util.List;

/**
 * Mini-cercle posé sur un sommet du polygone d'un étage.
 *
 * @param center position par rapport au centre du carnet
 */
public record Satellite(Vec2 center, double radius, List<PlacedGlyph> glyphs) {
	public Satellite {
		glyphs = List.copyOf(glyphs);
	}

	public List<PlacedGlyph> layer(GlyphLayer layer) {
		return glyphs.stream().filter(g -> g.glyph().layer() == layer).toList();
	}
}
