package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.exchange.Composition;
import com.ajustor.fmab.alchemy.exchange.ExchangeValue;
import com.ajustor.fmab.registry.FmabTags;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * La composition de chaque objet du jeu, tirée des tables d'échange (pour les matières de base) et
 * des recettes de fabrication, de taille de pierre et de cuisson (pour le reste). Calculée une fois
 * par jeu de recettes, à la première transmutation qui en a besoin.
 */
public final class ItemCompositions {
	/** Les éléments, dans l'ordre où l'on range une matière qui en porterait plusieurs. */
	public static final List<String> ELEMENTS = List.of("iron", "copper", "gold", "earth", "water", "wood");
	/** Profondeur des recettes : un objet fait d'objets faits d'objets… */
	private static final int PASSES = 12;
	/**
	 * Ce que rend une décomposition, élément par élément : les matières de base, de la plus lourde à
	 * la plus légère (on rend un lingot plutôt que neuf pépites).
	 */
	private static final Map<String, List<String>> UNITS = Map.of(
			"iron", List.of("minecraft:iron_block", "minecraft:iron_ingot", "minecraft:iron_nugget"),
			"copper", List.of("minecraft:copper_block", "minecraft:copper_ingot"),
			"gold", List.of("minecraft:gold_block", "minecraft:gold_ingot", "minecraft:gold_nugget"),
			"earth", List.of("minecraft:cobblestone"),
			"water", List.of("minecraft:packed_ice", "minecraft:ice"),
			"wood", List.of("minecraft:oak_log", "minecraft:oak_planks"));

	private static @Nullable RecipeManager cachedFor;
	private static Map<Item, Composition> cache = Map.of();
	private static Map<Item, Composition> base = Map.of();

	private ItemCompositions() {
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
	public static Optional<String> elementOf(ItemStack stack) {
		return ELEMENTS.stream().filter(e -> stack.is(FmabTags.elementItems(e))).findFirst();
	}

	/**
	 * La matière qu'une décomposition rend pour cette masse d'un élément : les plus grosses pièces
	 * d'abord. Ce qui reste sous la plus petite pièce se perd.
	 */
	public static List<ItemStack> units(ServerLevel level, String element, int mass) {
		List<ItemStack> out = new ArrayList<>();
		int remaining = mass;
		for (String id : UNITS.getOrDefault(element, List.of())) {
			Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
			Composition unit = base.get(item);
			if (unit == null) {
				continue;
			}
			int unitMass = (int) Math.round(unit.total());
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

	private static synchronized Map<Item, Composition> all(ServerLevel level) {
		MinecraftServer server = level.getServer();
		RecipeManager recipes = server.getRecipeManager();
		if (recipes != cachedFor) {
			base = bases(level);
			cache = Composition.compute(base, recipes(recipes), PASSES);
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

	private static List<Composition.Recipe<Item>> recipes(RecipeManager manager) {
		List<Composition.Recipe<Item>> out = new ArrayList<>();
		for (RecipeHolder<?> holder : manager.getRecipes()) {
			Recipe<?> recipe = holder.value();
			if (!(recipe instanceof NormalCraftingRecipe || recipe instanceof StonecutterRecipe
					|| recipe instanceof AbstractCookingRecipe)) {
				continue;
			}
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
				ingredients.add(ingredient.items().map(Holder::value).toList());
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
