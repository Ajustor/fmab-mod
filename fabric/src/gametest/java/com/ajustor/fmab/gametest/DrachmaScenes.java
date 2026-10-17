package com.ajustor.fmab.gametest;

import com.ajustor.fmab.entity.DrachmaGeneralEntity;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Drachma : un camp de pillards et le fort du général, posés côte à côte et vus d'en haut. Le camp a
 * ses pillards et son coffre ; le fort, son général et son coffre. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=drachma}.
 */
public class DrachmaScenes implements FabricClientGameTest {
	private static final int GROUND = -60;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("drachma")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getServer().runCommand("time set noon");
			sp.getServer().runCommand("gamerule doMobSpawning false");
			sp.getConnection().waitForChunksRender();
			String placed = sp.getServer().computeOnServer(server -> {
				try {
					var dispatcher = server.getCommands().getDispatcher();
					var source = server.createCommandSourceStack().withSuppressedOutput();
					dispatcher.execute("place structure fmab:drachma_camp 0 %d 0".formatted(GROUND), source);
					dispatcher.execute("place structure fmab:drachma_fort 40 %d 0".formatted(GROUND), source);
				} catch (CommandSyntaxException e) {
					return "refusé : " + e.getMessage();
				}
				var level = server.overworld();
				int pillagers = level.getEntitiesOfClass(Pillager.class, new AABB(-20, GROUND - 2, -20, 20, GROUND + 10, 20)).size();
				int generals = level.getEntitiesOfClass(DrachmaGeneralEntity.class,
						new AABB(20, GROUND - 2, -20, 70, GROUND + 14, 30)).size();
				int chests = 0;
				for (BlockPos pos : BlockPos.betweenClosed(-20, GROUND - 1, -20, 70, GROUND + 6, 30)) {
					if (level.getBlockEntity(pos) instanceof ChestBlockEntity) {
						chests++;
					}
				}
				return "pillards=" + pillagers + " généraux=" + generals + " coffres=" + chests;
			});
			check(placed.equals("pillards=3 généraux=1 coffres=2"), "Drachma devrait être garni : " + placed);
			sp.getServer().runCommand("tp @p 20 %d -30 0 50".formatted(GROUND + 30));
			sp.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.takeScreenshot("drachma_camp_and_fort");
		}
	}
}
