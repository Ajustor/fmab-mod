package com.ajustor.fmab.item;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.network.FmabNetwork;
import com.ajustor.fmab.network.OpenTattooPayload;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * sélectionné dans le Carnet de cercles du joueur. Le burin, sur un cercle déjà tracé, ouvre le rituel
 * de gravure d'un bras d'automail.
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
		// Le burin sur un cercle tracé : le rituel de gravure, pour un bras d'automail.
		if (medium == CircleMedium.ENGRAVING
				&& level.getBlockEntity(support) instanceof TransmutationCircleBlockEntity traced
				&& !traced.drawing().isEmpty()) {
			if (player instanceof ServerPlayer server) {
				FmabNetwork.send(server, OpenTattooPayload.of(server, support, true));
			}
			return InteractionResult.SUCCESS;
		}
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
			player.sendOverlayMessage(next.great() ? Component.translatable("item.fmab.circle_size.great")
					: Component.translatable("item.fmab.circle_size", next.blocks()));
		}
		return InteractionResult.SUCCESS;
	}

	public static CircleSize size(ItemStack stack) {
		return CircleSize.ofBlocks(stack.getOrDefault(FmabComponents.CIRCLE_SIZE, CircleSize.NORMAL.blocks()));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		CircleSize size = size(stack);
		tooltip.accept((size.great() ? Component.translatable("item.fmab.circle_size.great.tooltip")
				: Component.translatable("item.fmab.circle_size.tooltip", size.blocks())).withStyle(ChatFormatting.GRAY));
	}

	private SoundEvent sound() {
		return switch (medium) {
			case CHALK -> SoundEvents.BRUSH_GENERIC;
			case PAINT -> SoundEvents.HONEY_BLOCK_PLACE;
			case ENGRAVING -> SoundEvents.STONE_HIT;
		};
	}
}
