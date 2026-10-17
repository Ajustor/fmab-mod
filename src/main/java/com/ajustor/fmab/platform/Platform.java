package com.ajustor.fmab.platform;

import com.ajustor.fmab.platform.data.DataSlots;
import com.ajustor.fmab.platform.network.Network;
import com.ajustor.fmab.platform.registry.Content;

import java.nio.file.Path;

/**
 * Ce que le mod attend du loader qui le fait tourner (Fabric aujourd'hui) : le mod ne connaît que
 * cette interface et ses ports ; chaque loader en fournit une implémentation, trouvée par
 * {@link java.util.ServiceLoader} ({@code META-INF/services/com.ajustor.fmab.platform.Platform}).
 *
 * <p>Les événements, eux, sont dans {@code platform.event} : le loader les déclenche, le mod les écoute.
 */
public interface Platform {
	static Platform get() {
		return Holder.INSTANCE;
	}

	/** Le nom du loader (« fabric »…), pour les journaux et les rapports. */
	String loader();

	/** Le dossier des fichiers de configuration. */
	Path configDir();

	boolean isModLoaded(String modId);

	DataSlots dataSlots();

	Network network();

	Content content();

	/** Chargé au premier appel, une fois. */
	final class Holder {
		private static final Platform INSTANCE = Services.load(Platform.class);

		private Holder() {
		}
	}
}
