package com.ajustor.fmab.alchemy.exchange;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Ce dont un objet est fait, élément par élément : une pioche de fer, c'est 27 de fer et 2 de bois.
 * Les éléments sont ceux des glyphes (terre, fer, cuivre, or, eau…) et du bois, qui n'en a pas.
 *
 * <p>Les objets de base (lingots, pierre, planches…) tiennent leur masse des tables d'échange ; les
 * autres la tirent de leurs recettes : la somme de leurs ingrédients, divisée par ce que la recette
 * produit. {@link #compute} fait ce calcul pour tout un jeu de recettes, sans rien savoir de
 * Minecraft, ce qui le rend testable.
 */
public record Composition(Map<String, Double> masses) {
	public Composition {
		masses = Collections.unmodifiableMap(new LinkedHashMap<>(masses));
	}

	public static Composition of(String element, double mass) {
		return new Composition(Map.of(element, mass));
	}

	public double total() {
		return masses.values().stream().mapToDouble(Double::doubleValue).sum();
	}

	/** L'élément dont l'objet est surtout fait : c'est lui que le cercle doit viser. */
	public String principal() {
		String best = null;
		for (var entry : new TreeMap<>(masses).entrySet()) {
			if (best == null || entry.getValue() > masses.get(best) + 1e-9) {
				best = entry.getKey();
			}
		}
		if (best == null) {
			throw new IllegalStateException("composition vide");
		}
		return best;
	}

	/** Ce que coûte une copie : chaque masse arrondie au-dessus (on ne paie pas une demi-pépite). */
	public Map<String, Integer> cost() {
		Map<String, Integer> cost = new LinkedHashMap<>();
		masses.forEach((element, mass) -> cost.put(element, (int) Math.ceil(mass - 1e-9)));
		return cost;
	}

	/** Ce que rend une décomposition : chaque masse arrondie au-dessous (le reste se perd). */
	public Map<String, Integer> yield() {
		Map<String, Integer> yield = new LinkedHashMap<>();
		masses.forEach((element, mass) -> yield.put(element, (int) Math.floor(mass + 1e-9)));
		return yield;
	}

	private Composition plus(Composition other) {
		Map<String, Double> sum = new HashMap<>(masses);
		other.masses.forEach((element, mass) -> sum.merge(element, mass, Double::sum));
		return new Composition(sum);
	}

	private Composition divided(int count) {
		Map<String, Double> out = new HashMap<>();
		masses.forEach((element, mass) -> out.put(element, mass / count));
		return new Composition(out);
	}

	/**
	 * Une recette : des ingrédients (chacun accepte plusieurs objets, comme « n'importe quelle
	 * planche »), un résultat et sa quantité.
	 */
	public record Recipe<K>(List<List<K>> ingredients, K result, int count) {
	}

	/**
	 * Compose tout ce que les recettes permettent de composer à partir des objets de base. Pour
	 * chaque ingrédient, on prend l'option la moins lourde ; pour chaque objet, la recette la moins
	 * chère. Les objets de base gardent leur valeur ; un objet dont un ingrédient n'a aucune valeur
	 * (de la ficelle, de la redstone…) n'en a pas non plus.
	 *
	 * @param passes nombre de tours au plus : chaque tour monte d'un niveau dans les recettes
	 */
	public static <K> Map<K, Composition> compute(Map<K, Composition> base, List<Recipe<K>> recipes, int passes) {
		Map<K, Composition> values = new HashMap<>(base);
		for (int pass = 0; pass < passes; pass++) {
			boolean changed = false;
			for (Recipe<K> recipe : recipes) {
				if (base.containsKey(recipe.result()) || recipe.count() <= 0) {
					continue;
				}
				Optional<Composition> cost = cost(recipe, values);
				if (cost.isEmpty()) {
					continue;
				}
				Composition unit = cost.get().divided(recipe.count());
				Composition known = values.get(recipe.result());
				if (known == null || unit.total() < known.total() - 1e-9) {
					values.put(recipe.result(), unit);
					changed = true;
				}
			}
			if (!changed) {
				break;
			}
		}
		return values;
	}

	private static <K> Optional<Composition> cost(Recipe<K> recipe, Map<K, Composition> values) {
		Composition total = new Composition(Map.of());
		for (List<K> options : recipe.ingredients()) {
			Optional<Composition> cheapest = options.stream()
					.map(values::get)
					.filter(c -> c != null)
					.min(Comparator.comparingDouble(Composition::total));
			if (cheapest.isEmpty()) {
				return Optional.empty();
			}
			total = total.plus(cheapest.get());
		}
		return total.masses.isEmpty() ? Optional.empty() : Optional.of(total);
	}
}
