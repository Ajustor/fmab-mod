package com.ajustor.fmab.stone;

import com.ajustor.fmab.entity.AmestrianSoldierEntity;
import com.ajustor.fmab.entity.HauntedArmorEntity;
import com.ajustor.fmab.entity.HomunculusEntity;
import com.ajustor.fmab.entity.ImmortalSoldierEntity;
import com.ajustor.fmab.registry.FmabAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Le karma : une jauge morale de −100 à +100. Fabriquer des Pierres, tuer des innocents la fait
 * chuter ; détruire des homonculus, défendre les villageois, réparer la fait monter. Les villageois
 * en parlent entre eux : un karma bas renchérit leurs prix et lâche sur vous leurs golems, un karma
 * haut leur arrache des rabais.
 */
public final class Karma {
	public static final int MIN = -100;
	public static final int MAX = 100;
	/** En dessous, les homonculus commencent à s'intéresser à vous. */
	public static final int RECRUITABLE = -60;
	/** Rayon dans lequel une créature tuée « défend » un villageois. */
	private static final double DEFENSE = 16;

	private Karma() {
	}

	public static int of(ServerPlayer player) {
		Integer k = player.getAttached(FmabAttachments.KARMA);
		return k == null ? 0 : k;
	}

	public static void add(ServerPlayer player, int delta) {
		if (delta == 0) {
			return;
		}
		int before = of(player);
		int after = Math.clamp(before + delta, MIN, MAX);
		player.setAttached(FmabAttachments.KARMA, after);
		// On ne prévient qu'aux paliers : le karma se sent plus qu'il ne se compte.
		if (before / 25 != after / 25 || Integer.signum(before) != Integer.signum(after)) {
			player.sendOverlayMessage(Component.translatable(after < before ? "karma.fmab.falls" : "karma.fmab.rises"));
		}
	}

	public static void set(ServerPlayer player, int value) {
		player.setAttached(FmabAttachments.KARMA, Math.clamp(value, MIN, MAX));
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(source.getEntity() instanceof ServerPlayer killer)) {
				return;
			}
			if (entity instanceof AbstractVillager) {
				add(killer, -8);
			} else if (entity instanceof AmestrianSoldierEntity) {
				add(killer, -5);
			} else if (entity instanceof HauntedArmorEntity || entity instanceof ImmortalSoldierEntity) {
				// Libérer une âme captive.
				add(killer, 2);
			} else if (entity instanceof HomunculusEntity) {
				add(killer, 10);
			} else if (entity instanceof Monster && !entity.level().getEntitiesOfClass(Villager.class,
					entity.getBoundingBox().inflate(DEFENSE)).isEmpty()) {
				add(killer, 1);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 100 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				gossip(player);
			}
		});
	}

	/**
	 * Les villageois des environs se racontent ce que vaut le joueur. Un karma très bas devient une
	 * mauvaise réputation majeure (prix exorbitants, golems hostiles) ; un karma haut, une bonne.
	 */
	private static void gossip(ServerPlayer player) {
		int karma = of(player);
		if (Math.abs(karma) < 25) {
			return;
		}
		for (Villager villager : player.level().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(24))) {
			if (karma < 0) {
				villager.getGossips().add(player.getUUID(), karma <= -50 ? GossipType.MAJOR_NEGATIVE : GossipType.MINOR_NEGATIVE,
						Math.abs(karma) / 10);
			} else {
				villager.getGossips().add(player.getUUID(), GossipType.MINOR_POSITIVE, karma / 10);
			}
		}
	}
}
