package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.exchange.Composition;
import com.ajustor.fmab.registry.FmabAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Fabriquer un objet par l'alchimie, en trois temps comme toute transmutation.
 *
 * <ol>
 *   <li><b>Comprendre</b> : on décompose l'objet sur un cercle (son élément principal + Décomposer).
 *   Il se défait en sa matière, et l'alchimiste en retient la structure.</li>
 *   <li><b>Décomposer</b> la matière : celle qu'on pose sur le cercle ou qu'on porte, de chaque
 *   élément dont l'objet est fait, à hauteur de sa masse.</li>
 *   <li><b>Recomposer</b> : posé sur le cercle, un exemplaire de l'objet sert de modèle, et le cercle
 *   (son élément principal + Recomposer) en fait une copie, neuve et sans enchantement.</li>
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

	// --- Recomposer d'après modèle -----------------------------------------------------------------

	/** Élément + Recomposer, sans autre usage : il faut un modèle sur le cercle. */
	public static Effects.Result recompose(EffectContext ctx) {
		return copy(ctx).orElseGet(() -> {
			ctx.caster().sendSystemMessage(Component.translatable("transmutation.fmab.copy.no_model"));
			return Effects.Result.NO_TARGET;
		});
	}

	/**
	 * Copie l'objet posé en modèle sur le cercle, s'il y en a un.
	 *
	 * @return vide s'il n'y a pas de modèle : l'étage fait alors ce qu'il fait d'ordinaire
	 */
	public static Optional<Effects.Result> copy(EffectContext ctx) {
		ServerLevel level = ctx.level();
		Optional<ItemEntity> found = model(ctx);
		if (found.isEmpty()) {
			return Optional.empty();
		}
		ItemStack model = found.get().getItem();
		Composition composition = composition(level, model.getItem()).orElseThrow();
		ServerPlayer caster = ctx.caster();
		if (composition.masses().containsKey("gold")) {
			caster.sendSystemMessage(Component.translatable("transmutation.fmab.copy.gold", model.getHoverName()));
			return Optional.of(Effects.Result.NO_TARGET);
		}
		if (!understands(caster, model.getItem())) {
			caster.sendSystemMessage(Component.translatable("transmutation.fmab.copy.not_understood",
					model.getHoverName()));
			return Optional.of(Effects.Result.NO_TARGET);
		}
		String principal = composition.principal();
		if (!ctx.stage().combination().elements().contains(principal)) {
			caster.sendSystemMessage(Component.translatable("transmutation.fmab.copy.wrong_element",
					model.getHoverName(), element(principal)));
			return Optional.of(Effects.Result.NO_TARGET);
		}
		Map<String, Integer> cost = composition.cost();
		Map<String, MaterialPool> pools = new LinkedHashMap<>();
		MutableComponent missing = Component.empty();
		boolean enough = true;
		for (var entry : cost.entrySet()) {
			MaterialPool pool = MaterialPool.collect(ctx, entry.getKey(), null);
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
					model.getHoverName(), missing));
			return Optional.of(Effects.Result.NO_MATERIAL);
		}
		cost.forEach((element, mass) -> pools.get(element).consume(mass));
		Effects.drop(ctx, new ItemStack(model.getItem()));
		return Optional.of(Effects.Result.DONE);
	}

	/** Le modèle : l'objet composé posé sur le cercle, le plus près du centre. */
	private static Optional<ItemEntity> model(EffectContext ctx) {
		ServerLevel level = ctx.level();
		Vec3 center = Vec3.atCenterOf(ctx.circle());
		List<ItemEntity> candidates = level.getEntitiesOfClass(ItemEntity.class, ctx.onCircle(),
				e -> composition(level, e.getItem().getItem()).isPresent());
		return candidates.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
	}

	/**
	 * La composition d'un objet qu'on peut défaire ou recomposer : ni matière de base, ni objet du
	 * mod.
	 */
	static Optional<Composition> composition(ServerLevel level, Item item) {
		if (ItemCompositions.isBase(level, item)
				|| BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("fmab")) {
			return Optional.empty();
		}
		return ItemCompositions.of(level, item);
	}

	private static Component element(String element) {
		return Component.translatable("material.fmab." + element);
	}
}
