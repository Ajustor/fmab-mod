package com.ajustor.fmab.item;

import com.ajustor.fmab.entity.KunaiEntity;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Le kunaï d'alkahestry de Xing. Clic droit : on le lance ; il se plante là où il touche. Accroupi,
 * clic droit : on change ce que dessineront les kunaï, soin ou piège.
 */
public class KunaiItem extends Item {
	public KunaiItem(Properties properties) {
		super(properties);
	}

	public static boolean trap(ItemStack stack) {
		return Boolean.TRUE.equals(stack.get(FmabComponents.KUNAI_TRAP));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) {
			boolean trap = !trap(stack);
			stack.set(FmabComponents.KUNAI_TRAP, trap);
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable(trap ? "item.fmab.kunai.mode_trap" : "item.fmab.kunai.mode_heal"));
			}
			return InteractionResult.SUCCESS;
		}
		if (level instanceof ServerLevel server) {
			KunaiEntity kunai = new KunaiEntity(server, player, stack);
			kunai.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.6f, 0.5f);
			if (player.getAbilities().instabuild) {
				kunai.pickup = KunaiEntity.Pickup.CREATIVE_ONLY;
			}
			server.addFreshEntity(kunai);
			server.playSound(null, player.blockPosition(), FmabSounds.THROW, SoundSource.PLAYERS, 0.6f,
					1.6f);
		}
		player.getCooldowns().addCooldown(stack, 6);
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable(trap(stack) ? "item.fmab.kunai.trap" : "item.fmab.kunai.heal")
				.withStyle(trap(stack) ? ChatFormatting.DARK_PURPLE : ChatFormatting.GREEN));
		tooltip.accept(Component.translatable("item.fmab.kunai.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
