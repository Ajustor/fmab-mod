package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.client.screen.TreatiseScreen;
import com.ajustor.fmab.registry.FmabAttachments;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Le catalogue du Traité se complète au fil du savoir : un nouveau venu y voit la Terre (les
 * Rudiments sont acquis), mais pas l'Humain, qui reste une page blanche jusqu'à ce qu'il le trace.
 * Ses chapitres aussi se dévoilent : « Penser en étages » attend le rang d'Alchimiste.
 * Lancer avec {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=treatise_catalogue}.
 */
public class TreatiseCatalogueScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("treatise_catalogue")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			context.waitTicks(20);
			check(open(context, "fmab:terre"), "la Terre, apprise dans les Rudiments, devrait se lire");
			context.takeScreenshot("treatise_catalogue_known");
			check(!open(context, "fmab:humain"), "l'Humain ne devrait pas se lire avant d'être appris ou tracé");
			context.takeScreenshot("treatise_catalogue_unknown");
			context.setScreen(() -> null);

			// Tracé une fois : sa page se remplit (il n'est pas encore compris pour autant).
			sp.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				FmabAttachments.ALCHEMIST.set(p, FmabAttachments.ALCHEMIST.getOrCreate(p).practiceGlyph("fmab:humain"));
			});
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(5);
			check(open(context, "fmab:humain"), "l'Humain tracé une fois devrait se lire");
			context.takeScreenshot("treatise_catalogue_drawn_once");
			context.setScreen(() -> null);

			// Le sommaire d'un Apprenti : les chapitres d'Alchimiste restent scellés.
			check(contents(context, "treatise.fmab.design.title"), "la conception des cercles devrait s'ouvrir à tous");
			check(!contents(context, "treatise.fmab.stages.title"), "les étages devraient attendre le rang d'Alchimiste");
			context.takeScreenshot("treatise_contents_apprentice");
			context.setScreen(() -> null);
			sp.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				FmabAttachments.ALCHEMIST.set(p, FmabAttachments.ALCHEMIST.getOrCreate(p).withRank(Rank.ALCHEMIST));
			});
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(5);
			check(contents(context, "treatise.fmab.stages.title"), "un Alchimiste devrait lire les étages");
			check(!contents(context, "treatise.fmab.mastery.title"), "les cercles de maître devraient attendre l'État");
			context.takeScreenshot("treatise_contents_alchemist");
			context.setScreen(() -> null);
		}
	}

	/** Ouvre le Traité au sommaire : ce chapitre s'ouvre-t-il ? */
	private static boolean contents(ClientGameTestContext context, String chapter) {
		context.setScreen(TreatiseScreen::new);
		context.waitTick();
		return context.computeOnClient(client -> ((TreatiseScreen) client.gui.screen()).opens(chapter));
	}

	/** Ouvre le Traité à la page de ce glyphe : se lit-elle en entier ? */
	private static boolean open(ClientGameTestContext context, String glyph) {
		context.setScreen(TreatiseScreen::new);
		context.runOnClient(client -> ((TreatiseScreen) client.gui.screen()).goToGlyph(glyph));
		context.waitTick();
		return context.computeOnClient(client -> ((TreatiseScreen) client.gui.screen()).showsRevealedGlyph());
	}
}
