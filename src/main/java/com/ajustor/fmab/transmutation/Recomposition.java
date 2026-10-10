package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.exchange.Composition;
import com.ajustor.fmab.network.OpenDesignsPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.stone.PhilosopherStones;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Fabriquer un objet par l'alchimie, en trois temps comme toute transmutation.
 *
 * <ol>
 *   <li><b>Comprendre</b> : on décompose l'objet sur un cercle (son élément principal + Décomposer).
 *   Il se défait en sa matière, et l'alchimiste en retient la structure.</li>
 *   <li><b>Décomposer</b> la matière : celle qu'on pose sur le cercle ou qu'on porte, de chaque
 *   élément dont l'objet est fait, à hauteur de sa masse.</li>
 *   <li><b>Recomposer</b> : posé sur le cercle, un exemplaire de l'objet sert de modèle, et le cercle
 *   (son élément principal + Recomposer) en fait une copie, neuve et sans enchantement. Sans
 *   modèle, le cercle crée l'objet auquel l'alchimiste pense, choisi sur sa page de conception.</li>
 * </ol>
 *
 * <p>L'échange est équivalent, à la masse près : une pioche de fer coûte trois lingots et deux
 * bâtons de bois. On ne fabrique pas d'or (la loi l'interdit), ni les objets du mod, qui ont leurs
 * propres façons d'être faits.
 */
public final class Recomposition {
	private Recomposition() {
	}

	// --- Comprendre (décomposer un objet) ----------------------------------------------------------

	/** Élément + Décomposer : les objets posés sur le cercle se défont en leur matière. */
	public static Effects.Result dismantleEffect(EffectContext ctx) {
		return dismantle(ctx) > 0 ? Effects.Result.DONE : Effects.Result.NO_TARGET;
	}

	/**
	 * Défait les objets posés sur le cercle dont l'élément principal est visé par l'étage. Leur
	 * matière passe à l'étage suivant (ou retombe sur le cercle) ; l'alchimiste en retient la
	 * structure.
	 *
	 * @return combien d'objets se sont défaits
	 */
	public static int dismantle(EffectContext ctx) {
		ServerLevel level = ctx.level();
		Set<String> aimed = ctx.stage().combination().elements();
		int count = 0;
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, ctx.onCircle())) {
			ItemStack stack = entity.getItem();
			Optional<Composition> composition = composition(level, stack.getItem());
			if (composition.isEmpty() || !aimed.contains(composition.get().principal())) {
				continue;
			}
			if (ItemCompositions.isUnit(level, stack.getItem())) {
				// Un lingot défait redonnerait un lingot : on en comprend la matière, sans la toucher.
				understand(ctx.caster(), stack);
				continue;
			}
			Map<String, Integer> yield = new LinkedHashMap<>();
			composition.get().masses().forEach((element, mass) ->
					yield.put(element, (int) Math.floor(mass * stack.getCount() + 1e-9)));
			yield.forEach((element, mass) -> ctx.flow().addAll(ItemCompositions.units(level, element, mass)));
			understand(ctx.caster(), stack);
			count += stack.getCount();
			entity.discard();
		}
		return count;
	}

	/** L'alchimiste retient comment cet objet est fait. */
	private static void understand(ServerPlayer caster, ItemStack stack) {
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		Set<String> known = caster.getAttachedOrCreate(FmabAttachments.UNDERSTOOD);
		if (known.contains(id)) {
			return;
		}
		Set<String> more = new HashSet<>(known);
		more.add(id);
		caster.setAttached(FmabAttachments.UNDERSTOOD, Set.copyOf(more));
		caster.sendSystemMessage(Component.translatable("transmutation.fmab.understood", stack.getHoverName())
				.withStyle(ChatFormatting.DARK_AQUA));
	}

	public static boolean understands(ServerPlayer caster, Item item) {
		return caster.getAttachedOrCreate(FmabAttachments.UNDERSTOOD)
				.contains(BuiltInRegistries.ITEM.getKey(item).toString());
	}

	// --- Recomposer d'après modèle, ou d'après conception ----------------------------------------

	/** Élément + Recomposer, sans autre usage : il faut un modèle sur le cercle, ou une conception. */
	public static Effects.Result recompose(EffectContext ctx) {
		return copy(ctx).orElseGet(() -> {
			ctx.caster().sendSystemMessage(Component.translatable("transmutation.fmab.copy.no_model"));
			return Effects.Result.NO_TARGET;
		});
	}

	/**
	 * Crée l'objet posé en modèle sur le cercle, ou, à défaut, celui auquel l'alchimiste pense (sa
	 * conception). La matière qu'il contient est consommée, élément par élément ; ce qu'on pose en
	 * plus de l'élément principal sur le cercle (la moitié de sa masse au plus) rend l'objet plus
	 * solide. Avec une Pierre philosophale en main ou dans le corps, plus besoin de matière : l'objet
	 * est créé de toutes pièces.
	 *
	 * @return vide s'il n'y a ni modèle ni conception : l'étage fait alors ce qu'il fait d'ordinaire
	 */
	public static Optional<Effects.Result> copy(EffectContext ctx) {
		ServerLevel level = ctx.level();
		ServerPlayer caster = ctx.caster();
		// Un objet fabriqué posé sur le cercle passe avant la conception ; la conception passe avant
		// la matière posée, qui sert d'ordinaire à payer.
		Optional<ItemEntity> crafted = model(ctx, false);
		Optional<Item> designed = crafted.isPresent() ? Optional.empty() : design(caster);
		Optional<ItemEntity> model = crafted.isPresent() || designed.isPresent() ? crafted : model(ctx, true);
		Optional<Item> target = model.map(e -> e.getItem().getItem()).or(() -> designed);
		if (target.isEmpty()) {
			return Optional.empty();
		}
		Item item = target.get();
		ItemStack shown = new ItemStack(item);
		Optional<Composition> found = composition(level, item);
		if (found.isEmpty()) {
			return Optional.empty();
		}
		Composition composition = found.get();
		if (composition.masses().containsKey("gold")) {
			caster.sendSystemMessage(Component.translatable("transmutation.fmab.copy.gold", shown.getHoverName()));
			return Optional.of(Effects.Result.NO_TARGET);
		}
		if (!understands(caster, item)) {
			caster.sendSystemMessage(Component.translatable("transmutation.fmab.copy.not_understood",
					shown.getHoverName()));
			return Optional.of(Effects.Result.NO_TARGET);
		}
		String principal = composition.principal();
		if (!ctx.stage().combination().elements().contains(principal)) {
			caster.sendSystemMessage(Component.translatable("transmutation.fmab.copy.wrong_element",
					shown.getHoverName(), element(principal)));
			return Optional.of(Effects.Result.NO_TARGET);
		}
		if (fromNothing(caster)) {
			// La Pierre ignore l'échange équivalent : rien n'est pris, l'objet sort tel quel.
			Effects.drop(ctx, shown);
			return Optional.of(Effects.Result.DONE);
		}
		Map<String, Integer> cost = composition.cost();
		Map<String, MaterialPool> pools = new LinkedHashMap<>();
		MutableComponent missing = Component.empty();
		boolean enough = true;
		for (var entry : cost.entrySet()) {
			MaterialPool pool = MaterialPool.collect(ctx, entry.getKey(), null, true, model.orElse(null));
			pools.put(entry.getKey(), pool);
			if (pool.available() < entry.getValue()) {
				enough = false;
				if (!missing.getSiblings().isEmpty()) {
					missing.append(", ");
				}
				missing.append(Component.translatable("transmutation.fmab.copy.amount", element(entry.getKey()),
						pool.available(), entry.getValue()));
			}
		}
		if (!enough) {
			caster.sendSystemMessage(Component.translatable("transmutation.fmab.copy.missing",
					shown.getHoverName(), missing));
			return Optional.of(Effects.Result.NO_MATERIAL);
		}
		cost.forEach((element, mass) -> pools.get(element).consume(mass));
		ItemStack made = shown.copy();
		reinforce(ctx, made, principal, cost.get(principal), model.orElse(null));
		Effects.drop(ctx, made);
		return Optional.of(Effects.Result.DONE);
	}

	/** On porte ou l'on est une Pierre philosophale : la matière se crée de toutes pièces. */
	private static boolean fromNothing(ServerPlayer caster) {
		return PhilosopherStones.amplifies(caster);
	}

	/**
	 * La matière de l'élément principal posée sur le cercle en plus du prix entre dans l'objet : sa
	 * durabilité grandit d'autant, jusqu'à moitié en plus.
	 */
	private static void reinforce(EffectContext ctx, ItemStack made, String principal, int paid,
			@Nullable ItemEntity model) {
		if (!made.isDamageableItem() || paid <= 0) {
			return;
		}
		MaterialPool extra = MaterialPool.collect(ctx, principal, null, false, model);
		int added = Math.min(extra.available(), paid / 2);
		if (added <= 0 || !extra.consume(added)) {
			return;
		}
		int max = made.getMaxDamage();
		made.set(DataComponents.MAX_DAMAGE, max + (int) Math.round(max * (double) added / paid));
	}

	/** L'objet auquel l'alchimiste pense, s'il en a choisi un. */
	private static Optional<Item> design(ServerPlayer caster) {
		String id = caster.getAttached(FmabAttachments.DESIGN);
		if (id == null || id.isEmpty()) {
			return Optional.empty();
		}
		return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id));
	}

	// --- La page de conception --------------------------------------------------------------------

	/** Ce que l'alchimiste sait recomposer, avec le prix de chaque objet, pour sa page de conception. */
	public static OpenDesignsPayload designs(ServerPlayer player) {
		ServerLevel level = player.level();
		List<OpenDesignsPayload.Entry> entries = new ArrayList<>();
		for (String id : new TreeSet<>(player.getAttachedOrCreate(FmabAttachments.UNDERSTOOD))) {
			BuiltInRegistries.ITEM.getOptional(Identifier.parse(id))
					.flatMap(item -> composition(level, item))
					.ifPresent(c -> entries.add(new OpenDesignsPayload.Entry(id, c.cost())));
		}
		String current = player.getAttached(FmabAttachments.DESIGN);
		return new OpenDesignsPayload(entries, current == null ? "" : current);
	}

	/** L'alchimiste pense désormais à cet objet (ou à rien, si l'identifiant est vide). */
	public static void choose(ServerPlayer player, String id) {
		if (id.isEmpty()) {
			player.removeAttached(FmabAttachments.DESIGN);
			player.sendOverlayMessage(Component.translatable("transmutation.fmab.design.cleared"));
			return;
		}
		Optional<Item> item = BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(id));
		if (item.isEmpty() || !understands(player, item.get()) || composition(player.level(), item.get()).isEmpty()) {
			return;
		}
		player.setAttached(FmabAttachments.DESIGN, id);
		player.sendOverlayMessage(Component.translatable("transmutation.fmab.design.chosen",
				new ItemStack(item.get()).getHoverName()));
	}

	/**
	 * Le modèle : l'objet posé sur le cercle, le plus près du centre. Sans {@code matter}, seulement
	 * un objet fabriqué ; avec, une matière de l'élément visé, la plus lourde (entre un diamant et du
	 * charbon, le modèle est le diamant, le charbon le paie).
	 */
	private static Optional<ItemEntity> model(EffectContext ctx, boolean matter) {
		ServerLevel level = ctx.level();
		Vec3 center = Vec3.atCenterOf(ctx.circle());
		Set<String> aimed = ctx.stage().combination().elements();
		List<ItemEntity> candidates = level.getEntitiesOfClass(ItemEntity.class, ctx.onCircle(), e -> {
			Item item = e.getItem().getItem();
			Optional<Composition> c = composition(level, item);
			return c.isPresent() && ItemCompositions.isBase(level, item) == matter
					&& (!matter || aimed.contains(c.get().principal()));
		});
		Comparator<ItemEntity> nearest = Comparator.comparingDouble(e -> e.distanceToSqr(center));
		Comparator<ItemEntity> heaviest = Comparator.comparingDouble(
				e -> -composition(level, e.getItem().getItem()).map(Composition::total).orElse(0.0));
		return candidates.stream().min(matter ? heaviest.thenComparing(nearest) : nearest);
	}

	/**
	 * La composition d'un objet qu'on peut défaire ou recomposer : tout ce qui a une matière, sauf
	 * les objets du mod. Une matière de base se recompose aussi (un diamant à partir de charbon).
	 */
	static Optional<Composition> composition(ServerLevel level, Item item) {
		if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("fmab")) {
			return Optional.empty();
		}
		return ItemCompositions.of(level, item);
	}

	private static Component element(String element) {
		return Component.translatable("material.fmab." + element);
	}
}
