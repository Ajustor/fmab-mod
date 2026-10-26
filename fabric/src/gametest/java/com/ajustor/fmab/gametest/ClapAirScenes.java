package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.SimpleCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.data.TransmutationPose;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.GloveCasting;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les mains jointes face au ciel : un cercle d'air (une bourrasque) agit dans l'air devant soi ; un
 * cercle de terre, qui puise dans la matière, réclame toujours une surface. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=clap_air}.
 */
public class ClapAirScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("clap_air")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("weather clear");
			// Le regard droit vers le ciel : aucune surface à portée.
			sp.getServer().runCommand("tp @p 0.5 -60 0.5 0 -90");
			sp.getConnection().waitForChunksRender();
			context.waitTicks(5);

			String air = sp.getServer().computeOnServer(server -> clap(server.getPlayerList().getPlayers().getFirst(),
					"fmab:air", "fmab:projeter"));
			check(air.equals("CLAP"), "un cercle d'air devrait agir mains jointes, face au ciel : " + air);
			context.waitTicks(20);

			String earth = sp.getServer().computeOnServer(server -> clap(server.getPlayerList().getPlayers().getFirst(),
					"fmab:terre", "fmab:fixer"));
			check(earth.equals("NONE"), "un cercle de terre devrait réclamer une surface : " + earth);
		}
	}

	/** Joint les mains sur ce cercle et rend le geste joué (CLAP s'il a eu lieu, NONE sinon). */
	private static String clap(ServerPlayer p, String element, String action) {
		p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
		FmabAttachments.ALCHEMIST.set(p, AlchemistData.NEW.withRank(Rank.GATE));
		FmabAttachments.POSE.set(p, TransmutationPose.NONE);
		Concentration.set(p, Concentration.max(p));
		AlchemyRules rules = AlchemyRules.of(p.level().registryAccess());
		Glyph e = rules.glyph(element).orElseThrow();
		Glyph a = rules.glyph(action).orElseThrow();
		Drawing drawing = SimpleCircles.of(e, a, SimpleCircles.sidesFor(e, a));
		Notebooks.add(p, element, drawing);
		check(Notebooks.selected(p).equals(drawing), "le cercle devrait être sélectionné dans le carnet");
		GloveCasting.cast(p, true);
		TransmutationPose pose = FmabAttachments.POSE.get(p);
		return pose == null || !pose.active(p.level().getGameTime()) ? "NONE" : pose.kind().name();
	}
}
