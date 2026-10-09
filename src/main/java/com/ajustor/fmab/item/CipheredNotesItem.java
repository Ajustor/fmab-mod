package com.ajustor.fmab.item;

import com.ajustor.fmab.data.Tome;
import com.ajustor.fmab.registry.FmabComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/**
 * Les notes de recherche chiffrées de Marcoh : en apparence, un livre de recettes de cuisine. Lues
 * telles quelles, elles ne disent que des proportions de farine et de beurre ; Marcoh seul les rend en
 * clair, et elles deviennent un tome d'alchimie (lequel : inscrit dans les notes, à leur création).
 */
public class CipheredNotesItem extends Item {
	/** Ce que les notes peuvent cacher. */
	private static final List<Tome> SECRETS = List.of(Tomes.FORMS, Tomes.FLAME, Tomes.GOLD, Tomes.HOHENHEIM);
	private static final int RECIPES = 4;

	public CipheredNotesItem(Properties properties) {
		super(properties);
	}

	/** Des notes qui cachent le tome numéro {@code secret}. */
	public static ItemStack of(Item item, int secret) {
		ItemStack stack = new ItemStack(item);
		stack.set(FmabComponents.CIPHER, Math.floorMod(secret, SECRETS.size()));
		return stack;
	}

	/** Le tome caché dans ces notes, en clair. */
	public static ItemStack decipher(ItemStack notes) {
		Integer secret = notes.get(FmabComponents.CIPHER);
		return Tomes.stack(SECRETS.get(secret == null ? 0 : Math.floorMod(secret, SECRETS.size())));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide()) {
			Integer secret = player.getItemInHand(hand).get(FmabComponents.CIPHER);
			int recipe = 1 + Math.floorMod(secret == null ? 0 : secret, RECIPES);
			player.sendSystemMessage(Component.translatable("item.fmab.ciphered_notes.recipe" + recipe)
					.withStyle(ChatFormatting.ITALIC));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.ciphered_notes.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
