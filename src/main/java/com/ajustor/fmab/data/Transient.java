package com.ajustor.fmab.data;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * L'état passager que les systèmes gardent en mémoire, joueur par joueur (un marché en cours, un
 * temps de recharge…). Ce qui est suivi ici s'oublie quand le joueur se déconnecte, et tout se vide
 * à l'arrêt du serveur : rien ne s'accumule, et rien ne passe d'un monde à l'autre en solo.
 */
public final class Transient {
	private static final List<Map<UUID, ?>> PER_PLAYER = new ArrayList<>();
	private static final List<Collection<?>> PER_SERVER = new ArrayList<>();

	private Transient() {
	}

	public static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.getPlayer().getUUID();
			synchronized (PER_PLAYER) {
				PER_PLAYER.forEach(map -> map.remove(id));
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			synchronized (PER_PLAYER) {
				PER_PLAYER.forEach(Map::clear);
				PER_SERVER.forEach(Collection::clear);
			}
		});
	}

	/** Une table par joueur, oubliée à sa déconnexion et à l'arrêt du serveur. */
	public static <V, M extends Map<UUID, V>> M perPlayer(M map) {
		synchronized (PER_PLAYER) {
			PER_PLAYER.add(map);
		}
		return map;
	}

	/**
	 * Une table par entité (pas seulement des joueurs), ou une liste : vidée à l'arrêt du serveur.
	 * Ses entrées doivent expirer d'elles-mêmes.
	 */
	public static <C extends Collection<?>> C perServer(C collection) {
		synchronized (PER_PLAYER) {
			PER_SERVER.add(collection);
		}
		return collection;
	}

	/** Comme {@link #perServer(Collection)}, pour une table. */
	public static <V, M extends Map<UUID, V>> M perServerMap(M map) {
		synchronized (PER_PLAYER) {
			PER_SERVER.add(map.values());
		}
		return map;
	}
}
