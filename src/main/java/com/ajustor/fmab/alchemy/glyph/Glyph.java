package com.ajustor.fmab.alchemy.glyph;

import com.ajustor.fmab.alchemy.drawing.Primitive;

import java.util.List;

/**
 * Définition d'un glyphe, lue depuis {@code data/<ns>/glyph/<nom>.json}. Le même fichier sert au mod
 * et à l'éditeur web.
 *
 * @param role       ce que le glyphe désigne dans sa couche : l'élément ({@code earth}), l'action
 *                   ({@code fix}) ou le modificateur ({@code intensity})
 * @param primitives tracé de référence, dans le carré −1..1, y vers le bas
 * @param since      version du mod qui l'a introduit
 */
public record Glyph(
		String id,
		GlyphLayer layer,
		String role,
		List<Primitive> primitives,
		int complexity,
		int concentration,
		Rank rank,
		Tolerance tolerance,
		String nameKey,
		String descriptionKey,
		String since
) {
	public Glyph {
		primitives = List.copyOf(primitives);
	}

	public boolean is(GlyphLayer layer, String role) {
		return this.layer == layer && this.role.equals(role);
	}
}
