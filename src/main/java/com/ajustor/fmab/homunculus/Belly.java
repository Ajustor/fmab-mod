package com.ajustor.fmab.homunculus;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Le Ventre de Gluttony : un vide sombre, noyé de sang, où dérivent les restes de ce qu'il a
 * dévoré. On en sort en tuant ce qui y rôde, en ouvrant une Porte par transmutation (comme Ed), ou
 * en tenant jusqu'à ce que Gluttony recrache.
 */
public final class Belly {
	public static final ResourceKey<Level> BELLY = ResourceKey.create(Registries.DIMENSION, Fmab.id("gluttony_belly"));

	/** Gluttony recrache au bout de deux minutes. */
	private static final int SPIT_OUT = 20 * 120;
	private static final int SPACING = 64;
	private static final int SLOTS = 1024;
	private static final int CREATURES = 3;

	/**
	 * Un joueur avalé.
	 *
	 * @param dimension où le recracher
	 * @param origin    où il a été avalé
	 * @param ticks     temps passé dans le Ventre ; négatif tant qu'il n'y est pas
	 * @param creatures les restes dévorés qui rôdent autour de lui
	 */
	public record Swallowed(String dimension, BlockPos origin, int ticks, List<UUID> creatures) {
		public static final Codec<Swallowed> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("dimension").forGetter(Swallowed::dimension),
				BlockPos.CODEC.fieldOf("origin").forGetter(Swallowed::origin),
				Codec.INT.fieldOf("ticks").forGetter(Swallowed::ticks),
				UUIDUtil.STRING_CODEC.listOf().fieldOf("creatures").forGetter(Swallowed::creatures)
		).apply(i, Swallowed::new));

		public Swallowed {
			creatures = List.copyOf(creatures);
		}
	}

	private Belly() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Swallowed s = player.getAttached(FmabAttachments.SWALLOWED);
				if (s != null) {
					tick(server, player, s);
				} else if (player.level().dimension() == BELLY && player.tickCount % 20 == 0) {
					player.teleport(new TeleportTransition(server.overworld(),
							Vec3.atBottomCenterOf(server.overworld().getRespawnData().pos()), Vec3.ZERO, 0, 0,
							TeleportTransition.DO_NOTHING));
				}
			}
		});
	}

	public static boolean swallowed(ServerPlayer player) {
		return player.getAttached(FmabAttachments.SWALLOWED) != null;
	}

	/** Gluttony avale le joueur. */
	public static void swallow(ServerPlayer player, Entity gluttony) {
		if (swallowed(player)) {
			return;
		}
		player.setAttached(FmabAttachments.SWALLOWED, new Swallowed(player.level().dimension().identifier().toString(),
				gluttony.blockPosition(), -1, List.of()));
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.HOSTILE, 1.5f, 0.5f);
		player.sendSystemMessage(Component.translatable("homunculus.fmab.swallowed"));
	}

	/** Une transmutation réussie dans le Ventre ouvre une Porte vers dehors. */
	public static void transmuted(ServerPlayer player) {
		if (swallowed(player) && player.level().dimension() == BELLY) {
			player.sendSystemMessage(Component.translatable("homunculus.fmab.belly_gate"));
			release(player.level().getServer(), player);
		}
	}

	private static BlockPos spot(ServerPlayer player) {
		return new BlockPos(Math.floorMod(player.getUUID().hashCode(), SLOTS) * SPACING, 1, 0);
	}

	private static void tick(MinecraftServer server, ServerPlayer player, Swallowed s) {
		ServerLevel belly = server.getLevel(BELLY);
		if (belly == null) {
			player.removeAttached(FmabAttachments.SWALLOWED);
			return;
		}
		if (s.ticks() < 0 || player.level() != belly) {
			arrive(belly, player, s);
			return;
		}
		Swallowed now = new Swallowed(s.dimension(), s.origin(), s.ticks() + 1, s.creatures());
		player.setAttached(FmabAttachments.SWALLOWED, now);
		if (now.ticks() % 20 == 0 && !now.creatures().isEmpty()
				&& now.creatures().stream().map(belly::getEntity).noneMatch(e -> e != null && e.isAlive())) {
			player.sendSystemMessage(Component.translatable("homunculus.fmab.belly_cleared"));
			release(server, player);
			return;
		}
		if (now.ticks() >= SPIT_OUT) {
			player.sendSystemMessage(Component.translatable("homunculus.fmab.spat_out"));
			release(server, player);
		}
	}

	private static void arrive(ServerLevel belly, ServerPlayer player, Swallowed s) {
		BlockPos spot = spot(player);
		debris(belly, spot);
		player.teleport(new TeleportTransition(belly, Vec3.atBottomCenterOf(spot), Vec3.ZERO, player.getYRot(), 0,
				TeleportTransition.DO_NOTHING));
		player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
		List<UUID> creatures = new ArrayList<>();
		for (int i = 0; i < CREATURES; i++) {
			Husk remains = EntityTypes.HUSK.create(belly, EntitySpawnReason.TRIGGERED);
			if (remains == null) {
				continue;
			}
			double a = Math.PI * 2 * i / CREATURES;
			remains.snapTo(spot.getX() + 0.5 + Math.cos(a) * 8, spot.getY(), spot.getZ() + 0.5 + Math.sin(a) * 8, 0, 0);
			remains.setCustomName(Component.translatable("entity.fmab.devoured_remains"));
			remains.setPersistenceRequired();
			belly.addFreshEntity(remains);
			creatures.add(remains.getUUID());
		}
		player.setAttached(FmabAttachments.SWALLOWED, new Swallowed(s.dimension(), s.origin(), 0, creatures));
	}

	/** Les restes de ce que Gluttony a dévoré, épars autour du joueur, et des mares de sang. */
	private static void debris(ServerLevel belly, BlockPos spot) {
		var random = belly.getRandom();
		Block[] junk = {Blocks.BONE_BLOCK, Blocks.COBBLESTONE, Blocks.OAK_LOG, Blocks.MOSSY_COBBLESTONE, Blocks.IRON_CHAIN,
				Blocks.IRON_BARS, Blocks.BROWN_MUSHROOM_BLOCK};
		for (int i = 0; i < 40; i++) {
			BlockPos p = spot.offset(random.nextInt(31) - 15, 0, random.nextInt(31) - 15);
			if (p.distSqr(spot) < 9) {
				continue;
			}
			if (random.nextInt(3) == 0) {
				belly.setBlockAndUpdate(p.below(), FmabBlocks.CRYSTALLIZED_BLOOD.defaultBlockState());
			} else if (belly.getBlockState(p).isAir()) {
				belly.setBlockAndUpdate(p, junk[random.nextInt(junk.length)].defaultBlockState());
			}
		}
	}

	/** Recraché : retour là où Gluttony l'avait avalé, et les restes s'évanouissent. */
	private static void release(MinecraftServer server, ServerPlayer player) {
		Swallowed s = player.getAttached(FmabAttachments.SWALLOWED);
		player.removeAttached(FmabAttachments.SWALLOWED);
		ServerLevel belly = server.getLevel(BELLY);
		if (s == null) {
			return;
		}
		if (belly != null) {
			s.creatures().stream().map(belly::getEntity).filter(e -> e != null).forEach(Entity::discard);
		}
		ServerLevel home = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(s.dimension())));
		Vec3 at = Vec3.atBottomCenterOf(s.origin()).add(1.5, 0, 1.5);
		if (home == null) {
			home = server.overworld();
			at = Vec3.atBottomCenterOf(home.getRespawnData().pos());
		}
		player.teleport(new TeleportTransition(home, at, Vec3.ZERO, player.getYRot(), 0, TeleportTransition.DO_NOTHING));
		TransmutationLightning.discharge(home, BlockPos.containing(at), 1, 0.6);
		home.playSound(null, BlockPos.containing(at), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.5f, 0.6f);
	}
}
