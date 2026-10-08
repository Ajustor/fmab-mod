package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.glyph.GlyphJson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Plan du Traité d'alchimie, lu depuis {@code assets/fmab/treatise/treatise.json} : des chapitres
 * faits de pages de texte (clés de traduction), du catalogue des glyphes et de cercles d'exemple.
 */
public record Treatise(List<Chapter> chapters) {
	public record Chapter(String titleKey, List<Page> pages) {
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

	public static Treatise parse(JsonObject json) {
		List<Chapter> chapters = new ArrayList<>();
		for (JsonElement c : json.getAsJsonArray("chapters")) {
			JsonObject chapter = c.getAsJsonObject();
			List<Page> pages = new ArrayList<>();
			for (JsonElement p : chapter.getAsJsonArray("pages")) {
				JsonObject page = p.getAsJsonObject();
				if (page.has("glyphs")) {
					pages.add(new GlyphCatalogue());
				} else if (page.has("example")) {
					List<Primitive> prims = new ArrayList<>();
					page.getAsJsonArray("example").forEach(e -> prims.add(GlyphJson.primitive(e.getAsJsonObject())));
					pages.add(new ExamplePage(page.get("text").getAsString(), new Drawing(prims)));
				} else {
					pages.add(new TextPage(page.get("text").getAsString()));
				}
			}
			chapters.add(new Chapter(chapter.get("title").getAsString(), pages));
		}
		return new Treatise(chapters);
	}
}
