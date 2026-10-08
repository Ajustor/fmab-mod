package com.ajustor.fmab.alchemy.circle;

import com.ajustor.fmab.alchemy.glyph.GlyphLayer;

import java.util.List;
import java.util.OptionalDouble;

/**
 * Un étage : l'anneau qui le borde, le polygone inscrit et ce qu'on a écrit dedans.
 *
 * @param index        0 pour l'étage central, puis vers l'extérieur
 * @param sides        côtés du plus grand polygone inscrit, 0 sans polygone ; un hexagramme compte pour 6
 * @param hexagram     deux triangles inscrits, qui permettent de fusionner deux éléments
 * @param polygons     nombre de polygones inscrits dans l'anneau
 * @param capacity     somme des côtés de ces polygones : la charge que l'étage supporte
 * @param intensity    nombre de points d'intensité posés sur l'anneau
 * @param direction    angle (radians, y vers le bas) de la flèche de direction, s'il y en a une
 * @param link         liaison qui relie cet étage au précédent ; {@link LinkKind#NONE} pour le centre
 * @param satellites   mini-cercles posés sur les sommets du polygone
 */
public record Stage(
		int index,
		double ringRadius,
		int sides,
		boolean hexagram,
		int polygons,
		int capacity,
		List<PlacedGlyph> glyphs,
		int intensity,
		OptionalDouble direction,
		LinkKind link,
		List<Satellite> satellites
) {
	public Stage {
		glyphs = List.copyOf(glyphs);
		satellites = List.copyOf(satellites);
	}

	public List<PlacedGlyph> layer(GlyphLayer layer) {
		return glyphs.stream().filter(g -> g.glyph().layer() == layer).toList();
	}
}
