package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.SimpleCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
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
import net.minecraft.world.level.block.state.BlockState;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * La main s'exerce : le même mur de terre coûte moins de concentration à qui a beaucoup pratiqué la
 * Terre qu'à qui vient d'en apprendre le premier nœud. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=practice}.
 */
public class PracticeScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("practice")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("tp @p 0.5 -60 0.5 0 0");
			sp.getConnection().waitForChunksRender();
			// Le premier nœud de la Terre est acquis dès 15 points : seule la pratique sépare les deux.
			float beginner = spent(context, sp, new BlockPos(0, -60, 4), 15);
			float practised = spent(context, sp, new BlockPos(12, -60, 4), 300);
			check(beginner > 0, "le mur devrait coûter de la concentration");
			check(practised < beginner, "la pratique devrait alléger le mur : " + practised + " contre " + beginner);
			check(practised <= Math.round(beginner * 0.5f),
					"cent transmutations de Terre : moitié prix (" + practised + " contre " + beginner + ")");
		}
	}

	/** La concentration que coûte un mur de terre à qui a cette maîtrise de la Terre. */
	private static float spent(ClientGameTestContext context, TestSingleplayerContext sp, BlockPos circle, int mastery) {
		float spent = sp.getServer().computeOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst();
			var level = p.level();
			AlchemyRules rules = AlchemyRules.of(level.registryAccess());
			Glyph earth = rules.glyph("fmab:terre").orElseThrow();
			Glyph fix = rules.glyph("fmab:fixer").orElseThrow();
			Drawing drawing = SimpleCircles.of(earth, fix, SimpleCircles.sidesFor(earth, fix));
			BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
					.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
			level.setBlockAndUpdate(circle, state);
			if (level.getBlockEntity(circle) instanceof TransmutationCircleBlockEntity be) {
				be.setDrawing(drawing);
			}
			AlchemistData fresh = AlchemistData.NEW.addMastery("earth", mastery);
			FmabAttachments.ALCHEMIST.set(p, fresh);
			float max = Concentration.max(p);
			Concentration.set(p, max);
			Transmutation.Result result = Transmutation.activate(level, circle, state, drawing, p, CircleSize.NORMAL);
			check(result != Transmutation.Result.INERT && result != Transmutation.Result.TIRED,
					"le mur devrait prendre (" + result + ")");
			return max - Concentration.of(p);
		});
		context.waitTicks(20);
		return spent;
	}
}
