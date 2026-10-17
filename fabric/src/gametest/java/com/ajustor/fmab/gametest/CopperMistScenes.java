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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * FMAB-61 : le cuivre touche aux circuits (Fixer : un bloc de redstone le temps d'une charge ; Projeter :
 * les fils alentour se défont). Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=copper}.
 */
public class CopperMistScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("copper")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("tp @p 0.5 -60 0.5 0 0");
			sp.getConnection().waitForChunksRender();
			String power = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				FmabAttachments.ALCHEMIST.set(p, AlchemistData.NEW.learn("fmab:cuivre").learn("fmab:fixer")
						.learn("fmab:projeter"));
				BlockPos circle = new BlockPos(0, -60, 4);
				Transmutation.Result r = cast(p, circle, "fmab:cuivre", "fmab:fixer");
				// Face au sud : le bloc de redstone se forme un bloc plus loin.
				boolean charged = p.level().getBlockState(circle.south()).is(Blocks.REDSTONE_BLOCK);
				return r + " chargé=" + charged;
			});
			check(power.equals("DONE chargé=true"), "le cuivre devrait alimenter : " + power);

			String cut = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				var level = p.level();
				BlockPos circle = new BlockPos(10, -60, 4);
				level.setBlockAndUpdate(circle.east(2), Blocks.REDSTONE_WIRE.defaultBlockState());
				level.setBlockAndUpdate(circle.west(2), Blocks.REDSTONE_TORCH.defaultBlockState());
				Transmutation.Result r = cast(p, circle, "fmab:cuivre", "fmab:projeter");
				return r + " fil=" + level.getBlockState(circle.east(2)).is(Blocks.REDSTONE_WIRE)
						+ " torche=" + level.getBlockState(circle.west(2)).is(Blocks.REDSTONE_TORCH);
			});
			check(cut.equals("DONE fil=false torche=false"), "le cuivre devrait couper le circuit : " + cut);
		}
	}

	private static Transmutation.Result cast(ServerPlayer p, BlockPos circle, String element, String action) {
		var level = p.level();
		AlchemyRules rules = AlchemyRules.of(level.registryAccess());
		Glyph e = rules.glyph(element).orElseThrow();
		Glyph a = rules.glyph(action).orElseThrow();
		Drawing drawing = SimpleCircles.of(e, a, SimpleCircles.sidesFor(e, a));
		BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
				.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
		level.setBlockAndUpdate(circle, state);
		if (level.getBlockEntity(circle) instanceof TransmutationCircleBlockEntity be) {
			be.setDrawing(drawing);
		}
		Concentration.set(p, Concentration.max(p));
		return Transmutation.activate(level, circle, state, drawing, p, CircleSize.NORMAL);
	}
}
