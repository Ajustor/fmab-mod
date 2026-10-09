package com.ajustor.fmab.block;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.promised.NationalCircle;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.stone.Eclipse;
import com.ajustor.fmab.xing.Alkahestry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Un point de sang du cercle national, là où un massacre a eu lieu. Indestructible ; on le scelle
 * par un contre-cercle : un kunaï d'alkahestry planté au cœur (il faut connaître l'alkahestry), ou
 * un contre-cercle gravé au burin par un Alchimiste d'État. Pendant l'éclipse, les points encore
 * actifs crachent une colonne de sang vers le ciel.
 */
public class BloodCrestBlock extends Block {
	public static final BooleanProperty SEALED = BlockStateProperties.LOCKED;
	private static final DustParticleOptions BLOOD = new DustParticleOptions(0xB0101A, 1.4f);

	public BloodCrestBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(SEALED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SEALED);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		boolean kunai = stack.is(FmabItems.KUNAI);
		boolean chisel = stack.is(FmabItems.ALCHEMIST_CHISEL);
		if (state.getValue(SEALED) || !kunai && !chisel) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer p)) {
			return InteractionResult.SUCCESS;
		}
		if (kunai && !Alkahestry.knows(p) && !p.isCreative()) {
			p.sendOverlayMessage(Component.translatable("alkahestry.fmab.unknown"));
			return InteractionResult.SUCCESS;
		}
		if (chisel && !p.getAttachedOrCreate(FmabAttachments.ALCHEMIST).rank().atLeast(Rank.STATE) && !p.isCreative()) {
			p.sendOverlayMessage(Component.translatable("block.fmab.blood_crest.needs_state"));
			return InteractionResult.SUCCESS;
		}
		if (kunai) {
			stack.consume(1, p);
		} else {
			stack.hurtAndBreak(8, p, hand);
		}
		server.setBlockAndUpdate(pos, state.setValue(SEALED, true));
		server.sendParticles(kunai ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ENCHANT, pos.getX() + 0.5,
				pos.getY() + 1, pos.getZ() + 0.5, 40, 0.6, 0.4, 0.6, 0.05);
		server.sendParticles(BLOOD, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 60, 0.8, 0.6, 0.8, 0);
		server.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.5f, 0.7f);
		p.sendSystemMessage(Component.translatable("block.fmab.blood_crest.sealed"));
		NationalCircle.get(server.getServer()).seal(server.getServer(), pos, p);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable(state.getValue(SEALED)
					? "block.fmab.blood_crest.dormant" : "block.fmab.blood_crest.hint"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(SEALED)) {
			return;
		}
		double x = pos.getX() + 0.5, y = pos.getY() + 1.05, z = pos.getZ() + 0.5;
		level.addParticle(BLOOD, x + random.nextGaussian() * 0.3, y, z + random.nextGaussian() * 0.3, 0, 0.02, 0);
		if (Eclipse.now(level)) {
			// Le Jour promis : une colonne de sang monte vers le soleil noir.
			for (int i = 0; i < 12; i++) {
				level.addParticle(BLOOD, x + random.nextGaussian() * 0.15, y + random.nextDouble() * 24,
						z + random.nextGaussian() * 0.15, 0, 0.3, 0);
			}
		}
	}
}
