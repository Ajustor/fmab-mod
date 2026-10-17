package com.ajustor.fmab.fabric;

import com.ajustor.fmab.Fmab;
import net.fabricmc.api.ModInitializer;

/** Le point d'entrée Fabric : brancher Fabric sur les ports du mod, puis lancer le mod. */
public final class FmabFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		FabricEvents.install();
		Fmab.init();
	}
}
