package com.ajustor.fmab.gametest;

import com.ajustor.fmab.Fmab;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Les blocs du mod, posés côte à côte sur deux rangées (et en double, pour voir comment leurs
 * textures se raccordent), vus de face puis d'en haut.
 */
public class BlockScenes implements FabricClientGameTest {
	/** Les blocs à montrer : tous ceux du mod, sauf le cercle (il n'existe que tracé sur une surface). */
	private static List<Identifier> blocks() {
		return BuiltInRegistries.BLOCK.keySet().stream()
				.filter(id -> id.getNamespace().equals(Fmab.MOD_ID) && !id.getPath().equals("transmutation_circle"))
				.sorted()
				.toList();
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("blocks")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("weather clear");
			List<Identifier> ids = blocks();
			int half = (ids.size() + 1) / 2;
			for (int i = 0; i < ids.size(); i++) {
				int x = (i % half) * 2 - half;
				int z = i < half ? 4 : 7;
				String id = ids.get(i).toString();
				// Deux blocs l'un sur l'autre : on voit le raccord vertical et le dessus.
				sp.getServer().runCommand("setblock %d -60 %d %s".formatted(x, z, id));
				sp.getServer().runCommand("setblock %d -59 %d %s".formatted(x, z, id));
			}
			// En spectateur : il reste où on le pose, sans tomber.
			sp.getServer().runCommand("gamemode spectator @p");
			sp.getServer().runCommand("tp @p 0 -57.5 -4 0 12");
			sp.getConnection().waitForChunksRender();
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
			context.waitTicks(20);
			context.takeScreenshot("blocks_00_front");
			sp.getServer().runCommand("tp @p 0 -54 0 0 48");
			context.waitTicks(20);
			context.takeScreenshot("blocks_01_above");
		}
	}
}
