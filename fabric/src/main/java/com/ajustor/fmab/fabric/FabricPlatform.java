package com.ajustor.fmab.fabric;

import com.ajustor.fmab.platform.Platform;
import com.ajustor.fmab.platform.data.DataSlots;
import com.ajustor.fmab.platform.network.Network;
import com.ajustor.fmab.platform.registry.Content;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** Le mod sur Fabric : Fabric Loader et Fabric API derrière les ports du mod. */
public final class FabricPlatform implements Platform {
	private final DataSlots dataSlots = new FabricDataSlots();
	private final Network network = new FabricNetwork();
	private final Content content = new FabricContent();

	@Override
	public String loader() {
		return "fabric";
	}

	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public DataSlots dataSlots() {
		return dataSlots;
	}

	@Override
	public Network network() {
		return network;
	}

	@Override
	public Content content() {
		return content;
	}
}
