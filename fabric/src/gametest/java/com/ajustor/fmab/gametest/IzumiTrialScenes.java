package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.SimpleCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.client.screen.IzumiScreen;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.data.Training;
import com.ajustor.fmab.network.OpenIzumiPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.training.Trial;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.CircleFrame;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.GloveCasting;
import com.ajustor.fmab.transmutation.Transmutation;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les épreuves d'Izumi, à la portée d'un Apprenti : il comprend le Feu à force de le tracer (cinq
 * cercles, qui rebondissent tous), et l'épreuve de la pratique est réussie. Chez Izumi, survoler une
 * épreuve dit comment la réussir. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=izumi_trials}.
 */
public class IzumiTrialScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("izumi_trials")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("tp @p 0.5 -60 0.5 0 0");
			sp.getConnection().waitForChunksRender();
			context.waitTicks(20);
			sp.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				FmabAttachments.TRAINING.set(p, Training.NONE.meet().achieve(Trial.FIRST_WALL.id()));
			});
			// Un Apprenti trace cinq fois un cercle de Feu : il rebondit, mais le Feu finit par être compris.
			for (int i = 0; i < 5; i++) {
				sp.getServer().runOnServer(server -> {
					var p = server.getPlayerList().getPlayers().getFirst();
					var level = p.level();
					AlchemyRules rules = AlchemyRules.of(level.registryAccess());
					Glyph fire = rules.glyph("fmab:feu").orElseThrow();
					Glyph project = rules.glyph("fmab:projeter").orElseThrow();
					Drawing drawing = SimpleCircles.of(fire, project, SimpleCircles.sidesFor(fire, project));
					BlockPos circle = new BlockPos(0, -60, 4);
					BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
							.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
					level.setBlockAndUpdate(circle, state);
					if (level.getBlockEntity(circle) instanceof TransmutationCircleBlockEntity be) {
						be.setDrawing(drawing);
					}
					Concentration.set(p, Concentration.max(p));
					p.setHealth(p.getMaxHealth());
					Transmutation.activate(level, circle, state, drawing, p, CircleSize.NORMAL);
				});
				context.waitTicks(10);
			}
			List<String> achieved = sp.getServer().computeOnServer(server -> List.copyOf(FmabAttachments.TRAINING
					.getOrCreate(server.getPlayerList().getPlayers().getFirst()).achieved()));
			check(achieved.contains(Trial.PRACTICE.id()), "comprendre le Feu en le traçant devrait réussir l'épreuve : " + achieved);
			sp.getConnection().waitForClientboundPackets();

			// Chez Izumi : le mur et la pratique réussis.
			context.setScreen(() -> new IzumiScreen(new OpenIzumiPayload(0, achieved, List.of())));
			context.waitTicks(2);
			context.runOnClient(mc -> RenderShowcase.hideGui(mc, false));
			context.takeScreenshot("izumi_trials_list");
			hover(context, Trial.GLOVES);
			context.takeScreenshot("izumi_trials_hint_gloves");
			hover(context, Trial.PRACTICE);
			context.takeScreenshot("izumi_trials_hint_practice");
			context.setScreen(() -> null);

			// L'épreuve du gant : une transmutation sans cercle tracé (des mains jointes) ne la valide pas.
			sp.getServer().runCommand("tp @p 0.5 -60 0.5 0 60");
			context.waitTicks(5);
			boolean clapped = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				// Ce que fait le claquement de mains : le cercle du carnet agit sans avoir été tracé, et sans gant.
				Concentration.set(p, Concentration.max(p));
				Transmutation.activate(p.level(), new BlockPos(0, -60, 3), CircleFrame.forFace(Direction.UP, Direction.SOUTH),
						wall(p), p, false, Integer.MAX_VALUE);
				return FmabAttachments.TRAINING.getOrCreate(p).achieved().contains(Trial.GLOVES.id());
			});
			check(!clapped, "joindre les mains ne devrait pas réussir l'épreuve du gant");
			context.setScreen(() -> null);
			// Ailleurs, que le mur des mains jointes ne gêne pas.
			sp.getServer().runCommand("tp @p 10.5 -60 0.5 0 60");
			context.waitTicks(15);
			boolean gloved = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				// Les gants veulent la main libre.
				p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				ItemStack glove = new ItemStack(FmabItems.CLOTH_GLOVES);
				glove.set(FmabComponents.GLOVE_CIRCLE, wall(p));
				FmabAttachments.GLOVES.set(p, Gloves.NONE.with(false, glove));
				Concentration.set(p, Concentration.max(p));
				GloveCasting.cast(p, false);
				return FmabAttachments.TRAINING.getOrCreate(p).achieved().contains(Trial.GLOVES.id());
			});
			check(gloved, "une transmutation au gant devrait réussir l'épreuve du gant");
		}
	}

	/** Un mur de terre : Terre et Fixer. */
	private static Drawing wall(ServerPlayer p) {
		AlchemyRules rules = AlchemyRules.of(p.level().registryAccess());
		Glyph earth = rules.glyph("fmab:terre").orElseThrow();
		Glyph fix = rules.glyph("fmab:fixer").orElseThrow();
		return SimpleCircles.of(earth, fix, SimpleCircles.sidesFor(earth, fix));
	}

	/** Le curseur sur la ligne de cette épreuve, comme le joueur qui la survole. */
	private static void hover(ClientGameTestContext context, Trial trial) {
		double[] at = context.computeOnClient(mc -> {
			var window = mc.getWindow();
			int height = window.getGuiScaledHeight(), width = window.getGuiScaledWidth();
			int panel = 70 + Trial.values().length * 18 + 12 + 30;
			int top = (height - panel) / 2;
			double scale = window.getGuiScale();
			return new double[]{(width / 2.0) * scale, (top + 62 + trial.ordinal() * 18 + 3) * scale};
		});
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(2);
	}
}
