package com.ajustor.fmab.alchemy.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CombinationTable {
	/** École d'un élément, quand la combinaison ne la précise pas. */
	private static final Map<String, String> SCHOOLS = Map.of(
			"earth", "earth",
			"iron", "metal",
			"copper", "metal",
			"water", "water",
			"fire", "fire",
			"air", "fire");

	private final List<Combination> combinations;

	public CombinationTable(Collection<Combination> combinations) {
		this.combinations = List.copyOf(combinations);
	}

	/**
	 * La combinaison de ces éléments et de cette action : la sienne d'abord, sinon, pour un élément
	 * seul, le joker de l'action ({@code "elements": ["*"]}).
	 */
	public Optional<Combination> find(Set<String> elements, String action) {
		Optional<Combination> exact = combinations.stream()
				.filter(c -> c.action().equals(action) && c.elements().equals(elements))
				.findFirst();
		if (exact.isPresent() || elements.size() != 1) {
			return exact;
		}
		String element = elements.iterator().next();
		return combinations.stream()
				.filter(c -> c.wildcard() && c.action().equals(action) && !c.except().contains(element))
				.findFirst()
				.map(c -> c.forElement(element, SCHOOLS.getOrDefault(element, "earth")));
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
			Set<String> formula = new HashSet<>();
			if (json.has("formula")) {
				for (JsonElement e : json.getAsJsonArray("formula")) {
					formula.add(e.getAsString());
				}
			}
			Set<String> except = new HashSet<>();
			if (json.has("except")) {
				for (JsonElement e : json.getAsJsonArray("except")) {
					except.add(e.getAsString());
				}
			}
			boolean wildcard = elements.contains(Combination.ANY);
			return new Combination(
					id,
					elements,
					json.get("action").getAsString(),
					json.get("effect").getAsString(),
					json.has("range") ? json.get("range").getAsDouble() : 1,
					// Un joker sans école prend celle de l'élément auquel il s'applique.
					json.has("school") ? json.get("school").getAsString() : wildcard ? "" : SCHOOLS.getOrDefault(
							elements.stream().sorted().findFirst().orElse(""), "earth"),
					json.has("requires") ? Optional.of(json.get("requires").getAsString()) : Optional.empty(),
					json.has("passive") ? Optional.of(json.get("passive").getAsString()) : Optional.empty(),
					formula,
					except,
					json.has("spec") ? Optional.of(json.getAsJsonObject("spec")) : Optional.empty());
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
		o.addProperty("school", c.school());
		c.requires().ifPresent(r -> o.addProperty("requires", r));
		if (!c.except().isEmpty()) {
			JsonArray except = new JsonArray();
			c.except().stream().sorted().forEach(except::add);
			o.add("except", except);
		}
		c.spec().ifPresent(s -> o.add("spec", s.deepCopy()));
		c.passive().ifPresent(p -> o.addProperty("passive", p));
		if (!c.formula().isEmpty()) {
			JsonArray formula = new JsonArray();
			c.formula().stream().sorted().forEach(formula::add);
			o.add("formula", formula);
		}
		return o;
	}
}
