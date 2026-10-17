package com.ajustor.fmab.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Xing : le pavillon, son temple et ses maisons, et le pays alentour devenu biome de Xing. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=xing}.
 */
public class XingScenes implements FabricClientGameTest {
	private static final int GROUND = -60;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("xing")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getServer().runCommand("time set noon");
			sp.getConnection().waitForChunksRender();
			String placed = sp.getServer().computeOnServer(server -> {
				try {
					server.getCommands().getDispatcher().execute("place structure fmab:xing 0 %d 0".formatted(GROUND),
							server.createCommandSourceStack().withSuppressedOutput());
				} catch (CommandSyntaxException e) {
					return "refusé : " + e.getMessage();
				}
				var level = server.overworld();
				String biome = level.getBiome(new BlockPos(25, GROUND + 1, 25)).unwrapKey()
						.map(k -> k.identifier().toString()).orElse("?");
				int mud = 0, granite = 0;
				for (BlockPos pos : BlockPos.betweenClosed(-40, GROUND - 2, -40, 40, GROUND + 8, 40)) {
					var state = level.getBlockState(pos);
					if (state.is(Blocks.MUD_BRICKS)) {
						mud++;
					} else if (state.is(Blocks.POLISHED_GRANITE)) {
						granite++;
					}
				}
				return "biome=" + biome + " maisons=" + (mud > 40) + " temple=" + (granite > 100);
			});
			check(placed.equals("biome=fmab:xing maisons=true temple=true"), "Xing devrait être complet : " + placed);
			sp.getServer().runCommand("tp @p 0 %d -45 0 35".formatted(GROUND + 25));
			sp.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.takeScreenshot("xing_pavilion_and_halls");
		}
	}
}
