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
	 * La transmutation humaine : la formule complète d'un corps. Au centre d'un hexagramme inscrit
	 * dans un dodécagone, Humain et Recomposer ; sur les six pointes, des satellites infusent ce dont
	 * un corps est fait (eau, carbone, chaux, soufre, fer et cristal pour le silicium). Il en manque
	 * un, et le cercle rebondit. Un double anneau l'entoure. Six satellites, c'est la mesure d'un
	 * Alchimiste d'État : en dessous, le cercle vacille.
	 */
	public static final Drawing HUMAN_TRANSMUTATION = parse("""
			[{"type": "circle", "center": [16, 16], "radius": 15},
			 {"type": "circle", "center": [16, 16], "radius": 14.3},
			 {"type": "circle", "center": [16, 16], "radius": 10},
			 {"type": "polygon", "points": [[16.0, 6.0], [24.66, 21.0], [7.34, 21.0]]},
			 {"type": "polygon", "points": [[16.0, 26.0], [7.34, 11.0], [24.66, 11.0]]},
			 {"type": "polygon", "points": [[16.0, 6.0], [21.0, 7.34], [24.66, 11.0], [26.0, 16.0], [24.66, 21.0], [21.0, 24.66], [16.0, 26.0], [11.0, 24.66], [7.34, 21.0], [6.0, 16.0], [7.34, 11.0], [11.0, 7.34]]},
			 {"type": "circle", "center": [16.0, 6.0], "radius": 3.5},
			 {"type": "polygon", "points": [[14.24, 4.68], [17.76, 4.68], [16.0, 7.76]]},
			 {"type": "circle", "center": [24.66, 11.0], "radius": 3.5},
			 {"type": "polygon", "points": [[24.66, 8.91], [25.87, 11.0], [24.66, 13.09], [23.45, 11.0]]},
			 {"type": "line", "from": [22.57, 11.0], "to": [26.75, 11.0]},
			 {"type": "circle", "center": [24.66, 21.0], "radius": 3.5},
			 {"type": "circle", "center": [24.22, 21.44], "radius": 1.1},
			 {"type": "line", "from": [25.0, 20.66], "to": [26.31, 19.35]},
			 {"type": "polyline", "points": [[25.43, 19.35], [26.31, 19.35], [26.31, 20.23]]},
			 {"type": "circle", "center": [16.0, 26.0], "radius": 3.5},
			 {"type": "line", "from": [15.12, 26.0], "to": [16.88, 26.0]},
			 {"type": "circle", "center": [14.35, 26.0], "radius": 0.77},
			 {"type": "circle", "center": [17.65, 26.0], "radius": 0.77},
			 {"type": "circle", "center": [7.34, 21.0], "radius": 3.5},
			 {"type": "polygon", "points": [[6.02, 21.11], [8.66, 21.11], [7.34, 18.91]]},
			 {"type": "circle", "center": [7.34, 22.1], "radius": 0.88},
			 {"type": "circle", "center": [7.34, 11.0], "radius": 3.5},
			 {"type": "polygon", "points": [[7.34, 8.91], [8.44, 10.01], [8.44, 11.99], [7.34, 13.09], [6.24, 11.99], [6.24, 10.01]]},
			 {"type": "line", "from": [6.24, 10.01], "to": [8.44, 10.01]},
			 {"type": "line", "from": [6.24, 11.99], "to": [8.44, 11.99]},
			 {"type": "circle", "center": [16.0, 11.96], "radius": 0.57},
			 {"type": "line", "from": [16.0, 12.52], "to": [16.0, 13.57]},
			 {"type": "line", "from": [14.86, 12.9], "to": [17.14, 12.9]},
			 {"type": "polyline", "points": [[15.05, 14.71], [16.0, 13.57], [16.95, 14.71]]},
			 {"type": "polygon", "points": [[14.88, 18.18], [17.12, 18.18], [17.12, 20.42], [14.88, 20.42]]},
			 {"type": "line", "from": [14.88, 18.18], "to": [17.12, 20.42]},
			 {"type": "line", "from": [17.12, 18.18], "to": [14.88, 20.42]}]
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
