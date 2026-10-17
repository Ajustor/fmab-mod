package com.ajustor.fmab.platform.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/** Les joueurs côté serveur : connexion, déconnexion, mort et réapparition. */
public final class PlayerEvents {
	/** Un joueur vient de rejoindre le serveur. */
	public static final Event<Connection> JOIN = Event.create(listeners -> (player, server) ->
			listeners.forEach(l -> l.on(player, server)));
	/** Un joueur quitte le serveur. */
	public static final Event<Connection> DISCONNECT = Event.create(listeners -> (player, server) ->
			listeners.forEach(l -> l.on(player, server)));
	/** Un joueur réapparaît (après sa mort, ou en revenant de l'End) : l'ancien corps, le nouveau. */
	public static final Event<Respawn> AFTER_RESPAWN = Event.create(listeners -> (oldPlayer, newPlayer, alive) ->
			listeners.forEach(l -> l.afterRespawn(oldPlayer, newPlayer, alive)));
	/** Un joueur va mourir : un seul refus suffit à l'empêcher. */
	public static final Event<AllowDeath> ALLOW_DEATH = Event.create(listeners -> (player, source, amount) -> {
		for (AllowDeath l : listeners) {
			if (!l.allowDeath(player, source, amount)) {
				return false;
			}
		}
		return true;
	});

	private PlayerEvents() {
	}

	@FunctionalInterface
	public interface Connection {
		void on(ServerPlayer player, MinecraftServer server);
	}

	@FunctionalInterface
	public interface Respawn {
		void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive);
	}

	@FunctionalInterface
	public interface AllowDeath {
		boolean allowDeath(ServerPlayer player, DamageSource source, float amount);
	}
}
