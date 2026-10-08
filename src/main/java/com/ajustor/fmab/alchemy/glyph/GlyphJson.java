package com.ajustor.fmab.alchemy.glyph;

import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lecture du format JSON des glyphes. Volontairement sans dépendance à Minecraft : les tests et
 * l'éditeur web lisent les mêmes fichiers.
 *
 * <pre>{@code
 * {
 *   "id": "fmab:terre",
 *   "layer": "element",
 *   "element": "earth",            // ou "action": "fix", ou "modifier": "intensity"
 *   "primitives": [ { "type": "polygon", "points": [[-0.8, -0.6], [0.8, -0.6], [0.0, 0.8]] } ],
 *   "complexity": 1,
 *   "concentration": 2,
 *   "rank": "apprentice",
 *   "tolerance": { "rotation_deg": 15, "position": 0.1, "scale": [0.7, 1.4] },
 *   "name_key": "glyph.fmab.terre",
 *   "description_key": "glyph.fmab.terre.desc",
 *   "since": "0.1"
 * }
 * }</pre>
 */
public final class GlyphJson {
	private GlyphJson() {
	}

	public static Glyph parse(JsonObject json) {
		String id = string(json, "id");
		try {
			GlyphLayer layer = GlyphLayer.fromSerializedName(string(json, "layer"));
			String roleKey = layer.name().toLowerCase(Locale.ROOT);
			String role = string(json, roleKey);
			List<Primitive> primitives = new ArrayList<>();
			for (JsonElement e : array(json, "primitives")) {
				primitives.add(primitive(e.getAsJsonObject()));
			}
			if (primitives.isEmpty()) {
				throw new IllegalArgumentException("aucune primitive");
			}
			Tolerance tolerance = Tolerance.DEFAULT;
			if (json.has("tolerance")) {
				JsonObject t = json.getAsJsonObject("tolerance");
				JsonArray scale = t.getAsJsonArray("scale");
				tolerance = new Tolerance(
						t.get("rotation_deg").getAsDouble(),
						t.get("position").getAsDouble(),
						scale.get(0).getAsDouble(),
						scale.get(1).getAsDouble());
			}
			return new Glyph(
					id,
					layer,
					role,
					primitives,
					json.get("complexity").getAsInt(),
					json.get("concentration").getAsInt(),
					Rank.fromSerializedName(string(json, "rank")),
					tolerance,
					string(json, "name_key"),
					string(json, "description_key"),
					json.has("since") ? string(json, "since") : "0.1");
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Glyphe " + id + " invalide : " + e.getMessage(), e);
		}
	}

	public static Primitive primitive(JsonObject p) {
		String type = string(p, "type");
		return switch (type) {
			case "line" -> new Primitive.Line(vec(p.get("from")), vec(p.get("to")));
			case "polyline" -> new Primitive.Polyline(points(p));
			case "polygon" -> new Primitive.Polygon(points(p));
			case "circle" -> new Primitive.Circle(vec(p.get("center")), p.get("radius").getAsDouble());
			case "arc" -> new Primitive.Arc(
					vec(p.get("center")),
					p.get("radius").getAsDouble(),
					p.get("start_deg").getAsDouble(),
					p.get("sweep_deg").getAsDouble());
			case "dot" -> new Primitive.Dot(vec(p.get("at")));
			default -> throw new IllegalArgumentException("type de primitive inconnu : " + type);
		};
	}

	public static JsonObject toJson(Glyph g) {
		JsonObject o = new JsonObject();
		o.addProperty("id", g.id());
		String layer = g.layer().name().toLowerCase(Locale.ROOT);
		o.addProperty("layer", layer);
		o.addProperty(layer, g.role());
		JsonArray prims = new JsonArray();
		g.primitives().forEach(p -> prims.add(GlyphJson.toJson(p)));
		o.add("primitives", prims);
		o.addProperty("complexity", g.complexity());
		o.addProperty("concentration", g.concentration());
		o.addProperty("rank", g.rank().serializedName());
		JsonObject tol = new JsonObject();
		tol.addProperty("rotation_deg", g.tolerance().rotationDeg());
		tol.addProperty("position", g.tolerance().position());
		JsonArray scale = new JsonArray();
		scale.add(g.tolerance().minScale());
		scale.add(g.tolerance().maxScale());
		tol.add("scale", scale);
		o.add("tolerance", tol);
		o.addProperty("name_key", g.nameKey());
		o.addProperty("description_key", g.descriptionKey());
		o.addProperty("since", g.since());
		return o;
	}

	public static JsonObject toJson(Primitive primitive) {
		JsonObject o = new JsonObject();
		switch (primitive) {
			case Primitive.Line l -> {
				o.addProperty("type", "line");
				o.add("from", vec(l.from()));
				o.add("to", vec(l.to()));
			}
			case Primitive.Polyline l -> {
				o.addProperty("type", "polyline");
				o.add("points", vecs(l.points()));
			}
			case Primitive.Polygon g -> {
				o.addProperty("type", "polygon");
				o.add("points", vecs(g.points()));
			}
			case Primitive.Circle c -> {
				o.addProperty("type", "circle");
				o.add("center", vec(c.center()));
				o.addProperty("radius", c.radius());
			}
			case Primitive.Arc a -> {
				o.addProperty("type", "arc");
				o.add("center", vec(a.center()));
				o.addProperty("radius", a.radius());
				o.addProperty("start_deg", a.startDeg());
				o.addProperty("sweep_deg", a.sweepDeg());
			}
			case Primitive.Dot d -> {
				o.addProperty("type", "dot");
				o.add("at", vec(d.at()));
			}
		}
		return o;
	}

	private static List<Vec2> points(JsonObject p) {
		List<Vec2> out = new ArrayList<>();
		for (JsonElement e : array(p, "points")) {
			out.add(vec(e));
		}
		return out;
	}

	private static Vec2 vec(JsonElement e) {
		JsonArray a = e.getAsJsonArray();
		return new Vec2(a.get(0).getAsDouble(), a.get(1).getAsDouble());
	}

	private static JsonArray vec(Vec2 v) {
		JsonArray a = new JsonArray();
		a.add(v.x());
		a.add(v.y());
		return a;
	}

	private static JsonArray vecs(List<Vec2> vs) {
		JsonArray a = new JsonArray();
		vs.forEach(v -> a.add(vec(v)));
		return a;
	}

	private static String string(JsonObject o, String key) {
		JsonElement e = o.get(key);
		if (e == null) {
			throw new IllegalArgumentException("champ « " + key + " » manquant");
		}
		return e.getAsString();
	}

	private static JsonArray array(JsonObject o, String key) {
		JsonElement e = o.get(key);
		if (e == null) {
			throw new IllegalArgumentException("champ « " + key + " » manquant");
		}
		return e.getAsJsonArray();
	}
}
