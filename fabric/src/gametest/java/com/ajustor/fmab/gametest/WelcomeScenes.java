package com.ajustor.fmab.gametest;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.registry.FmabItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * L'arrivée d'un nouveau joueur : il reçoit le Traité, un mot d'accueil qui lui dit où lire et quelles
 * touches le guident, et le premier progrès s'annonce. La capture montre le chat à l'arrivée. Lancer
 * avec {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=welcome}.
 */
public class WelcomeScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("welcome")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getConnection().waitForChunksRender();
			context.waitTicks(40);
			context.takeScreenshot("welcome_chat");
			boolean treatise = sp.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst()
					.getInventory().contains(stack -> stack.is(FmabItems.ALCHEMY_TREATISE)));
			check(treatise, "le nouveau venu devrait avoir le Traité");
			String root = sp.getServer().computeOnServer(server -> {
				var holder = server.getAdvancements().get(Fmab.id("alchemist/root"));
				if (holder == null) {
					return "absent";
				}
				var progress = server.getPlayerList().getPlayers().getFirst().getAdvancements().getOrStartProgress(holder);
				//? if >=26.3 {
				/*boolean toast = holder.value().display().map(d -> d.showToast()).orElse(false);
				*///?} else {
				boolean toast = holder.value().display().map(d -> d.shouldShowToast()).orElse(false);
				//?}
				return progress.isDone() && toast ? "ok" : "fait=" + progress.isDone() + " annoncé=" + toast;
			});
			check(root.equals("ok"), "le premier progrès devrait s'annoncer à l'arrivée : " + root);
			// Le mot d'accueil se lit avec les touches réglées par le joueur.
			String greeting = context.computeOnClient(client -> Fmab.welcome().getString());
			check(!greeting.contains("welcome.fmab") && !greeting.contains("key.fmab"),
					"le mot d'accueil devrait être traduit : " + greeting);
		}
	}
}
