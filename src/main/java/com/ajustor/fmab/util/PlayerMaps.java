package com.ajustor.fmab.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Les états que les règles gardent par joueur le temps d'une session : délais de recharge, combos,
 * marchés en cours, pas comptés… Une map inscrite ici perd l'entrée d'un joueur quand il se
 * déconnecte, et tout son contenu quand le serveur s'arrête. Sans cela, elles grossissaient sans
 * fin, et en solo, changer de monde gardait des heures de jeu de l'ancien, ce qui faussait les délais.
 */
public final class PlayerMaps {
	private static final List<Map<UUID, ?>> MAPS = new ArrayList<>();

	private PlayerMaps() {
	}

	/** Inscrit une map : à appeler à sa création, une fois. */
	public static synchronized <V> Map<UUID, V> track(Map<UUID, V> map) {
		MAPS.add(map);
		return map;
	}

	public static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
	}

	/** Oublie ce joueur dans toutes les maps inscrites. */
	public static synchronized void forget(UUID player) {
		MAPS.forEach(map -> map.remove(player));
	}

	/** Vide toutes les maps inscrites. */
	public static synchronized void clear() {
		MAPS.forEach(Map::clear);
	}
}
