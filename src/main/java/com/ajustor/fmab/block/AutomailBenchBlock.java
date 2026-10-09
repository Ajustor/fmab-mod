package com.ajustor.fmab.block;

import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.item.AutomailItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Établi d'automail : on y pose soi-même un bras ou une jambe sur un membre perdu. Pour réparer,
 * il faut Winry.
 */
public class AutomailBenchBlock extends Block {
	public AutomailBenchBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hitResult) {
		if (!(stack.getItem() instanceof AutomailItem)) {
			ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND
					: InteractionHand.MAIN_HAND);
			if (other.getItem() instanceof AutomailItem) {
				// L'autre main tient la pièce : on la pose.
				if (player instanceof ServerPlayer server) {
					Automails.fit(server, other);
				}
				return InteractionResult.SUCCESS;
			}
			if (player instanceof ServerPlayer server) {
				server.sendOverlayMessage(Component.translatable("block.fmab.automail_bench.hint"));
			}
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer server) {
			Automails.fit(server, stack);
		}
		return InteractionResult.SUCCESS;
	}
}
