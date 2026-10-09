package com.ajustor.fmab.entity;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Les barres de boss du mod : elles ne s'affichent qu'une fois le combat engagé, et aux seuls
 * joueurs qui sont vraiment là, distance mesurée en trois dimensions. Un boss qui attend sous la
 * ville ne s'annonce donc pas aux passants de la surface.
 */
public final class BossBars {
	/** Jusqu'où, en blocs, on voit la barre d'un combat en cours. */
	public static final double RANGE = 48;
	/** Tous les combien de ticks on revoit qui voit la barre. */
	private static final int REFRESH = 10;

	private BossBars() {
	}

	/** Le combat a commencé : le boss vise un joueur, ou un joueur l'a frappé il y a peu. */
	public static boolean engaged(Mob boss) {
		return boss.getTarget() instanceof Player || boss.getLastHurtByPlayer() != null;
	}

	/**
	 * À chaque tick du boss : la barre suit ses points de vie, et elle s'affiche aux joueurs à portée
	 * tant que {@code show} est vrai.
	 */
	public static void update(ServerBossEvent bar, Mob boss, boolean show) {
		if (!(boss.level() instanceof ServerLevel level)) {
			return;
		}
		bar.setProgress(boss.getHealth() / boss.getMaxHealth());
		if (boss.tickCount % REFRESH != 0) {
			return;
		}
		List<ServerPlayer> near = show && boss.isAlive()
				? level.getPlayers(p -> p.distanceToSqr(boss) <= RANGE * RANGE)
				: List.of();
		for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
			if (!near.contains(player)) {
				bar.removePlayer(player);
			}
		}
		near.forEach(bar::addPlayer);
	}
}
