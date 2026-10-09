package com.ajustor.fmab.item;

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

import java.util.function.Consumer;

/**
 * Un fragment de fresque des ruines de Xerxès : un épisode de la chute du royaume, gravé dans la
 * pierre. Clic droit pour le déchiffrer. Il y en a {@link #COUNT}.
 */
public class XerxesMuralItem extends Item {
	public static final int COUNT = 5;

	public XerxesMuralItem(Properties properties) {
		super(properties);
	}

	public static int index(ItemStack stack) {
		Integer i = stack.get(FmabComponents.MURAL);
		return i == null ? 1 : Math.clamp(i, 1, COUNT);
	}

	public static ItemStack of(Item item, int index) {
		ItemStack stack = new ItemStack(item);
		stack.set(FmabComponents.MURAL, index);
		return stack;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide()) {
			int i = index(player.getItemInHand(hand));
			player.sendSystemMessage(Component.translatable("item.fmab.xerxes_mural.title", i, COUNT)
					.withStyle(ChatFormatting.GOLD));
			player.sendSystemMessage(Component.translatable("item.fmab.xerxes_mural.text" + i)
					.withStyle(ChatFormatting.ITALIC));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.xerxes_mural.title", index(stack), COUNT)
				.withStyle(ChatFormatting.GRAY));
	}
}
