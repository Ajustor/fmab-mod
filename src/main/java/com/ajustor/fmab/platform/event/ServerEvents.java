package com.ajustor.fmab.platform.event;

import net.minecraft.server.MinecraftServer;

/** La vie du serveur : démarrage, arrêt, chaque tick, rechargement des données. */
public final class ServerEvents {
	/** Le serveur a démarré, les mondes sont chargés. */
	public static final Event<Server> STARTED = Event.create(listeners -> server -> listeners.forEach(l -> l.on(server)));
	/** Le serveur s'est arrêté. */
	public static final Event<Server> STOPPED = Event.create(listeners -> server -> listeners.forEach(l -> l.on(server)));
	/** La fin de chaque tick du serveur. */
	public static final Event<Server> END_TICK = Event.create(listeners -> server -> listeners.forEach(l -> l.on(server)));
	/** Les packs de données viennent d'être rechargés ({@code /reload}). */
	public static final Event<DataReloaded> DATA_RELOADED = Event.create(listeners -> (server, success) ->
			listeners.forEach(l -> l.onReloaded(server, success)));

	private ServerEvents() {
	}

	@FunctionalInterface
	public interface Server {
		void on(MinecraftServer server);
	}

	@FunctionalInterface
	public interface DataReloaded {
		void onReloaded(MinecraftServer server, boolean success);
	}
}
