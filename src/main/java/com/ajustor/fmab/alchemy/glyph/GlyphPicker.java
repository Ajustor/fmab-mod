package com.ajustor.fmab.alchemy.glyph;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Le choix du glyphe du tampon, dans le carnet : on filtre par couche et par nom plutôt que de faire
 * défiler un à un tous les glyphes compris. Les derniers posés passent en tête.
 */
public final class GlyphPicker {
	/** Les derniers glyphes posés dont on se souvient. */
	public static final int MAX_RECENT = 8;

	private GlyphPicker() {
	}

	/**
	 * @param layer  la couche voulue, ou null pour toutes
	 * @param query  un bout du nom affiché ou de l'identifiant, sans égard aux accents ni à la casse
	 * @param name   le nom affiché d'un glyphe (traduit)
	 * @param recent les identifiants des derniers glyphes posés, le plus récent d'abord
	 * @return les glyphes retenus : les récents d'abord, puis par couche et par nom
	 */
	public static List<Glyph> filter(List<Glyph> glyphs, GlyphLayer layer, String query, Function<Glyph, String> name,
			List<String> recent) {
		String needle = fold(query.strip());
		List<Glyph> out = new ArrayList<>();
		for (Glyph g : glyphs) {
			if (layer != null && g.layer() != layer) {
				continue;
			}
			if (!needle.isEmpty() && !fold(name.apply(g)).contains(needle) && !fold(path(g.id())).contains(needle)) {
				continue;
			}
			out.add(g);
		}
		out.sort(Comparator.<Glyph>comparingInt(g -> rank(recent, g.id()))
				.thenComparing(Glyph::layer)
				.thenComparing(g -> fold(name.apply(g))));
		return out;
	}

	/** La liste des récents, ce glyphe en tête (sans doublon), bornée à {@link #MAX_RECENT}. */
	public static List<String> remember(List<String> recent, String id) {
		List<String> out = new ArrayList<>(MAX_RECENT);
		out.add(id);
		for (String r : recent) {
			if (out.size() >= MAX_RECENT) {
				break;
			}
			if (!r.equals(id)) {
				out.add(r);
			}
		}
		return List.copyOf(out);
	}

	/** Place parmi les récents ; ceux qui n'y sont pas viennent après. */
	private static int rank(List<String> recent, String id) {
		int i = recent.indexOf(id);
		return i < 0 ? Integer.MAX_VALUE : i;
	}

	/** L'identifiant sans son espace de noms : « fab » ne doit pas trouver tous les glyphes de « fmab: ». */
	private static String path(String id) {
		return id.substring(id.indexOf(':') + 1);
	}

	/** Minuscules, sans accents : « Éther » se trouve en tapant « ether ». */
	static String fold(String s) {
		return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
	}
}
