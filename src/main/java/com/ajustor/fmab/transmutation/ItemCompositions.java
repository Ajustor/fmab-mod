package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.exchange.Composition;
import com.ajustor.fmab.alchemy.exchange.ExchangeValue;
import com.ajustor.fmab.data.AlloyGroup;
import com.ajustor.fmab.registry.FmabRegistries;
import com.ajustor.fmab.registry.FmabTags;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * La composition de chaque objet du jeu, tirée des tables d'échange (pour les matières de base),
 * des alliages et des recettes (pour le reste), celles du jeu comme celles des autres mods. Calculée une fois
 * par jeu de recettes, à la première transmutation qui en a besoin.
 */
public final class ItemCompositions {
	/** Les éléments, dans l'ordre où l'on range une matière qui en porterait plusieurs. */
	public static final List<String> ELEMENTS = List.of("iron", "copper", "gold", "silver", "tin", "lead", "zinc",
			"nickel", "aluminum", "netherite", "mercury", "carbon", "crystal", "sulfur", "calcium", "earth", "water",
			"wood", "fiber", "plant", "flesh");
	/** Profondeur des recettes : un objet fait d'objets faits d'objets… */
	private static final int PASSES = 12;
	/**
	 * Ce que rend une décomposition, élément par élément : les matières de base, de la plus lourde à
	 * la plus légère (on rend un lingot plutôt que neuf pépites). Un élément absent d'ici (le zinc
	 * des autres mods…) rend ses matières de base, une par masse, les lingots d'abord.
	 */
	private static final Map<String, List<String>> UNITS = Map.ofEntries(Map.entry("carbon",
					List.of("minecraft:diamond", "minecraft:coal")),
			Map.entry("crystal", List.of("minecraft:amethyst_shard")),
			Map.entry("mercury", List.of("minecraft:redstone_block", "minecraft:redstone")),
			Map.entry("sulfur", List.of("minecraft:gunpowder")),
			Map.entry("calcium", List.of("minecraft:bone_block", "minecraft:bone_meal")),
			Map.entry("netherite", List.of("minecraft:netherite_scrap")),
			Map.entry("plant", List.of("minecraft:wheat")),
			Map.entry("flesh", List.of("minecraft:rotten_flesh")),
			Map.entry("iron", List.of("minecraft:iron_block", "minecraft:iron_ingot", "minecraft:iron_nugget")),
			Map.entry("copper", List.of("minecraft:copper_block", "minecraft:copper_ingot")),
			Map.entry("gold", List.of("minecraft:gold_block", "minecraft:gold_ingot", "minecraft:gold_nugget")),
			Map.entry("earth", List.of("minecraft:cobblestone")),
			Map.entry("water", List.of("minecraft:packed_ice", "minecraft:ice")),
			Map.entry("wood", List.of("minecraft:oak_log", "minecraft:oak_planks")),
			Map.entry("fiber", List.of("minecraft:white_wool", "minecraft:string")));
	/** Ce qu'une recette demande sans que sa matière entre dans l'objet (un gabarit de forge…). */
	private static final TagKey<Item> CATALYSTS = TagKey.create(Registries.ITEM, Fmab.id("composition_catalysts"));

	private static @Nullable RecipeManager cachedFor;
	private static Map<Item, Composition> cache = Map.of();
	private static Map<Item, Composition> base = Map.of();
	/** Par élément, les pièces qu'une décomposition rend, de la plus lourde à la plus légère. */
	private static Map<String, List<Item>> units = Map.of();

	private ItemCompositions() {
	}

	/**
	 * Le calcul parcourt tous les objets et toutes les recettes : on le fait au démarrage et après un
	 * /reload, plutôt qu'à la première transmutation d'un joueur.
	 */
	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> all(server.overworld()));
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
			if (success) {
				all(server.overworld());
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> forget());
	}

	private static synchronized void forget() {
		cachedFor = null;
		cache = Map.of();
		base = Map.of();
		units = Map.of();
	}

	/** La composition d'un objet, s'il en a une. */
	public static Optional<Composition> of(ServerLevel level, Item item) {
		return Optional.ofNullable(all(level).get(item));
	}

	/** Une matière de base (lingot, pierre, planche…), qu'on ne recompose pas : on la transmute. */
	public static boolean isBase(ServerLevel level, Item item) {
		all(level);
		return base.containsKey(item);
	}

	/** L'élément d'une matière de base, s'il en a un. */
	private static Optional<String> elementOf(ItemStack stack) {
		return ELEMENTS.stream().filter(e -> stack.is(FmabTags.elementItems(e))).findFirst();
	}

	/**
	 * La matière qu'une décomposition rend pour cette masse d'un élément : les plus grosses pièces
	 * d'abord. Ce qui reste sous la plus petite pièce se perd.
	 */
	public static List<ItemStack> units(ServerLevel level, String element, int mass) {
		all(level);
		List<ItemStack> out = new ArrayList<>();
		int remaining = mass;
		for (Item item : units.getOrDefault(element, List.of())) {
			int unitMass = (int) Math.round(base.get(item).total());
			if (unitMass <= 0) {
				continue;
			}
			int count = remaining / unitMass;
			remaining -= count * unitMass;
			while (count > 0) {
				int stack = Math.min(count, item.getDefaultMaxStackSize());
				out.add(new ItemStack(item, stack));
				count -= stack;
			}
		}
		return out;
	}

	/** Une pièce qu'une décomposition rend telle quelle : la défaire ne changerait rien. */
	public static boolean isUnit(ServerLevel level, Item item) {
		all(level);
		return units.values().stream().anyMatch(list -> list.contains(item));
	}

	private static synchronized Map<Item, Composition> all(ServerLevel level) {
		MinecraftServer server = level.getServer();
		RecipeManager recipes = server.getRecipeManager();
		if (recipes != cachedFor) {
			base = bases(level);
			units = units(base);
			Map<Item, Composition> known = new HashMap<>(alloys(level));
			known.putAll(base);
			cache = Composition.compute(known, recipes(recipes), PASSES);
			cachedFor = recipes;
		}
		return cache;
	}

	/** Les matières de base : un élément (tag {@code fmab:element/…}) et une valeur d'échange. */
	private static Map<Item, Composition> bases(ServerLevel level) {
		Map<Item, Composition> out = new HashMap<>();
		for (Item item : BuiltInRegistries.ITEM) {
			ItemStack stack = new ItemStack(item);
			Optional<String> element = elementOf(stack);
			Optional<ExchangeValue> value = element.isEmpty() ? Optional.empty() : MaterialPool.valueOf(level, stack);
			if (element.isPresent() && value.isPresent()) {
				out.put(item, Composition.of(element.get(), value.get().mass()));
			}
		}
		return out;
	}

	/**
	 * Les pièces de chaque élément : celles de {@link #UNITS} s'il en a, sinon une matière de base
	 * par masse (un lingot plutôt qu'une plaque, un objet du jeu plutôt que d'un mod).
	 */
	private static Map<String, List<Item>> units(Map<Item, Composition> base) {
		Map<String, List<Item>> out = new HashMap<>();
		Map<String, Map<Integer, Item>> byMass = new HashMap<>();
		Comparator<Item> preferred = Comparator.comparingInt(ItemCompositions::shapeRank)
				.thenComparing(i -> !BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("minecraft"))
				.thenComparing(i -> BuiltInRegistries.ITEM.getKey(i).toString());
		for (var entry : base.entrySet()) {
			String element = entry.getValue().principal();
			int mass = (int) Math.round(entry.getValue().total());
			byMass.computeIfAbsent(element, e -> new TreeMap<>(Comparator.reverseOrder()))
					.merge(mass, entry.getKey(), (a, b) -> preferred.compare(a, b) <= 0 ? a : b);
		}
		byMass.forEach((element, items) -> out.put(element, List.copyOf(items.values())));
		UNITS.forEach((element, ids) -> {
			List<Item> listed = new ArrayList<>();
			for (String id : ids) {
				Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
				if (base.containsKey(item)) {
					listed.add(item);
				}
			}
			out.put(element, listed);
		});
		return out;
	}

	/** Lingots, blocs et pépites avant les plaques, poudres et minerais. */
	private static int shapeRank(Item item) {
		String path = BuiltInRegistries.ITEM.getKey(item).getPath();
		if (path.contains("ore")) {
			return 4;
		}
		if (path.endsWith("_ingot") || path.endsWith("_nugget")) {
			return 0;
		}
		if (path.endsWith("_block") && !path.startsWith("raw_")) {
			return 1;
		}
		return path.startsWith("raw_") ? 3 : 2;
	}

	/** Les alliages ({@code data/<ns>/fmab/alloy}) : leur masse partagée entre leurs éléments. */
	private static Map<Item, Composition> alloys(ServerLevel level) {
		Map<Item, Composition> out = new HashMap<>();
		for (AlloyGroup alloy : level.registryAccess().lookupOrThrow(FmabRegistries.ALLOY)) {
			int parts = alloy.parts().values().stream().mapToInt(Integer::intValue).sum();
			alloy.values().forEach((key, mass) -> {
				Map<String, Double> masses = new HashMap<>();
				alloy.parts().forEach((element, part) -> masses.put(element, mass * (double) part / parts));
				Composition composition = new Composition(masses);
				if (key.startsWith("#")) {
					TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(key.substring(1)));
					BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(h -> out.putIfAbsent(h.value(), composition));
				} else {
					BuiltInRegistries.ITEM.getOptional(Identifier.parse(key)).ifPresent(i -> out.put(i, composition));
				}
			});
		}
		return out;
	}

	private static List<Composition.Recipe<Item>> recipes(RecipeManager manager) {
		List<Composition.Recipe<Item>> out = new ArrayList<>();
		for (RecipeHolder<?> holder : manager.getRecipes()) {
			// Toutes les recettes qui se laissent lire : établi, four, tailleur, forge, et celles des
			// autres mods qui décrivent leurs ingrédients comme le jeu.
			Recipe<?> recipe = holder.value();
			Optional<ItemStack> result = recipe.display().stream()
					.map(RecipeDisplay::result)
					.map(ItemCompositions::stackOf)
					.flatMap(Optional::stream)
					.findFirst();
			if (result.isEmpty() || recipe.placementInfo().isImpossibleToPlace()) {
				continue;
			}
			List<List<Item>> ingredients = new ArrayList<>();
			for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
				List<Item> options = ingredient.items().map(Holder::value).toList();
				if (!options.isEmpty() && options.stream().allMatch(i -> i.builtInRegistryHolder().is(CATALYSTS))) {
					continue;
				}
				ingredients.add(options);
			}
			out.add(new Composition.Recipe<>(ingredients, result.get().getItem(), result.get().getCount()));
		}
		// Un ordre stable : la même partie donne les mêmes valeurs d'un lancement à l'autre.
		out.sort(Comparator.comparing(r -> BuiltInRegistries.ITEM.getKey(r.result()).toString()));
		return out;
	}

	private static Optional<ItemStack> stackOf(SlotDisplay display) {
		if (display instanceof SlotDisplay.ItemStackSlotDisplay stack) {
			return Optional.of(stack.stack().create());
		}
		if (display instanceof SlotDisplay.ItemSlotDisplay item) {
			return Optional.of(new ItemStack(item.item()));
		}
		return Optional.empty();
	}
}
