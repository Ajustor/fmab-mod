package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphJson;
import com.ajustor.fmab.alchemy.knowledge.KnowledgeNode;
import com.ajustor.fmab.alchemy.rules.Combination;
import com.ajustor.fmab.alchemy.rules.CombinationTable;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Les glyphes du dépôt, lus comme le mod et l'éditeur web les lisent. */
public final class TestGlyphs {
	private static Map<String, Glyph> cache;

	private TestGlyphs() {
	}

	public static Path resources() {
		return Path.of(System.getProperty("fmab.resources", "src/main/resources"));
	}

	public static synchronized Map<String, Glyph> all() {
		if (cache == null) {
			Map<String, Glyph> out = new LinkedHashMap<>();
			try (Stream<Path> files = Files.list(resources().resolve("data/fmab/fmab/glyph"))) {
				for (Path f : files.sorted().toList()) {
					Glyph g = GlyphJson.parse(JsonParser.parseString(Files.readString(f)).getAsJsonObject());
					out.put(g.id(), g);
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			cache = out;
		}
		return cache;
	}

	public static CombinationTable combinations() {
		List<Combination> out = new ArrayList<>();
		try (Stream<Path> files = Files.list(resources().resolve("data/fmab/fmab/combination"))) {
			for (Path f : files.sorted().toList()) {
				String id = "fmab:" + f.getFileName().toString().replace(".json", "");
				out.add(CombinationTable.parse(id, JsonParser.parseString(Files.readString(f)).getAsJsonObject()));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return new CombinationTable(out);
	}

	/** Les nœuds de savoir, identifiés comme le jeu le fait : {@code fmab:<école>/<nom>}. */
	public static List<KnowledgeNode> knowledgeNodes() {
		Path root = resources().resolve("data/fmab/fmab/knowledge");
		List<KnowledgeNode> out = new ArrayList<>();
		try (Stream<Path> files = Files.walk(root)) {
			for (Path f : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
				String id = "fmab:" + root.relativize(f).toString().replace('\\', '/').replace(".json", "");
				out.add(KnowledgeNode.parse(id, JsonParser.parseString(Files.readString(f)).getAsJsonObject()));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return out;
	}

	public static Glyph get(String name) {
		Glyph g = all().get("fmab:" + name);
		if (g == null) {
			throw new IllegalArgumentException("glyphe inconnu : " + name);
		}
		return g;
	}

	/**
	 * Le tracé de référence d'un glyphe, posé dans le carnet comme le ferait un joueur : centré en
	 * {@code at}, demi-taille {@code halfSize} cases, tourné de {@code rotationDeg}, chaque
	 * coordonnée accrochée à la demi-case.
	 */
	public static List<Primitive> drawn(String name, Vec2 at, double halfSize, double rotationDeg) {
		double rad = Math.toRadians(rotationDeg);
		return get(name).primitives().stream()
				.map(p -> snap(p.map(v -> v.rotate(rad).scale(halfSize).add(at))))
				.toList();
	}

	public static Primitive snap(Primitive p) {
		return switch (p) {
			case Primitive.Circle c -> new Primitive.Circle(snap(c.center()), Math.max(0.5, half(c.radius())));
			case Primitive.Arc a -> new Primitive.Arc(snap(a.center()), Math.max(0.5, half(a.radius())),
					Math.round(a.startDeg() / 15) * 15.0, Math.round(a.sweepDeg() / 15) * 15.0);
			default -> p.map(TestGlyphs::snap);
		};
	}

	private static Vec2 snap(Vec2 v) {
		return new Vec2(half(v.x()), half(v.y()));
	}

	private static double half(double d) {
		return Math.round(d * 2) / 2.0;
	}
}
