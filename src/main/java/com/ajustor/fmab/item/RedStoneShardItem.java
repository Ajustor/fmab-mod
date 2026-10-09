package com.ajustor.fmab.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Un éclat de pierre rouge, comme celle de Cornello à Liore : une Pierre philosophale imparfaite.
 * Tenue en main, elle amplifie les transmutations comme une vraie Pierre, au prix de ses quelques
 * âmes, mais elle ne tient rien : le cercle rebondit comme sans elle. Elle ne rend pas les corps.
 */
public class RedStoneShardItem extends Item {
	public RedStoneShardItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.philosopher_stone.souls", stack.getMaxDamage() - stack.getDamageValue())
				.withStyle(ChatFormatting.RED));
		tooltip.accept(Component.translatable("item.fmab.red_stone_shard.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
