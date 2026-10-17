package com.ajustor.fmab.gametest;

import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.GloveCasting;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * FMAB-61 : les gants sans cercle. Les gants à étincelles claquent et mettent le feu là où l'on
 * regarde ; les gantelets frappent le sol et soulèvent une ligne de piques devant eux. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=glove_arts}.
 */
public class GloveArtsScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("glove_arts")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("tp @p 0.5 -60 0.5 0 60");
			sp.getConnection().waitForChunksRender();
			context.waitTicks(5);
			String snap = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				// Les gants veulent la main libre : le Traité de départ va dans le sac.
				p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				FmabAttachments.GLOVES.set(p, Gloves.NONE.with(false, new ItemStack(FmabItems.SPARK_GLOVES)));
				float before = Concentration.max(p);
				Concentration.set(p, before);
				GloveCasting.cast(p, false);
				int fires = 0;
				for (BlockPos pos : BlockPos.betweenClosed(-4, -61, -1, 4, -58, 8)) {
					if (p.level().getBlockState(pos).getBlock() instanceof BaseFireBlock) {
						fires++;
					}
				}
				return "feu=" + (fires > 0) + " payé=" + (Concentration.of(p) < before);
			});
			check(snap.equals("feu=true payé=true"), "le claquement devrait mettre le feu : " + snap);

			String punch = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				for (BlockPos pos : BlockPos.betweenClosed(-4, -60, -1, 4, -58, 8)) {
					p.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				}
				FmabAttachments.GLOVES.set(p, Gloves.NONE.with(false, new ItemStack(FmabItems.IRON_GAUNTLETS)));
				Concentration.set(p, Concentration.max(p));
				float before = Concentration.of(p);
				// Face au sud : on frappe le sol juste devant soi.
				GloveCasting.strike(p, new BlockPos(0, -61, 1), Direction.UP);
				String seen = " (payé=" + (Concentration.of(p) < before) + " main=" + p.getMainHandItem()
						+ " face=" + p.getDirection() + " sol=" + p.level().getBlockState(new BlockPos(0, -61, 2))
						+ " au-dessus=" + p.level().getBlockState(new BlockPos(0, -60, 2)) + ")";
				int spikes = 0;
				for (int z = 2; z <= 6; z++) {
					if (!p.level().getBlockState(new BlockPos(0, -60, z)).isAir()) {
						spikes++;
					}
				}
				return "piques=" + spikes + (spikes == 5 ? "" : seen);
			});
			check(punch.startsWith("piques=5"), "le coup de poing devrait soulever cinq piques : " + punch);
		}
	}
}
