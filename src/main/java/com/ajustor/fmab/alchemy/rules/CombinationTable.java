package com.ajustor.fmab.alchemy.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class CombinationTable {
	private final List<Combination> combinations;

	public CombinationTable(Collection<Combination> combinations) {
		this.combinations = List.copyOf(combinations);
	}

	public Optional<Combination> find(Set<String> elements, String action) {
		return combinations.stream()
				.filter(c -> c.action().equals(action) && c.elements().equals(elements))
				.findFirst();
	}

	public List<Combination> all() {
		return combinations;
	}

	public static Combination parse(String id, JsonObject json) {
		try {
			Set<String> elements = new HashSet<>();
			for (JsonElement e : json.getAsJsonArray("elements")) {
				elements.add(e.getAsString());
			}
			return new Combination(
					id,
					elements,
					json.get("action").getAsString(),
					json.get("effect").getAsString(),
					json.has("range") ? json.get("range").getAsDouble() : 1);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Combinaison " + id + " invalide : " + e.getMessage(), e);
		}
	}

	public static JsonObject toJson(Combination c) {
		JsonObject o = new JsonObject();
		JsonArray elements = new JsonArray();
		c.elements().stream().sorted().forEach(elements::add);
		o.add("elements", elements);
		o.addProperty("action", c.action());
		o.addProperty("effect", c.effect());
		o.addProperty("range", c.range());
		return o;
	}
}
