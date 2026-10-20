package com.ajustor.fmab.gametest;

import com.ajustor.fmab.client.screen.KnowledgeScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * L'arbre de savoir tient dans la fenêtre : ses huit écoles se rangent en grille au lieu de
 * s'aligner sur une seule rangée plus large que l'écran, et le bas de la grille se lit en défilant.
 * Lancer avec {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=knowledge_screen}.
 */
public class KnowledgeScreenScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("knowledge_screen")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.setScreen(() -> new KnowledgeScreen(null));
			context.waitTicks(2);
			check(context.computeOnClient(client -> ((KnowledgeScreen) client.gui.screen()).fitsOnScreen()),
					"l'arbre de savoir ne devrait pas déborder de l'écran");
			context.takeScreenshot("knowledge_screen_top");

			context.runOnClient(client -> client.gui.screen().mouseScrolled(0, 0, 0, -100));
			context.waitTicks(2);
			context.takeScreenshot("knowledge_screen_scrolled");
			context.setScreen(() -> null);
		}
	}
}
