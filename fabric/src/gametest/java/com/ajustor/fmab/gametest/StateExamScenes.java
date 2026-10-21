package com.ajustor.fmab.gametest;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.Treatise;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.client.screen.ExamScreen;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.network.OpenExamPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.state.StateExam;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.Transmutation;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.io.Reader;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * L'examen d'État demande un cercle à deux étages réussi : sans lui, l'examinateur refuse les objets ;
 * un Alchimiste qui réussit le plan B du Traité (deux étages) peut ensuite les remettre. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=state_exam}.
 */
public class StateExamScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("state_exam")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("tp @p 0.5 -60 0.5 0 0");
			sp.getConnection().waitForChunksRender();
			context.waitTicks(20);
			Drawing planB = context.computeOnClient(mc -> {
				try (Reader reader = mc.getResourceManager().openAsReader(Fmab.id("treatise/treatise.json"))) {
					return Treatise.parse(JsonParser.parseReader(reader).getAsJsonObject()).chapters().stream()
							.flatMap(c -> c.pages().stream())
							.filter(p -> p instanceof Treatise.ExamplePage e && e.textKey().equals("treatise.fmab.ch5.fallback_wall"))
							.map(p -> ((Treatise.ExamplePage) p).drawing()).findFirst().orElseThrow();
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			});
			// Un Alchimiste, tous ses glyphes compris, les objets de l'examen en poche.
			boolean refused = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				AlchemistData data = AlchemistData.NEW.withRank(Rank.ALCHEMIST);
				for (Glyph g : AlchemyRules.of(p.level().registryAccess()).glyphs()) {
					data = data.learn(g.id());
				}
				FmabAttachments.ALCHEMIST.set(p, data);
				p.getInventory().add(new ItemStack(FmabItems.STONE_LANCE));
				p.getInventory().add(new ItemStack(Items.ICE, 8));
				p.getInventory().add(new ItemStack(Items.IRON_INGOT, 4));
				return !StateExam.giveItems(p);
			});
			check(refused, "sans cercle à deux étages, l'examinateur devrait refuser les objets");
			exam(context, sp);
			context.waitTicks(2);
			context.takeScreenshot("state_exam_before_two_stages");
			context.setScreen(() -> null);

			// Le plan B : Eau et Fixer au centre, Terre et Fixer dehors, reliés par un trait brisé.
			Transmutation.Result result = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				var level = p.level();
				BlockPos circle = new BlockPos(0, -60, 4);
				BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
						.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
				level.setBlockAndUpdate(circle, state);
				if (level.getBlockEntity(circle) instanceof TransmutationCircleBlockEntity be) {
					be.setDrawing(planB);
				}
				Concentration.set(p, Concentration.max(p));
				return Transmutation.activate(level, circle, state, planB, p, CircleSize.NORMAL);
			});
			check(result == Transmutation.Result.DONE, "un Alchimiste devrait réussir le plan B : " + result);
			boolean proven = sp.getServer().computeOnServer(server ->
					FmabAttachments.EXAM.getOrCreate(server.getPlayerList().getPlayers().getFirst()).twoStages());
			check(proven, "le cercle à deux étages devrait compter pour l'examen");
			exam(context, sp);
			context.waitTicks(2);
			context.takeScreenshot("state_exam_ready");
			context.setScreen(() -> null);
			boolean accepted = sp.getServer().computeOnServer(server ->
					StateExam.giveItems(server.getPlayerList().getPlayers().getFirst()));
			check(accepted, "avec le cercle à deux étages, l'examinateur devrait accepter les objets");
		}
	}

	/** L'examen, tel que l'examinateur l'ouvre au candidat. */
	private static void exam(ClientGameTestContext context, TestSingleplayerContext sp) {
		OpenExamPayload state = sp.getServer().computeOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst();
			return OpenExamPayload.of(p, p);
		});
		context.setScreen(() -> new ExamScreen(state));
	}
}
