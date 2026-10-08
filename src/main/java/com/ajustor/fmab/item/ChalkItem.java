package com.ajustor.fmab.item;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.block.ChalkCircleBlock;
import com.ajustor.fmab.block.ChalkCircleBlockEntity;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Craie de transmutation : trace au sol le cercle sélectionné dans un Carnet de cercles porté par
 * le joueur (main secondaire en priorité, sinon le premier carnet de l'inventaire).
 */
public class ChalkItem extends Item {
	public ChalkItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getClickedFace() != Direction.UP) {
			return InteractionResult.PASS;
		}
		Level level = context.getLevel();
		Player player = context.getPlayer();
		BlockPos pos = context.getClickedPos().above();
		if (player == null) {
			return InteractionResult.PASS;
		}
		BlockState state = FmabBlocks.CHALK_CIRCLE.defaultBlockState()
				.setValue(ChalkCircleBlock.FACING, player.getDirection());
		if (!level.getBlockState(pos).canBeReplaced() || !state.canSurvive(level, pos)) {
			return InteractionResult.FAIL;
		}
		Drawing drawing = selectedDrawing(player);
		if (drawing.isEmpty()) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("item.fmab.chalk.no_circle"));
			}
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state, 3);
			if (level.getBlockEntity(pos) instanceof ChalkCircleBlockEntity circle) {
				circle.setDrawing(drawing);
			}
			level.playSound(null, pos, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 1, 1.2f);
			context.getItemInHand().hurtAndBreak(1, player, context.getHand());
		}
		return InteractionResult.SUCCESS;
	}

	public static Drawing selectedDrawing(Player player) {
		ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
		if (offhand.is(FmabItems.CIRCLE_NOTEBOOK)) {
			return offhand.getOrDefault(FmabComponents.NOTEBOOK, NotebookContents.EMPTY).selectedDrawing();
		}
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(FmabItems.CIRCLE_NOTEBOOK)) {
				Drawing d = stack.getOrDefault(FmabComponents.NOTEBOOK, NotebookContents.EMPTY).selectedDrawing();
				if (!d.isEmpty()) {
					return d;
				}
			}
		}
		return Drawing.EMPTY;
	}
}
