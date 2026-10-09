package com.ajustor.fmab.block;

import com.ajustor.fmab.progress.Milestones;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Le sceau d'Ouroboros qui ferme le chemin de Père, au fond du tunnel de Sloth. Indestructible : il
 * ne s'ouvre que devant qui a vu tomber les trois gardiens de Central, Sloth, Wrath et Pride.
 */
public class FatherSealBlock extends Block {
	/** Les gardiens à abattre, par identifiant de type d'entité. */
	public static final List<String> GUARDIANS = List.of("sloth", "wrath", "pride");
	private static final DustParticleOptions RED = new DustParticleOptions(0xB0101A, 1.2f);
	private static final int MAX_SPREAD = 32;

	public FatherSealBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer p)) {
			return InteractionResult.SUCCESS;
		}
		Set<String> slain = p.getAttachedOrCreate(FmabAttachments.SLAIN);
		List<Component> missing = new ArrayList<>();
		for (String guardian : GUARDIANS) {
			if (!slain.contains(guardian)) {
				missing.add(Component.translatable("entity.fmab." + guardian));
			}
		}
		if (!missing.isEmpty() && !p.isCreative()) {
			p.sendSystemMessage(Component.translatable("block.fmab.father_seal.holds",
					ComponentUtils.formatList(missing, Component.literal(", "))));
			server.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1, 0.6f);
			return InteractionResult.SUCCESS;
		}
		open(server, pos);
		Milestones.reach(p, "seal_opened");
		p.sendSystemMessage(Component.translatable("block.fmab.father_seal.opens"));
		return InteractionResult.SUCCESS;
	}

	/** Le sceau se dissout, tout entier : chaque bloc de sceau qui touche celui-ci. */
	private static void open(ServerLevel level, BlockPos from) {
		Deque<BlockPos> todo = new ArrayDeque<>(List.of(from));
		Set<BlockPos> seen = new HashSet<>();
		while (!todo.isEmpty() && seen.size() < MAX_SPREAD) {
			BlockPos at = todo.poll();
			if (!seen.add(at) || !level.getBlockState(at).is(FmabBlocks.FATHER_SEAL)) {
				continue;
			}
			level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
			level.sendParticles(RED, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 25, 0.4, 0.4, 0.4, 0);
			for (Direction d : Direction.values()) {
				todo.add(at.relative(d));
			}
		}
		level.playSound(null, from, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.5f, 0.5f);
	}
}
