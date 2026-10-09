package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.glyph.GlyphJson;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Les cercles qu'aucun traité ne reproduit. On les recompose soi-même à partir de leur description,
 * ou on les trouve dans de rares notes de recherche.
 */
public final class ForbiddenCircles {
	/** L'effet de la transmutation humaine. */
	public static final String HUMAN_TRANSMUTATION_EFFECT = "fmab:human_transmutation";

	/**
	 * La transmutation humaine des frères Elric : un carré et un triangle superposés dans l'anneau,
	 * Humain en haut, Recomposer en bas.
	 */
	public static final Drawing HUMAN_TRANSMUTATION = parse("""
			[{"type": "circle", "center": [16, 16], "radius": 14},
			 {"type": "polygon", "points": [[16, 2], [30, 16], [16, 30], [2, 16]]},
			 {"type": "polygon", "points": [[16, 30], [4, 9], [28, 9]]},
			 {"type": "circle", "center": [16.0, 13.0], "radius": 1.0},
			 {"type": "line", "from": [16.0, 14.0], "to": [16.0, 15.5]},
			 {"type": "line", "from": [14.0, 14.5], "to": [18.0, 14.5]},
			 {"type": "polyline", "points": [[14.5, 17.0], [16.0, 15.5], [17.5, 17.0]]},
			 {"type": "polygon", "points": [[14.5, 21.5], [18.0, 21.5], [18.0, 25.0], [14.5, 25.0]]},
			 {"type": "line", "from": [14.5, 21.5], "to": [18.0, 25.0]},
			 {"type": "line", "from": [18.0, 21.5], "to": [14.5, 25.0]}]
			""");

	private ForbiddenCircles() {
	}

	private static Drawing parse(String json) {
		List<Primitive> primitives = new ArrayList<>();
		JsonParser.parseString(json).getAsJsonArray()
				.forEach(e -> primitives.add(GlyphJson.primitive(e.getAsJsonObject())));
		return new Drawing(primitives);
	}
}
