package com.ajustor.fmab.homunculus;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.entity.EnvyEntity;
import com.ajustor.fmab.gate.GateOfTruth;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Envy erre dans le monde : de temps à autre, il apparaît non loin d'un joueur, sous les traits de
 * quelqu'un d'autre. Jamais plus d'un Envy autour d'un même joueur.
 */
public final class EnvySpawner {
	/** Toutes les cinq minutes, une chance sur six par joueur. */
	private static final int PERIOD = 20 * 60 * 5;
	private static final int CHANCE = 6;
	private static final int MIN_DISTANCE = 24;
	private static final int MAX_DISTANCE = 40;
	/** Pas un second Envy dans ce rayon. */
	private static final int ALONE = 160;

	private EnvySpawner() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % PERIOD != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tryNear(server, player);
			}
		});
	}

	private static void tryNear(MinecraftServer server, ServerPlayer player) {
		ServerLevel level = player.level();
		RandomSource random = level.getRandom();
		if (!player.getAttachedOrCreate(FmabAttachments.ALCHEMIST).rank().atLeast(Rank.ALCHEMIST)
				|| player.isCreative() || player.isSpectator() || level.dimension() == GateOfTruth.WHITE_SPACE
				|| level.dimension() == Belly.BELLY || random.nextInt(CHANCE) != 0) {
			return;
		}
		if (!level.getEntitiesOfClass(EnvyEntity.class, player.getBoundingBox().inflate(ALONE)).isEmpty()) {
			return;
		}
		double angle = random.nextDouble() * Math.PI * 2;
		int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE);
		int x = player.getBlockX() + (int) (Math.cos(angle) * distance);
		int z = player.getBlockZ() + (int) (Math.sin(angle) * distance);
		BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
		if (!level.getBlockState(at.below()).isSolid() || !level.getFluidState(at).isEmpty()) {
			return;
		}
		EnvyEntity envy = FmabEntities.ENVY.create(level, EntitySpawnReason.EVENT);
		if (envy == null) {
			return;
		}
		envy.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
		disguise(envy, server, player, random);
		level.addFreshEntity(envy);
	}

	/**
	 * Le déguisement : un autre joueur s'il y en a, parfois le joueur lui-même (de quoi douter de
	 * son reflet), sinon un habitant d'Amestris.
	 */
	public static void disguise(EnvyEntity envy, MinecraftServer server, ServerPlayer near, RandomSource random) {
		List<ServerPlayer> others = server.getPlayerList().getPlayers().stream().filter(p -> p != near).toList();
		int roll = random.nextInt(10);
		if (!others.isEmpty() && roll < 5) {
			envy.disguiseAs(others.get(random.nextInt(others.size())));
		} else if (roll < 7) {
			envy.disguiseAs(near);
		} else {
			envy.disguiseAs(EnvyEntity.CITIZENS.get(random.nextInt(EnvyEntity.CITIZENS.size())));
		}
	}
}
