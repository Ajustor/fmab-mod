package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.SimpleCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.Transmutation;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * L'or interdit : trois blocs de fer sur le cercle, un seul devient de l'or ; les deux autres restent
 * du fer. Lancer avec {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=gold}.
 */
public class GoldScenes implements FabricClientGameTest {
	private static final BlockPos CIRCLE = new BlockPos(0, -60, 3);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("gold")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("tp @p 0.5 -60 0.5 0 0");
			sp.getConnection().waitForChunksRender();
			String result = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				var level = p.level();
				FmabAttachments.ALCHEMIST.set(p, AlchemistData.NEW.withRank(Rank.GATE).learn("fmab:or").learn("fmab:recomposer"));
				AlchemyRules rules = AlchemyRules.of(level.registryAccess());
				Glyph gold = rules.glyph("fmab:or").orElseThrow();
				Glyph recompose = rules.glyph("fmab:recomposer").orElseThrow();
				Drawing drawing = SimpleCircles.of(gold, recompose, SimpleCircles.sidesFor(gold, recompose));
				BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
						.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
				level.setBlockAndUpdate(CIRCLE, state);
				if (level.getBlockEntity(CIRCLE) instanceof TransmutationCircleBlockEntity be) {
					be.setDrawing(drawing);
				}
				ItemEntity iron = new ItemEntity(level, CIRCLE.getX() + 0.5, CIRCLE.getY() + 0.1, CIRCLE.getZ() + 0.5,
						new ItemStack(Items.IRON_BLOCK, 3));
				iron.setDeltaMovement(0, 0, 0);
				iron.setNeverPickUp();
				level.addFreshEntity(iron);
				Concentration.set(p, Concentration.max(p));
				Transmutation.Result r = Transmutation.activate(level, CIRCLE, state, drawing, p, CircleSize.NORMAL);
				int goldBlocks = 0;
				int ironBlocks = 0;
				for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(CIRCLE).inflate(3))) {
					if (e.getItem().is(Items.GOLD_BLOCK)) {
						goldBlocks += e.getItem().getCount();
					} else if (e.getItem().is(Items.IRON_BLOCK)) {
						ironBlocks += e.getItem().getCount();
					}
				}
				return r + " or=" + goldBlocks + " fer=" + ironBlocks;
			});
			check(result.equals("DONE or=1 fer=2"), "un seul bloc de fer devrait devenir de l'or : " + result);
		}
	}
}
