package com.ajustor.fmab.stone;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.entity.EnvyEntity;
import com.ajustor.fmab.entity.HomunculusEntity;
import com.ajustor.fmab.entity.LustEntity;
import com.ajustor.fmab.gate.GateOfTruth;
import com.ajustor.fmab.item.PhilosopherStoneItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * La Pierre philosophale vivante, comme Van Hohenheim : l'alchimiste a fondu dans son corps les âmes
 * de deux Pierres pleines, pendant une éclipse, sur un cercle de transmutation humaine. Il ne paie
 * plus de péage à la Porte, ses transmutations ne lui coûtent rien, et il ressuscite sur place à
 * chaque mort. Mais chaque grande transmutation et chaque mort consument des âmes ; à zéro, il meurt
 * pour de bon et redevient un homme. Son karma touche le fond, et les homonculus le traquent.
 */
public final class LivingStone {
	/** Les âmes de deux Pierres pleines. */
	public static final int SOULS = 200;
	/** Une mort coûte cher ; une grande transmutation (deux étages ou plus), une âme. */
	private static final int DEATH = 20;
	private static final DustParticleOptions RED = new DustParticleOptions(0xD01020, 1.2f);
	/** Toutes les dix minutes, une chance sur trois qu'un homonculus vienne le chercher. */
	private static final int HUNT_PERIOD = 20 * 60 * 10;

	private LivingStone() {
	}

	public static int souls(ServerPlayer player) {
		Integer souls = player.getAttached(FmabAttachments.LIVING_STONE);
		return souls == null ? 0 : souls;
	}

	/**
	 * Le rituel : sur un cercle de transmutation humaine, pendant l'éclipse, un Initié de la Porte qui
	 * tient une Pierre pleine dans chaque main devient une Pierre vivante.
	 *
	 * @return vrai si le rituel a eu lieu (la transmutation humaine n'a pas lieu)
	 */
	public static boolean tryRitual(ServerPlayer player, BlockPos circle) {
		ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
		ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
		if (!full(main) || !full(off) || souls(player) > 0) {
			return false;
		}
		if (!Eclipse.now(player.level())) {
			player.sendSystemMessage(Component.translatable("stone.fmab.ritual_needs_eclipse"));
			return true;
		}
		if (!player.getAttachedOrCreate(FmabAttachments.ALCHEMIST).rank()
				.atLeast(Rank.GATE)) {
			player.sendSystemMessage(Component.translatable("stone.fmab.ritual_needs_gate"));
			return true;
		}
		if (!player.isCreative()) {
			main.shrink(1);
			off.shrink(1);
		}
		player.setAttached(FmabAttachments.LIVING_STONE, SOULS);
		GateState gate = player.getAttachedOrCreate(FmabAttachments.GATE);
		player.setAttached(FmabAttachments.GATE, gate.restore(gate.lost()));
		GateOfTruth.returnParts(player);
		Karma.set(player, Karma.MIN);
		player.setHealth(player.getMaxHealth());
		ServerLevel level = player.level();
		level.sendParticles(RED, player.getX(), player.getY(0.5), player.getZ(), 300, 1.5, 1.5, 1.5, 0);
		TransmutationLightning.discharge(level, circle, 3, 1.5);
		level.playSound(null, circle, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5f, 0.6f);
		player.sendSystemMessage(Component.translatable("stone.fmab.became_living_stone", SOULS));
		return true;
	}

	private static boolean full(ItemStack stack) {
		return stack.is(FmabItems.PHILOSOPHER_STONE) && PhilosopherStoneItem.souls(stack) >= stack.getMaxDamage();
	}

	/** Une grande transmutation consume une âme. */
	public static void spend(ServerPlayer player, int souls) {
		int left = souls(player);
		if (left <= 0 || player.isCreative()) {
			return;
		}
		player.setAttached(FmabAttachments.LIVING_STONE, Math.max(1, left - souls));
	}

	public static void register() {
		// Il ressuscite sur place tant qu'il lui reste des âmes.
		ServerPlayerEvents.ALLOW_DEATH.register((player, source, amount) -> {
			int left = souls(player);
			if (left <= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && player.getY() < -128) {
				return true;
			}
			int after = left - DEATH;
			if (after <= 0) {
				player.removeAttached(FmabAttachments.LIVING_STONE);
				player.sendSystemMessage(Component.translatable("stone.fmab.living_stone_spent"));
				return true;
			}
			player.setAttached(FmabAttachments.LIVING_STONE, after);
			player.setHealth(player.getMaxHealth());
			player.level().sendParticles(RED, player.getX(), player.getY(0.5), player.getZ(), 80, 0.5, 1, 0.5, 0);
			player.level().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS,
					1, 0.8f);
			player.sendOverlayMessage(Component.translatable("stone.fmab.resurrected", after));
			return false;
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % HUNT_PERIOD != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				boolean wanted = souls(player) > 0 || Karma.of(player) <= Karma.RECRUITABLE;
				if (wanted && player.level().getRandom().nextInt(3) == 0) {
					hunt(player);
				}
			}
		});
	}

	/**
	 * Les homonculus viennent chercher une Pierre vivante (et recruter les alchimistes sans
	 * scrupules) : Lust, ou Envy démasqué, non loin du joueur.
	 */
	private static void hunt(ServerPlayer player) {
		ServerLevel level = player.level();
		if (!level.getEntitiesOfClass(HomunculusEntity.class, player.getBoundingBox().inflate(64)).isEmpty()) {
			return;
		}
		double angle = level.getRandom().nextDouble() * Math.PI * 2;
		int x = player.getBlockX() + (int) (Math.cos(angle) * 28);
		int z = player.getBlockZ() + (int) (Math.sin(angle) * 28);
		BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
		HomunculusEntity hunter = level.getRandom().nextBoolean()
				? FmabEntities.LUST.create(level, EntitySpawnReason.EVENT)
				: FmabEntities.ENVY.create(level, EntitySpawnReason.EVENT);
		if (hunter == null) {
			return;
		}
		hunter.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		if (hunter instanceof EnvyEntity envy) {
			envy.disguiseAs(player);
		}
		level.addFreshEntity(hunter);
		hunter.setTarget(player);
		player.sendSystemMessage(Component.translatable(hunter instanceof LustEntity
				? "stone.fmab.hunted_lust" : "stone.fmab.hunted_envy"));
	}
}
