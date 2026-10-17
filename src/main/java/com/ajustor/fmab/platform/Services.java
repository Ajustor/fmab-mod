package com.ajustor.fmab.platform;

import java.util.ServiceLoader;

/** Trouve l'implémentation d'un port fournie par le loader en cours (une seule doit exister). */
public final class Services {
	private Services() {
	}

	public static <T> T load(Class<T> port) {
		var found = ServiceLoader.load(port, Services.class.getClassLoader()).stream().toList();
		if (found.size() != 1) {
			throw new IllegalStateException("Il faut exactement une implémentation de " + port.getName()
					+ " (trouvées : " + found.size() + ") : le mod tourne-t-il sur un loader pris en charge ?");
		}
		return found.getFirst().get();
	}
}
