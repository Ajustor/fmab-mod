package com.ajustor.fmab.gametest;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.Treatise;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.client.screen.CircleWheelScreen;
import com.ajustor.fmab.client.screen.NotebookScreen;
import com.ajustor.fmab.client.screen.TreatiseScreen;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Le carnet se remplit depuis le Traité sans figer le jeu : chaque cercle d'exemple y est recopié
 * d'un clic, chronométré, bien au-delà des seize pages d'autrefois. Son sommaire liste alors toutes les
 * pages et en ouvre une d'un clic, et la roue des cercles les range neuf par neuf, roue après roue.
 * Lancer avec {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=notebook_contents}.
 */
public class NotebookContentsScenes implements FabricClientGameTest {
	/** Au-delà, un clic sur « Ajouter au carnet » passe pour un gel. */
	private static final long SLOW_MS = 2000;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("notebook_contents")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			context.waitTicks(20);
			// Tous les chapitres ouverts : un Alchimiste d'État aguerri.
			sp.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				FmabAttachments.ALCHEMIST.set(p, FmabAttachments.ALCHEMIST.getOrCreate(p).withRank(Rank.STATE)
						.addMastery("earth", 1000));
			});
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(5);

			context.setScreen(TreatiseScreen::new);
			context.waitTicks(2);
			long worst = 0;
			String slowest = "";
			int copied = 0;
			for (String key : examples(context)) {
				if (!context.computeOnClient(mc -> ((TreatiseScreen) mc.gui.screen()).goToPage(key))) {
					continue;
				}
				context.waitTick();
				long start = System.nanoTime();
				boolean pressed = context.computeOnClient(mc -> ((TreatiseScreen) mc.gui.screen()).copyToNotebook());
				context.waitTicks(3);
				long ms = (System.nanoTime() - start) / 1_000_000;
				Fmab.LOGGER.info("[notebook_contents] {} : {} ms", key, ms);
				if (pressed) {
					copied++;
				}
				if (ms > worst) {
					worst = ms;
					slowest = key;
				}
			}
			check(copied > 20, "le Traité devrait proposer plus de vingt cercles à recopier, pas " + copied);
			check(worst < SLOW_MS, "recopier « " + slowest + " » a pris " + worst + " ms");
			int pages = context.computeOnClient(mc -> Notebooks.of(mc.player).pages().size());
			check(pages > 16, "le carnet devrait tenir plus de seize cercles, il a " + pages + " pages");
			context.takeScreenshot("notebook_contents_treatise");

			// Le sommaire : toutes les pages, par leur nom ; un clic en ouvre une.
			context.setScreen(NotebookScreen::new);
			context.waitTicks(2);
			context.runOnClient(mc -> ((NotebookScreen) mc.gui.screen()).toggleContents());
			context.waitTicks(12);
			check(context.computeOnClient(mc -> ((NotebookScreen) mc.gui.screen()).contentsOpen()),
					"le sommaire devrait s'ouvrir");
			check(context.computeOnClient(mc -> ((NotebookScreen) mc.gui.screen()).pageCount()) == pages,
					"le sommaire devrait tenir toutes les pages");
			context.takeScreenshot("notebook_contents_index");
			context.runOnClient(mc -> ((NotebookScreen) mc.gui.screen()).openPage(20));
			context.waitTicks(2);
			check(context.computeOnClient(mc -> ((NotebookScreen) mc.gui.screen()).openedPage()) == 20,
					"un clic au sommaire devrait ouvrir la page 21");
			check(!context.computeOnClient(mc -> ((NotebookScreen) mc.gui.screen()).contentsOpen()),
					"le sommaire devrait se refermer sur la page ouverte");
			context.takeScreenshot("notebook_contents_page");

			// Le sélecteur de glyphes : tous compris, on trouve le tampon par son nom au lieu de les faire
			// défiler un à un, et le dernier posé revient en tête.
			sp.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				AlchemistData data = FmabAttachments.ALCHEMIST.getOrCreate(p);
				for (Glyph g : AlchemyRules.of(server.registryAccess()).glyphs()) {
					data = data.learn(g.id());
				}
				FmabAttachments.ALCHEMIST.set(p, data);
			});
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(5);
			context.runOnClient(mc -> ((NotebookScreen) mc.gui.screen()).togglePicker());
			context.waitTicks(12);
			check(context.computeOnClient(mc -> ((NotebookScreen) mc.gui.screen()).pickerOpen()),
					"le sélecteur de glyphes devrait s'ouvrir");
			int known = context.computeOnClient(mc -> ((NotebookScreen) mc.gui.screen()).searchGlyphs("").size());
			check(known > 20, "le sélecteur devrait montrer tous les glyphes compris, pas " + known);
			context.takeScreenshot("notebook_contents_picker");
			List<String> found = context.computeOnClient(mc -> ((NotebookScreen) mc.gui.screen()).searchGlyphs("netherite"));
			check(found.equals(List.of("fmab:netherite")), "chercher « netherite » devrait le trouver seul : " + found);
			context.waitTicks(2);
			context.takeScreenshot("notebook_contents_picker_search");
			check(context.computeOnClient(mc -> ((NotebookScreen) mc.gui.screen()).pickFirst()),
					"Entrée devrait choisir le glyphe trouvé");
			context.waitTicks(2);
			check(!context.computeOnClient(mc -> ((NotebookScreen) mc.gui.screen()).pickerOpen()),
					"choisir un glyphe devrait rendre la grille");
			check("fmab:netherite".equals(context.computeOnClient(mc -> ((NotebookScreen) mc.gui.screen()).stampInHand())),
					"le tampon devrait porter le glyphe choisi");
			context.runOnClient(mc -> ((NotebookScreen) mc.gui.screen()).togglePicker());
			context.waitTicks(2);
			List<String> again = context.computeOnClient(mc -> ((NotebookScreen) mc.gui.screen()).searchGlyphs(""));
			check(again.getFirst().equals("fmab:netherite"), "le dernier glyphe posé devrait venir en tête : " + again);
			context.setScreen(() -> null);
			context.waitTicks(2);

			// La roue des cercles : neuf places par roue, la molette passe à la suivante.
			int wheels = context.computeOnClient(mc -> Notebooks.of(mc.player).wheels());
			check(wheels >= 2, "plus de neuf cercles devraient remplir plusieurs roues : " + wheels);
			context.runOnClient(mc -> CircleWheelScreen.open(mc.options.keyUse));
			context.waitTicks(12);
			context.takeScreenshot("notebook_contents_wheel");
			context.runOnClient(mc -> mc.gui.screen().mouseScrolled(0, 0, 0, -1));
			context.waitTicks(4);
			context.takeScreenshot("notebook_contents_wheel_2");
			context.setScreen(() -> null);
		}
	}

	/** Les textes des pages d'exemple du Traité, dans l'ordre du livre. */
	private static List<String> examples(ClientGameTestContext context) {
		return context.computeOnClient(mc -> {
			List<String> out = new ArrayList<>();
			try (Reader reader = mc.getResourceManager().openAsReader(Fmab.id("treatise/treatise.json"))) {
				for (Treatise.Chapter c : Treatise.parse(JsonParser.parseReader(reader).getAsJsonObject()).chapters()) {
					for (Treatise.Page page : c.pages()) {
						if (page instanceof Treatise.ExamplePage e) {
							out.add(e.textKey());
						}
					}
				}
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			return out;
		});
	}
}
