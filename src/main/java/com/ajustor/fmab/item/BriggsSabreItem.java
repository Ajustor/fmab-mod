package com.ajustor.fmab.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Le sabre de Briggs : une lame lourde, lente, forgée pour le froid du Nord. Elle mord plus fort
 * dans la chair des homonculus (voir {@code HomunculusEntity}), et ne doit rien à l'alchimie : elle
 * frappe aussi bien dans la zone de Père.
 */
public class BriggsSabreItem extends Item {
	/** Multiplicateur des dégâts portés à un homonculus. */
	public static final float HOMUNCULUS_BONUS = 1.5f;

	public BriggsSabreItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.briggs_sabre.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
