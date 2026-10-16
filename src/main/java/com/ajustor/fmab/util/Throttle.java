package com.ajustor.fmab.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Un délai par joueur, tenu par le serveur : un client modifié peut envoyer vingt demandes par
 * seconde, le serveur n'en écoute qu'une par délai. Les joueurs partis sont oubliés ({@link PlayerMaps}),
 * et une horloge revenue en arrière (un autre monde, en solo) compte comme un délai écoulé.
 */
public final class Throttle {
	private final int ticks;
	private final Map<UUID, Long> last = PlayerMaps.track(new HashMap<>());

	/** @param ticks le délai entre deux actions acceptées */
	public Throttle(int ticks) {
		this.ticks = ticks;
	}

	/** Le joueur peut-il agir à cet instant ? Si oui, l'instant est retenu ; un refus ne repousse rien. */
	public boolean pass(UUID player, long now) {
		Long previous = last.get(player);
		if (previous != null && now >= previous && now - previous < ticks) {
			return false;
		}
		last.put(player, now);
		return true;
	}
}
