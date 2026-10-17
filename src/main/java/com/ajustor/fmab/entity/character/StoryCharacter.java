package com.ajustor.fmab.entity.character;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.function.Supplier;

/**
 * Un personnage de l'histoire, tel que le joueur le rencontre : ce qu'il dit (une réplique à chaque
 * rencontre, de {@code npc.fmab.<id>.line1} à la dernière) et ce qu'il vend, à la façon d'un
 * villageois. Les objets ne sont cherchés qu'à l'ouverture de la boutique : le catalogue ne dépend
 * pas de l'ordre d'enregistrement des objets.
 *
 * @param id     l'identifiant de l'entité ({@code fmab:<id>}), de sa peau et de ses textes
 * @param lines  le nombre de répliques
 * @param trades ce qu'il vend ; vide s'il ne fait que parler
 */
public record StoryCharacter(String id, int lines, List<Trade> trades) {
	/** Combien de fois chaque article se vend avant de manquer (il revient chaque jour). */
	static final int STOCK = 12;

	/** Un article : son prix (une ou deux sortes d'objets) et ce qu'on reçoit. */
	public record Trade(Supplier<ItemLike> cost, int count, Supplier<ItemStack> result) {
		MerchantOffer offer() {
			return new MerchantOffer(new ItemCost(cost.get(), count), result.get(), STOCK, 0, 0.05f);
		}
	}

	public static Trade sells(Supplier<ItemStack> result, Supplier<ItemLike> cost, int count) {
		return new Trade(cost, count, result);
	}

	public MerchantOffers offers() {
		MerchantOffers out = new MerchantOffers();
		trades.forEach(t -> out.add(t.offer()));
		return out;
	}

	public String lineKey(int line) {
		return "npc.fmab." + id + ".line" + (line + 1);
	}
}
