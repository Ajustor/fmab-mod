package com.ajustor.fmab.block;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.SoulSeal;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.gate.SoulArmor;
import com.ajustor.fmab.item.GloveItem;
import com.ajustor.fmab.item.InscriptionItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.registry.FmabTags;
import com.ajustor.fmab.transmutation.AlchemyRules;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;
import java.util.UUID;

/**
 * Table d'alchimiste : on y brode (au fil alchimique) ou grave (au burin) sur un gant le cercle
 * sélectionné dans le carnet. Clic droit sur la table avec le gant en main, le carnet dans l'autre
 * main ou dans l'inventaire.
 */
public class AlchemistTableBlock extends Block {
	/** Graver un gantelet use le burin plus qu'une inscription au sol. */
	private static final int CHISEL_WEAR = 4;
	/** Un vêtement ne porte qu'un cercle à un étage. */
	private static final int CLOTHES_STAGES = 1;
	/** Le sang qu'on donne pour tracer son sceau sans encre. */
	private static final float SEAL_BLOOD = 4;

	public AlchemistTableBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hitResult) {
		if (stack.is(ItemTags.CHEST_ARMOR) && !stack.is(FmabItems.SOUL_CHESTPLATE) && player.isShiftKeyDown()) {
			return seal(stack, level, pos, player);
		}
		if (stack.is(FmabTags.EMBROIDERABLE)) {
			return embroider(stack, level, pos, player);
		}
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

	/** Un vêtement ne porte qu'un étage : son cercle agit en permanence tant qu'on le porte. */
	private static InteractionResult embroider(ItemStack stack, Level level, BlockPos pos, Player player) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Drawing drawing = InscriptionItem.selectedDrawing(player);
		if (drawing.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("item.fmab.chalk.no_circle"));
			return InteractionResult.FAIL;
		}
		AlchemistData me = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		if (AlchemyRules.of(level.registryAccess()).analyze(drawing, me).parsed().stages().size() > CLOTHES_STAGES) {
			player.sendOverlayMessage(Component.translatable("transmutation.fmab.support_too_small", CLOTHES_STAGES));
			return InteractionResult.FAIL;
		}
		if (!consumeTool((ServerPlayer) player, false)) {
			player.sendOverlayMessage(Component.translatable("block.fmab.alchemist_table.needs_thread"));
			return InteractionResult.FAIL;
		}
		stack.set(FmabComponents.EMBROIDERY, drawing);
		level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
		player.sendOverlayMessage(Component.translatable("block.fmab.alchemist_table.embroidered"));
		return InteractionResult.SUCCESS;
	}

	/**
	 * Accroupi, on trace dans un plastron le sceau de sang sélectionné dans le carnet : le sien, ou
	 * celui d'un ami (comme Ed pour Al). Il se trace à l'encre alchimique ou du sang de qui le trace
	 * (une âme n'en a plus : il lui faut de l'encre). Posé sur un porte-armure, au-dessus d'un cercle
	 * d'âme, il pourra accueillir l'âme de son propriétaire.
	 */
	/**
	 * Accroupi, un plastron à la main, sur la table : on y trace son sceau. Le jeu saute le bloc quand
	 * on est accroupi avec un objet ; on passe donc par l'événement d'utilisation, qui vient avant.
	 */
	public static void registerSealing() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!player.isShiftKeyDown() || !stack.is(ItemTags.CHEST_ARMOR) || stack.is(FmabItems.SOUL_CHESTPLATE)
					|| !level.getBlockState(hit.getBlockPos()).is(FmabBlocks.ALCHEMIST_TABLE)) {
				return InteractionResult.PASS;
			}
			return seal(stack, level, hit.getBlockPos(), player);
		});
	}

	private static InteractionResult seal(ItemStack stack, Level level, BlockPos pos, Player player) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Drawing selected = InscriptionItem.selectedDrawing(player);
		Optional<UUID> owner = level.getServer().getPlayerList().getPlayers().stream()
				.map(Player::getUUID)
				.filter(id -> SoulSeal.of(id).equals(selected))
				.findFirst();
		if (owner.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("block.fmab.alchemist_table.no_seal"));
			return InteractionResult.FAIL;
		}
		ItemStack ink = ItemStack.EMPTY;
		for (ItemStack s : player.getInventory()) {
			if (s.is(FmabItems.ALCHEMICAL_INK)) {
				ink = s;
				break;
			}
		}
		boolean soul = player.getAttachedOrCreate(FmabAttachments.GATE).soulBound();
		if (player.isCreative()) {
			// Rien à payer.
		} else if (!ink.isEmpty()) {
			ink.shrink(1);
		} else if (!soul) {
			player.hurtServer((ServerLevel) level, level.damageSources().magic(), SEAL_BLOOD);
		} else {
			player.sendOverlayMessage(Component.translatable("block.fmab.alchemist_table.needs_ink"));
			return InteractionResult.FAIL;
		}
		ItemStack sealed = SoulArmor.sealed(level.registryAccess(), owner.get(), stack);
		stack.shrink(1);
		if (!player.getInventory().add(sealed)) {
			player.drop(sealed, false);
		}
		level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 0.8f);
		player.sendOverlayMessage(Component.translatable("block.fmab.alchemist_table.sealed"));
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
