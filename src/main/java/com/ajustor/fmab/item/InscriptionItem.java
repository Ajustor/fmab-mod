package com.ajustor.fmab.item;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabTags;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Craie, peinture alchimique ou burin : inscrit sur la face cliquée (sol, mur ou plafond) le cercle
 * sélectionné dans le Carnet de cercles du joueur.
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
		Drawing drawing = Notebooks.selected(player);
		if (drawing.isEmpty()) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Notebooks.noCircle());
			}
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide()) {
			CircleSize size = size(context.getItemInHand());
			level.setBlock(pos, state, 3);
			if (level.getBlockEntity(pos) instanceof TransmutationCircleBlockEntity circle) {
				circle.setDrawing(drawing);
				circle.setSize(size);
			}
			level.playSound(null, pos, sound(), SoundSource.BLOCKS, 1, 1.2f);
			context.getItemInHand().hurtAndBreak(size.wear(), player, context.getHand());
		}
		return InteractionResult.SUCCESS;
	}

	/** Accroupi, un clic dans le vide change la taille des cercles que l'outil trace. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		CircleSize next = size(stack).next();
		stack.set(FmabComponents.CIRCLE_SIZE, next.blocks());
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("item.fmab.circle_size", next.blocks()));
		}
		return InteractionResult.SUCCESS;
	}

	public static CircleSize size(ItemStack stack) {
		return CircleSize.ofBlocks(stack.getOrDefault(FmabComponents.CIRCLE_SIZE, CircleSize.NORMAL.blocks()));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.circle_size.tooltip", size(stack).blocks())
				.withStyle(ChatFormatting.GRAY));
	}

	private SoundEvent sound() {
		return switch (medium) {
			case CHALK -> SoundEvents.BRUSH_GENERIC;
			case PAINT -> SoundEvents.HONEY_BLOCK_PLACE;
			case ENGRAVING -> SoundEvents.STONE_HIT;
		};
	}
}
