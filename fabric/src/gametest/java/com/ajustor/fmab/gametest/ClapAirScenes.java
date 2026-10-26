package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.SimpleCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.data.TransmutationPose;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
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
 * Face au ciel : mains jointes, un cercle d'air (une bourrasque) agit dans l'air devant soi, et un
 * cercle de terre, qui puise dans la matière, réclame toujours une surface ; les gants de Mustang
 * claquent et lancent leur cercle d'air dans le vide. Lancer avec
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

			// Les gants de Mustang, face au ciel : le claquement enflamme l'air même, et un cercle de vent
			// brodé part dans le vide.
			context.waitTicks(20);
			float snapped = sp.getServer().computeOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				FmabAttachments.GLOVES.set(p, Gloves.NONE.with(false, new ItemStack(FmabItems.SPARK_GLOVES)));
				Concentration.set(p, Concentration.max(p));
				GloveCasting.cast(p, false);
				return Concentration.max(p) - Concentration.of(p);
			});
			check(snapped > 0, "le claquement de Mustang devrait partir face au ciel");
			context.waitTicks(20);
			int worn = sp.getServer().computeOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				ItemStack glove = new ItemStack(FmabItems.SPARK_GLOVES);
				glove.set(FmabComponents.GLOVE_CIRCLE, circle(p, "fmab:air", "fmab:projeter"));
				FmabAttachments.GLOVES.set(p, Gloves.NONE.with(false, glove));
				Concentration.set(p, Concentration.max(p));
				GloveCasting.cast(p, false);
				return FmabAttachments.GLOVES.getOrCreate(p).get(false).getDamageValue();
			});
			check(worn > 0, "un cercle d'air brodé sur les gants de Mustang devrait partir face au ciel");
		}
	}

	private static Drawing circle(ServerPlayer p, String element, String action) {
		AlchemyRules rules = AlchemyRules.of(p.level().registryAccess());
		Glyph e = rules.glyph(element).orElseThrow();
		Glyph a = rules.glyph(action).orElseThrow();
		return SimpleCircles.of(e, a, SimpleCircles.sidesFor(e, a));
	}

	/** Joint les mains sur ce cercle et rend le geste joué (CLAP s'il a eu lieu, NONE sinon). */
	private static String clap(ServerPlayer p, String element, String action) {
		p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
		// Une Pierre vivante joint les mains sans avoir vu la Porte : le rang d'Initié déclencherait des
		// progrès qui mettent la partie en pause.
		FmabAttachments.ALCHEMIST.set(p, AlchemistData.NEW.withRank(Rank.ALCHEMIST));
		FmabAttachments.LIVING_STONE.set(p, 50);
		FmabAttachments.POSE.set(p, TransmutationPose.NONE);
		Concentration.set(p, Concentration.max(p));
		Drawing drawing = circle(p, element, action);
		Notebooks.add(p, element, drawing);
		check(Notebooks.selected(p).equals(drawing), "le cercle devrait être sélectionné dans le carnet");
		GloveCasting.cast(p, true);
		TransmutationPose pose = FmabAttachments.POSE.get(p);
		return pose == null || !pose.active(p.level().getGameTime()) ? "NONE" : pose.kind().name();
	}
}
