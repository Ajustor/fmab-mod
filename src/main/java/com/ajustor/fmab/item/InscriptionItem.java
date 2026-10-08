package com.ajustor.fmab.item;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.registry.FmabTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
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
 * Craie, peinture alchimique ou burin : inscrit sur la face cliquée (sol, mur ou plafond) le cercle
 * sélectionné dans un Carnet de cercles porté par le joueur (main secondaire en priorité, sinon le
 * premier carnet de l'inventaire).
 */
public class InscriptionItem extends Item {
	private final CircleMedium medium;

	public InscriptionItem(CircleMedium medium, Properties properties) {
		super(properties);
		this.medium = medium;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		BlockPos support = context.getClickedPos();
		BlockPos pos = support.relative(context.getClickedFace());
		if (medium == CircleMedium.ENGRAVING && !level.getBlockState(support).is(FmabTags.ENGRAVABLE)) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("item.fmab.alchemist_chisel.not_engravable"));
			}
			return InteractionResult.FAIL;
		}
		BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
				.stateFor(context.getClickedFace(), player.getDirection(), medium);
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
			if (level.getBlockEntity(pos) instanceof TransmutationCircleBlockEntity circle) {
				circle.setDrawing(drawing);
			}
			level.playSound(null, pos, sound(), SoundSource.BLOCKS, 1, 1.2f);
			context.getItemInHand().hurtAndBreak(1, player, context.getHand());
		}
		return InteractionResult.SUCCESS;
	}

	private SoundEvent sound() {
		return switch (medium) {
			case CHALK -> SoundEvents.BRUSH_GENERIC;
			case PAINT -> SoundEvents.HONEY_BLOCK_PLACE;
			case ENGRAVING -> SoundEvents.STONE_HIT;
		};
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
