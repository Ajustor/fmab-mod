package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.glyph.GlyphJson;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Plan du Traité d'alchimie, lu depuis {@code assets/fmab/treatise/treatise.json} : des chapitres
 * faits de pages de texte (clés de traduction), du catalogue des glyphes et de cercles d'exemple.
 * Certains chapitres ne se dévoilent qu'au fil des progrès du lecteur.
 */
public record Treatise(List<Chapter> chapters) {
	public record Chapter(String titleKey, List<Page> pages, Unlock unlock) {
	}

	/**
	 * Ce qu'il faut pour lire un chapitre : un rang, et des points de maîtrise (toutes écoles
	 * confondues). Les deux doivent être atteints ; {@code "unlock": {"rank": "alchemist"}} dans le
	 * plan.
	 */
	public record Unlock(Rank rank, int mastery) {
		public static final Unlock ALWAYS = new Unlock(Rank.APPRENTICE, 0);

		public boolean met(Rank reader, int totalMastery) {
			return reader.atLeast(rank) && totalMastery >= mastery;
		}

		static Unlock parse(JsonObject json) {
			return new Unlock(json.has("rank") ? Rank.fromSerializedName(json.get("rank").getAsString()) : Rank.APPRENTICE,
					json.has("mastery") ? json.get("mastery").getAsInt() : 0);
		}
	}

	public sealed interface Page {
	}

	public record TextPage(String textKey) implements Page {
	}

	/** Se déplie en une page par glyphe, dans l'ordre des rangs. */
	public record GlyphCatalogue() implements Page {
	}

	public record ExamplePage(String textKey, Drawing drawing) implements Page {
	}

	/** Le sceau de sang du lecteur : chacun y voit le sien. */
	public record SealPage(String textKey) implements Page {
	}

	/**
	 * Le catalogue se complète au fil du savoir : un glyphe y paraît une fois compris, ou dès qu'on
	 * l'a tracé ; les autres restent des pages blanches qui disent où l'apprendre.
	 */
	public static boolean revealed(String glyph, Set<String> known, int uses) {
		return known.contains(glyph) || uses > 0;
	}

	public static Treatise parse(JsonObject json) {
		List<Chapter> chapters = new ArrayList<>();
		for (JsonElement c : json.getAsJsonArray("chapters")) {
			JsonObject chapter = c.getAsJsonObject();
			List<Page> pages = new ArrayList<>();
			for (JsonElement p : chapter.getAsJsonArray("pages")) {
				JsonObject page = p.getAsJsonObject();
				if (page.has("glyphs")) {
					pages.add(new GlyphCatalogue());
				} else if (page.has("seal")) {
					pages.add(new SealPage(page.get("text").getAsString()));
				} else if (page.has("example")) {
					List<Primitive> prims = new ArrayList<>();
					page.getAsJsonArray("example").forEach(e -> prims.add(GlyphJson.primitive(e.getAsJsonObject())));
					pages.add(new ExamplePage(page.get("text").getAsString(), new Drawing(prims)));
				} else {
					pages.add(new TextPage(page.get("text").getAsString()));
				}
			}
			chapters.add(new Chapter(chapter.get("title").getAsString(), pages,
					chapter.has("unlock") ? Unlock.parse(chapter.getAsJsonObject("unlock")) : Unlock.ALWAYS));
		}
		return new Treatise(chapters);
	}
}
