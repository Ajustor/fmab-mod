package com.ajustor.fmab.alchemy.circle;

import com.ajustor.fmab.alchemy.glyph.Glyph;
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
 * @param modifiers    les autres modificateurs posés sur l'anneau : forme, durée, cibles
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
		List<Satellite> satellites,
		List<Glyph> modifiers
) {
	public Stage {
		glyphs = List.copyOf(glyphs);
		satellites = List.copyOf(satellites);
		modifiers = List.copyOf(modifiers);
	}

	/** La forme de l'effet : {@code line} (par défaut), {@code circle} ou {@code cone}. */
	public String shape() {
		for (Glyph g : modifiers) {
			if (g.role().equals("shape_circle")) {
				return "circle";
			}
			if (g.role().equals("shape_cone")) {
				return "cone";
			}
		}
		return "line";
	}

	/** Combien de glyphes de durée sur l'anneau. */
	public int duration() {
		return (int) modifiers.stream().filter(g -> g.role().equals("duration")).count();
	}

	/**
	 * Ce que visent les glyphes de cible posés sur l'anneau : {@code item} (Objet), {@code block}
	 * (Bloc). Vide sans glyphe de cible : l'effet agit sur tout ce qu'il sait toucher.
	 */
	public java.util.Set<String> aims() {
		java.util.Set<String> out = new java.util.TreeSet<>();
		for (Glyph g : modifiers) {
			if (g.role().startsWith("aim_")) {
				out.add(g.role().substring("aim_".length()));
			}
		}
		return java.util.Set.copyOf(out);
	}

	/** Combien de glyphes de cibles sur l'anneau. */
	public int targets() {
		return (int) modifiers.stream().filter(g -> g.role().equals("targets")).count();
	}

	public List<PlacedGlyph> layer(GlyphLayer layer) {
		return glyphs.stream().filter(g -> g.glyph().layer() == layer).toList();
	}
}
