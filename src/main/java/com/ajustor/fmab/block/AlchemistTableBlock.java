package com.ajustor.fmab.block;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.item.GloveItem;
import com.ajustor.fmab.item.InscriptionItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.AlchemyRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Table d'alchimiste : on y brode (au fil alchimique) ou grave (au burin) sur un gant le cercle
 * sélectionné dans le carnet. Clic droit sur la table avec le gant en main, le carnet dans l'autre
 * main ou dans l'inventaire.
 */
public class AlchemistTableBlock extends Block {
	/** Graver un gantelet use le burin plus qu'une inscription au sol. */
	private static final int CHISEL_WEAR = 4;

	public AlchemistTableBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hitResult) {
		if (!(stack.getItem() instanceof GloveItem glove)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Drawing drawing = InscriptionItem.selectedDrawing(player);
		if (drawing.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("item.fmab.chalk.no_circle"));
			return InteractionResult.FAIL;
		}
		AlchemistData me = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		int stages = AlchemyRules.of(level.registryAccess()).analyze(drawing, me).parsed().stages().size();
		if (stages > glove.kind().stages()) {
			player.sendOverlayMessage(Component.translatable("transmutation.fmab.support_too_small",
					glove.kind().stages()));
			return InteractionResult.FAIL;
		}
		if (!consumeTool((ServerPlayer) player, glove.kind().engraved())) {
			player.sendOverlayMessage(Component.translatable(glove.kind().engraved()
					? "block.fmab.alchemist_table.needs_chisel" : "block.fmab.alchemist_table.needs_thread"));
			return InteractionResult.FAIL;
		}
		stack.set(FmabComponents.GLOVE_CIRCLE, drawing);
		level.playSound(null, pos, glove.kind().engraved() ? SoundEvents.ANVIL_USE : SoundEvents.WOOL_PLACE,
				SoundSource.BLOCKS, 0.8f, 1.2f);
		player.sendOverlayMessage(Component.translatable("block.fmab.alchemist_table.done"));
		return InteractionResult.SUCCESS;
	}

	/** Le fil se consomme ; le burin s'use. */
	private static boolean consumeTool(ServerPlayer player, boolean engraved) {
		for (ItemStack tool : player.getInventory()) {
			if (engraved && tool.is(FmabItems.ALCHEMIST_CHISEL)) {
				if (!player.isCreative()) {
					tool.hurtAndBreak(CHISEL_WEAR, player.level(), player, item -> {
					});
				}
				return true;
			}
			if (!engraved && tool.is(FmabItems.ALCHEMICAL_THREAD)) {
				if (!player.isCreative()) {
					tool.shrink(1);
				}
				return true;
			}
		}
		return false;
	}
}
