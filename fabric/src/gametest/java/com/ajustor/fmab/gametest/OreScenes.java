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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Élément + Décomposer brise les minerais de cet élément autour du cercle, et seulement eux : le fer
 * défait le minerai de fer et laisse le charbon, le carbone défait le charbon. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=ores}.
 */
public class OreScenes implements FabricClientGameTest {
	private static final BlockPos CIRCLE = new BlockPos(0, -60, 3);
	private static final BlockPos IRON = CIRCLE.below().east();
	private static final BlockPos COAL = CIRCLE.below().west();

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("ores")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("tp @p 0.5 -60 0.5 0 0");
			sp.getConnection().waitForChunksRender();
			String iron = sp.getServer().computeOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.level().setBlockAndUpdate(IRON, Blocks.IRON_ORE.defaultBlockState());
				p.level().setBlockAndUpdate(COAL, Blocks.COAL_ORE.defaultBlockState());
				return decompose(p, "fmab:fer") + " fer=" + p.level().getBlockState(IRON).is(Blocks.IRON_ORE)
						+ " charbon=" + p.level().getBlockState(COAL).is(Blocks.COAL_ORE);
			});
			check(iron.equals("DONE fer=false charbon=true"), "le fer devrait défaire le minerai de fer seul : " + iron);
			context.waitTicks(10);
			String coal = sp.getServer().computeOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				return decompose(p, "fmab:carbone") + " charbon=" + p.level().getBlockState(COAL).is(Blocks.COAL_ORE);
			});
			check(coal.equals("DONE charbon=false"), "le carbone devrait défaire le minerai de charbon : " + coal);
		}
	}

	/** Trace Élément + Décomposer à la craie et pose la paume dessus. */
	private static Transmutation.Result decompose(ServerPlayer p, String element) {
		var level = p.level();
		// Des glyphes d'apprenti, compris : pas besoin du rang d'Initié (ses progrès mettraient la partie en pause).
		FmabAttachments.ALCHEMIST.set(p, AlchemistData.NEW.withRank(Rank.ALCHEMIST).learn(element).learn("fmab:decomposer"));
		AlchemyRules rules = AlchemyRules.of(level.registryAccess());
		Glyph e = rules.glyph(element).orElseThrow();
		Glyph a = rules.glyph("fmab:decomposer").orElseThrow();
		Drawing drawing = SimpleCircles.of(e, a, SimpleCircles.sidesFor(e, a));
		BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
				.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
		level.setBlockAndUpdate(CIRCLE, state);
		if (level.getBlockEntity(CIRCLE) instanceof TransmutationCircleBlockEntity be) {
			be.setDrawing(drawing);
		}
		Concentration.set(p, Concentration.max(p));
		return Transmutation.activate(level, CIRCLE, state, drawing, p, CircleSize.NORMAL);
	}
}
