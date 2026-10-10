package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.exchange.ExchangeValue;
import com.ajustor.fmab.alchemy.exchange.Family;
import com.ajustor.fmab.data.ExchangeGroup;
import com.ajustor.fmab.registry.FmabRegistries;
import com.ajustor.fmab.registry.FmabTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Matière disponible pour l'étape de décomposition : ce qu'a produit l'étage précédent, puis les
 * objets posés sur le cercle, puis l'inventaire de l'alchimiste.
 */
public final class MaterialPool {
	private record Source(ItemStack stack, int unitMass) {
	}

	private final List<Source> sources;

	private MaterialPool(List<Source> sources) {
		this.sources = sources;
	}

	/**
	 * Les objets de l'élément donné (tag {@code fmab:element/<element>}), triés du plus léger au
	 * plus lourd pour gaspiller le moins de masse possible.
	 *
	 * @param family famille exigée ; {@code null} pour un combustible, qui brûle sans être
	 *               transmuté (il compte alors pour sa valeur d'échange, ou 1)
	 */
	public static MaterialPool collect(EffectContext ctx, String element, Family family) {
		return collect(ctx, element, family, true);
	}

	/**
	 * Comme {@link #collect(EffectContext, String, Family)}, en laissant de côté l'inventaire si
	 * {@code fromInventory} est faux : seulement ce qui vient de l'étage précédent ou du cercle.
	 */
	public static MaterialPool collect(EffectContext ctx, String element, Family family, boolean fromInventory) {
		ServerLevel level = ctx.level();
		TagKey<Item> tag = FmabTags.elementItems(element);
		List<Source> sources = new ArrayList<>();
		for (ItemStack stack : ctx.flow()) {
			add(level, stack, tag, family, sources);
		}
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, ctx.onCircle())) {
			add(level, entity.getItem(), tag, family, sources);
		}
		if (fromInventory) {
			for (ItemStack stack : ctx.caster().getInventory()) {
				add(level, stack, tag, family, sources);
			}
		}
		sources.sort(Comparator.comparingInt(Source::unitMass));
		return new MaterialPool(sources);
	}

	private static void add(ServerLevel level, ItemStack stack, TagKey<Item> tag, Family family, List<Source> out) {
		if (stack.isEmpty() || !stack.is(tag)) {
			return;
		}
		Optional<ExchangeValue> value = valueOf(level, stack);
		if (family == null) {
			out.add(new Source(stack, value.map(ExchangeValue::mass).orElse(1)));
		} else {
			value.filter(v -> v.family() == family).ifPresent(v -> out.add(new Source(stack, v.mass())));
		}
	}

	public int available() {
		int total = 0;
		for (Source s : sources) {
			total += s.unitMass * s.stack.getCount();
		}
		return total;
	}

	/**
	 * Consomme au moins {@code mass}. La masse rendue en trop est perdue : l'échange équivalent
	 * interdit d'en produire plus qu'on en donne, pas d'en donner plus qu'il n'en faut.
	 *
	 * @return faux, sans rien consommer, s'il n'y a pas assez de matière
	 */
	public boolean consume(int mass) {
		if (available() < mass) {
			return false;
		}
		int remaining = mass;
		for (Source s : sources) {
			while (remaining > 0 && !s.stack.isEmpty()) {
				s.stack.shrink(1);
				remaining -= s.unitMass;
			}
			if (remaining <= 0) {
				return true;
			}
		}
		return remaining <= 0;
	}

	/** Valeur d'échange d'un objet : son identifiant d'abord, puis ses tags. */
	public static Optional<ExchangeValue> valueOf(ServerLevel level, ItemStack stack) {
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		for (ExchangeGroup group : level.registryAccess().lookupOrThrow(FmabRegistries.EXCHANGE)) {
			Integer direct = group.values().get(id.toString());
			if (direct != null) {
				return Optional.of(new ExchangeValue(group.family(), direct));
			}
		}
		for (ExchangeGroup group : level.registryAccess().lookupOrThrow(FmabRegistries.EXCHANGE)) {
			for (var entry : group.values().entrySet()) {
				if (entry.getKey().startsWith("#")) {
					TagKey<Item> tag = TagKey.create(Registries.ITEM,
							Identifier.parse(entry.getKey().substring(1)));
					if (stack.is(tag)) {
						return Optional.of(new ExchangeValue(group.family(), entry.getValue()));
					}
				}
			}
		}
		return Optional.empty();
	}
}
