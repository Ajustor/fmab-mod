package com.ajustor.fmab.alchemy.knowledge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Un nœud de l'arbre de savoir, lu depuis {@code data/<ns>/knowledge/<école>/<nom>.json}.
 *
 * <pre>{@code
 * {
 *   "school": "earth",
 *   "mastery": 15,
 *   "parent": "fmab:earth/docile_stone",       // facultatif
 *   "perks": [ { "type": "concentration_discount", "value": 1 } ],
 *   "name_key": "knowledge.fmab.earth.docile_stone",
 *   "description_key": "knowledge.fmab.earth.docile_stone.desc"
 * }
 * }</pre>
 *
 * @param mastery maîtrise de l'école à atteindre ; un livre ou un maître peut aussi l'accorder
 *                directement
 */
public record KnowledgeNode(String id, String school, int mastery, Optional<String> parent, List<Perk> perks,
		String nameKey, String descriptionKey) {
	public KnowledgeNode {
		perks = List.copyOf(perks);
	}

	public KnowledgeNode withId(String newId) {
		return new KnowledgeNode(newId, school, mastery, parent, perks, nameKey, descriptionKey);
	}

	/**
	 * Ce que le nœud apporte.
	 *
	 * <ul>
	 *   <li>{@code concentration_discount} : concentration en moins pour chaque effet de l'école ;</li>
	 *   <li>{@code range_bonus} : portée en plus pour chaque effet de l'école ;</li>
	 *   <li>{@code wall_thickness} : épaisseur ajoutée aux murs ;</li>
	 *   <li>{@code unlock} : rien en soi, mais les combinaisons qui demandent ce nœud deviennent
	 *   possibles.</li>
	 * </ul>
	 */
	public record Perk(String type, double value) {
	}

	public static KnowledgeNode parse(String id, JsonObject json) {
		try {
			List<Perk> perks = new ArrayList<>();
			if (json.has("perks")) {
				for (JsonElement e : json.getAsJsonArray("perks")) {
					JsonObject p = e.getAsJsonObject();
					perks.add(new Perk(p.get("type").getAsString(), p.has("value") ? p.get("value").getAsDouble() : 0));
				}
			}
			return new KnowledgeNode(
					id,
					json.get("school").getAsString(),
					json.get("mastery").getAsInt(),
					json.has("parent") ? Optional.of(json.get("parent").getAsString()) : Optional.empty(),
					perks,
					json.get("name_key").getAsString(),
					json.get("description_key").getAsString());
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Nœud de savoir " + id + " invalide : " + e.getMessage(), e);
		}
	}

	public static JsonObject toJson(KnowledgeNode node) {
		JsonObject o = new JsonObject();
		o.addProperty("school", node.school());
		o.addProperty("mastery", node.mastery());
		node.parent().ifPresent(p -> o.addProperty("parent", p));
		JsonArray perks = new JsonArray();
		for (Perk perk : node.perks()) {
			JsonObject p = new JsonObject();
			p.addProperty("type", perk.type());
			p.addProperty("value", perk.value());
			perks.add(p);
		}
		o.add("perks", perks);
		o.addProperty("name_key", node.nameKey());
		o.addProperty("description_key", node.descriptionKey());
		return o;
	}
}
