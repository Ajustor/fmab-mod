package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.circle.Stage;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.GlyphJson;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Cercles de test communs au mod et à l'éditeur web ({@code testdata/circles/*.json}). Le Java
 * est la référence : {@code ./gradlew test -PwriteFixtures=true} réécrit les résultats attendus à
 * partir des scénarios ci-dessous ; sans cette option, le test vérifie que les fichiers et le code
 * sont d'accord. L'éditeur web rejoue les mêmes fichiers avec son portage TypeScript.
 */
class CircleFixturesTest {
	private static final Path DIR = Path.of(System.getProperty("fmab.testdata", "testdata")).resolve("circles");

	private final CircleParser parser = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
	private final CircleAnalyzer analyzer = new CircleAnalyzer(TestGlyphs.combinations(), TestGlyphs.all().values());

	@TestFactory
	List<DynamicTest> fixturesMatchTheReferenceImplementation() throws IOException {
		if (Boolean.getBoolean("fmab.writeFixtures")) {
			write();
		}
		List<DynamicTest> tests = new ArrayList<>();
		try (Stream<Path> files = Files.list(DIR)) {
			for (Path f : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
				tests.add(DynamicTest.dynamicTest(f.getFileName().toString(), () -> {
					JsonObject fixture = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
					Rank rank = Rank.fromSerializedName(fixture.get("rank").getAsString());
					Drawing drawing = drawing(fixture.getAsJsonArray("primitives"));
					JsonObject actual = summary(analyzer.analyze(parser.parse(drawing), rank, null));
					assertEquals(fixture.getAsJsonObject("expected"), actual);
				}));
			}
		}
		return tests;
	}

	/** Ce que les deux implémentations doivent trouver à l'identique. */
	static JsonObject summary(Analysis a) {
		JsonObject o = new JsonObject();
		o.addProperty("outcome", a.outcome().name().toLowerCase(Locale.ROOT));
		o.addProperty("complexity", a.complexity());
		o.addProperty("concentration", a.concentration());
		o.addProperty("stability", Math.round(a.stability() * 1000) / 1000.0);
		o.addProperty("required_rank", a.requiredRank().serializedName());
		o.addProperty("rebound", Math.round(a.reboundSeverity() * 1000) / 1000.0);
		JsonArray stages = new JsonArray();
		for (Stage s : a.parsed().stages()) {
			JsonObject st = new JsonObject();
			st.addProperty("sides", s.sides());
			st.addProperty("capacity", s.capacity());
			st.addProperty("hexagram", s.hexagram());
			st.addProperty("intensity", s.intensity());
			st.addProperty("direction", s.direction().isPresent());
			st.addProperty("link", s.link().name().toLowerCase(Locale.ROOT));
			JsonArray glyphs = new JsonArray();
			s.glyphs().forEach(g -> glyphs.add(g.glyph().id()));
			st.add("glyphs", glyphs);
			st.addProperty("satellites", s.satellites().size());
			stages.add(st);
		}
		o.add("stages", stages);
		JsonArray effects = new JsonArray();
		for (Analysis.StageEffect e : a.effects()) {
			effects.add(e.combination().effect() + (e.satellite() >= 0 ? "@satellite" : "")
					+ (e.infusions().isEmpty() ? "" : "+" + String.join("+", e.infusions().stream().sorted().toList())));
		}
		o.add("effects", effects);
		JsonArray issues = new JsonArray();
		for (CircleIssue i : a.issues()) {
			issues.add(i.kind().name().toLowerCase(Locale.ROOT));
		}
		o.add("issues", issues);
		return o;
	}

	private static Drawing drawing(JsonArray prims) {
		List<Primitive> out = new ArrayList<>();
		prims.forEach(p -> out.add(GlyphJson.primitive(p.getAsJsonObject())));
		return new Drawing(out);
	}

	// ---- Scénarios ------------------------------------------------------------------------------------

	private record Scenario(String rank, String description, List<Primitive> primitives) {
	}

	private static Map<String, Scenario> scenarios() {
		Map<String, Scenario> s = new LinkedHashMap<>();
		Vec2 top = new Vec2(16, 11), bottom = new Vec2(16, 20);
		s.put("wall", new Scenario("apprentice", "Terre + Fixer dans un triangle",
				circle(3, glyph("terre", top), glyph("fixer", bottom))));
		s.put("spike", new Scenario("apprentice", "Terre + Projeter dans un carré",
				circle(4, glyph("terre", top), glyph("projeter", bottom))));
		s.put("blade_square", new Scenario("apprentice", "Fer + Projeter dans un carré",
				circle(4, glyph("fer", top), glyph("projeter", bottom))));
		s.put("blade_triangle_unstable", new Scenario("apprentice", "Fer + Projeter dans un triangle : instable",
				circle(3, glyph("fer", top), glyph("projeter", bottom))));
		s.put("ice", new Scenario("apprentice", "Eau + Fixer", circle(3, glyph("eau", top), glyph("fixer", bottom))));
		s.put("decompose", new Scenario("apprentice", "Terre + Décomposer",
				circle(4, glyph("terre", top), glyph("decomposer", bottom))));
		s.put("repair", new Scenario("apprentice", "Fer + Réparer",
				circle(4, glyph("fer", top), glyph("reparer", bottom))));
		s.put("unknown_combination", new Scenario("apprentice", "Eau + Décomposer : rebond",
				circle(4, glyph("eau", top), glyph("decomposer", bottom))));
		s.put("ice_spike", new Scenario("apprentice", "Eau + Projeter : pique de glace",
				circle(4, glyph("eau", top), glyph("projeter", bottom))));
		s.put("no_ring", new Scenario("apprentice", "Pas d'anneau", glyph("terre", top)));
		s.put("hexagon_apprentice", new Scenario("apprentice", "Un hexagone dépasse l'Apprenti",
				circle(6, glyph("terre", top), glyph("fixer", bottom))));
		List<Primitive> dots = new ArrayList<>(circle(4, glyph("terre", top), glyph("fixer", bottom)));
		dots.add(new Primitive.Dot(new Vec2(2, 16)));
		dots.add(new Primitive.Dot(new Vec2(30, 16)));
		s.put("intensity", new Scenario("apprentice", "Deux points d'intensité", dots));
		List<Primitive> arrow = new ArrayList<>(circle(4, glyph("terre", top), glyph("fixer", bottom)));
		arrow.addAll(TestGlyphs.drawn("direction", new Vec2(30, 16), 1.5, 90));
		s.put("direction", new Scenario("apprentice", "Flèche de direction", arrow));
		List<Primitive> stray = new ArrayList<>(circle(4, glyph("terre", top), glyph("fixer", bottom)));
		stray.add(new Primitive.Line(new Vec2(6, 14), new Vec2(8, 18)));
		s.put("unreadable", new Scenario("apprentice", "Un trait illisible", stray));
		for (String name : List.of("feu", "air", "cuivre", "recomposer")) {
			s.put("glyph_" + name, new Scenario("alchemist", "Reconnaissance : " + name,
					circle(4, glyph(name, top), glyph("projeter", bottom))));
		}
		s.put("series", new Scenario("alchemist", "Deux étages en série", twoStages(List.of(
				new Primitive.Line(new Vec2(11.5, 20.5), new Vec2(7.5, 24.5))))));
		s.put("parallel", new Scenario("alchemist", "Deux étages en parallèle", twoStages(List.of(
				new Primitive.Line(new Vec2(11.5, 20.5), new Vec2(7.5, 24.5)),
				new Primitive.Line(new Vec2(12, 21), new Vec2(8, 25))))));
		s.put("conditional", new Scenario("alchemist", "Deux étages, trait brisé", twoStages(List.of(
				new Primitive.Line(new Vec2(11.5, 20.5), new Vec2(10, 22)),
				new Primitive.Line(new Vec2(9, 23), new Vec2(7.5, 24.5))))));
		s.put("unlinked", new Scenario("alchemist", "Deux étages non reliés", twoStages(List.of())));
		List<Primitive> infusion = new ArrayList<>(List.of(new Primitive.Line(new Vec2(11.5, 20.5), new Vec2(7.5, 24.5)),
				new Primitive.Circle(new Vec2(26, 10), 3)));
		infusion.addAll(TestGlyphs.drawn("feu", new Vec2(26, 10), 2, 0));
		s.put("satellite_infusion", new Scenario("alchemist", "Satellite de Feu", twoStages(infusion)));
		List<Primitive> complete = new ArrayList<>(List.of(new Primitive.Line(new Vec2(11.5, 20.5), new Vec2(7.5, 24.5)),
				new Primitive.Circle(new Vec2(26, 10), 4.5)));
		complete.addAll(TestGlyphs.drawn("terre", new Vec2(26, 8), 2, 0));
		complete.addAll(TestGlyphs.drawn("fixer", new Vec2(26, 12.5), 1.5, 0));
		s.put("satellite_complete", new Scenario("alchemist", "Satellite complet", twoStages(complete)));
		List<Primitive> fusion = new ArrayList<>(List.of(new Primitive.Circle(Drawing.CENTER_POINT, 14),
				polygon(14, 3, -90), polygon(14, 3, 90), polygon(14, 6, 0)));
		fusion.addAll(TestGlyphs.drawn("feu", new Vec2(12, 12), 2.5, 0));
		fusion.addAll(TestGlyphs.drawn("air", new Vec2(20, 12), 2.5, 0));
		fusion.addAll(TestGlyphs.drawn("projeter", new Vec2(16, 20), 2.5, 0));
		s.put("fusion_layered", new Scenario("alchemist", "Hexagramme et hexagone", fusion));
		List<Primitive> blast = new ArrayList<>(fusion.subList(0, fusion.size()));
		blast.removeAll(TestGlyphs.drawn("projeter", new Vec2(16, 20), 2.5, 0));
		blast.addAll(TestGlyphs.drawn("decomposer", new Vec2(16, 20), 2.5, 0));
		s.put("detonation", new Scenario("alchemist", "Feu + Air + Décomposer : détonation", blast));
		List<Primitive> noFusion = new ArrayList<>(fusion.subList(0, 1));
		noFusion.add(polygon(14, 4, -90));
		noFusion.addAll(fusion.subList(4, fusion.size()));
		s.put("fusion_without_hexagram", new Scenario("alchemist", "Deux éléments sans hexagramme", noFusion));
		return s;
	}

	private void write() throws IOException {
		Files.createDirectories(DIR);
		for (Map.Entry<String, Scenario> e : scenarios().entrySet()) {
			Scenario sc = e.getValue();
			Drawing d = new Drawing(sc.primitives());
			JsonObject o = new JsonObject();
			o.addProperty("description", sc.description());
			o.addProperty("rank", sc.rank());
			JsonArray prims = new JsonArray();
			d.primitives().forEach(p -> prims.add(GlyphJson.toJson(p)));
			o.add("primitives", prims);
			o.add("expected", summary(analyzer.analyze(parser.parse(d), Rank.fromSerializedName(sc.rank()), null)));
			Files.writeString(DIR.resolve(e.getKey() + ".json"),
					new GsonBuilder().setPrettyPrinting().create().toJson(o) + "\n");
		}
	}

	private static List<Primitive> glyph(String name, Vec2 at) {
		return TestGlyphs.drawn(name, at, 3, 0);
	}

	@SafeVarargs
	private static List<Primitive> circle(int sides, List<Primitive>... glyphs) {
		List<Primitive> all = new ArrayList<>();
		all.add(new Primitive.Circle(Drawing.CENTER_POINT, 14));
		all.add(polygon(14, sides, -90));
		for (List<Primitive> g : glyphs) {
			all.addAll(g);
		}
		return all;
	}

	private static List<Primitive> twoStages(List<Primitive> extra) {
		List<Primitive> all = new ArrayList<>();
		all.add(new Primitive.Circle(Drawing.CENTER_POINT, 6));
		all.add(polygon(6, 4, -90));
		all.addAll(TestGlyphs.drawn("terre", new Vec2(16, 13.5), 2, 0));
		all.addAll(TestGlyphs.drawn("decomposer", new Vec2(16, 18.5), 1.8, 0));
		all.add(new Primitive.Circle(Drawing.CENTER_POINT, 12));
		all.add(polygon(12, 6, -90));
		all.addAll(TestGlyphs.drawn("terre", new Vec2(16, 7), 2, 0));
		all.addAll(TestGlyphs.drawn("projeter", new Vec2(16, 25), 2, 0));
		all.addAll(extra);
		return all;
	}

	private static Primitive polygon(double radius, int sides, double startDeg) {
		List<Vec2> vertices = new ArrayList<>();
		for (int i = 0; i < sides; i++) {
			double a = Math.toRadians(startDeg + 360.0 * i / sides);
			vertices.add(new Vec2(Math.round(16 + radius * Math.cos(a)), Math.round(16 + radius * Math.sin(a))));
		}
		return new Primitive.Polygon(vertices);
	}
}
