package com.ajustor.fmab.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

import java.util.List;
import java.util.Locale;

/**
 * Les objets du mod accrochés dans des cadres sur un mur, un porte-armure vêtu du plastron d'âme, et
 * la lance de pierre en main. Lancer avec {@code ./gradlew runClientGameTest -PfmabGametest=items}.
 */
public class ItemScenes implements FabricClientGameTest {
	private static final List<String> ITEMS = List.of("alchemical_ink", "alchemical_paint", "alchemical_thread", "chalk",
			"alchemy_tome", "alchemy_treatise", "ishval_tattoo", "crimson_seals", "cloth_gloves", "spark_gloves",
			"state_gloves", "iron_gauntlets", "leather_gloves", "philosopher_stone", "philosopher_stone_core",
			"red_stone_shard", "state_watch", "soul_chestplate", "stone_lance", "stone_golem_spawn_egg");
	private static final int Y = -60;
	private static final int PER_ROW = 7;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("items")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("weather clear");
			// Le mur, face au nord ; les cadres accrochés côté nord.
			sp.getServer().runCommand("fill -1 %d 0 %d %d 0 minecraft:spruce_planks".formatted(Y, PER_ROW, Y + 4));
			for (int i = 0; i < ITEMS.size(); i++) {
				int x = i % PER_ROW, y = Y + 3 - i / PER_ROW;
				sp.getServer().runCommand(String.format(Locale.ROOT,
						"summon item_frame %d %d -1 {Facing:2b,Fixed:1b,Invisible:0b,Item:{id:\"fmab:%s\",count:1}}",
						x, y, ITEMS.get(i)));
			}
			sp.getServer().runCommand(String.format(Locale.ROOT,
					"summon armor_stand 10.5 %d -2.5 {Rotation:[200f,0f],ShowArms:1b,equipment:{chest:{id:\"fmab:soul_chestplate\",count:1},"
							+ "mainhand:{id:\"fmab:stone_lance\",count:1}}}", Y));
			sp.getServer().runCommand("gamemode spectator @p");
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
			sp.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %.1f -6.5 0 8", PER_ROW / 2.0, Y + 0.6));
			context.waitTicks(60);
			context.takeScreenshot("items_wall");
			sp.getServer().runCommand(String.format(Locale.ROOT, "tp @p 10.5 %.1f -5.2 0 12", Y + 0.4));
			context.waitTicks(20);
			context.takeScreenshot("items_soul_armor");
			// La lance en main, à la première personne.
			sp.getServer().runCommand("gamemode creative @p");
			sp.getServer().runCommand("item replace entity @p weapon.mainhand with fmab:stone_lance");
			sp.getServer().runCommand(String.format(Locale.ROOT, "tp @p 3.5 %d -6.5 0 0", Y));
			context.waitTicks(20);
			context.runOnClient(mc -> RenderShowcase.hideGui(mc, false));
			context.waitTicks(2);
			context.takeScreenshot("items_lance_in_hand");
		}
	}
}
