package com.ajustor.fmab.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

import java.util.List;
import java.util.Locale;

/**
 * Les personnages du mod, en rang, pour juger leurs silhouettes : chacun de trois quarts, presque de
 * profil (à hauteur de buste) et de dos, de près, puis toute la troupe. Lancer avec {@code ./gradlew runClientGameTest -PfmabGametest=figures}.
 */
public class FigureScenes implements FabricClientGameTest {
	private static final List<String> FIGURES = List.of("lust", "izumi", "winry", "olivier", "may_chang", "hohenheim",
			"amestrian_soldier", "briggs_soldier", "drachma_soldier", "state_examiner", "scar", "marcoh", "cornello",
			"envy", "greed", "wrath", "gluttony", "sloth");
	private static final int Y = -60;
	private static final int STEP = 4;

	private static void look(TestSingleplayerContext sp, double x, double y, double z, double tx, double ty, double tz) {
		double dx = tx - x, dy = ty - y, dz = tz - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
		sp.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y - 1.62, z, yaw, pitch));
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("figures")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("gamemode spectator @p");
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
			for (int i = 0; i < FIGURES.size(); i++) {
				// Face au nord, vers la caméra qui les regarde depuis le nord.
				sp.getServer().runCommand(String.format(Locale.ROOT,
						"summon fmab:%s %d.5 %d 0.5 {NoAI:1b,NoGravity:1b,Silent:1b,Invulnerable:1b,Rotation:[180f,0f]}",
						FIGURES.get(i), i * STEP, Y));
			}
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(40);
			for (int i = 0; i < FIGURES.size(); i++) {
				double x = i * STEP + 0.5;
				look(sp, x - 1.5, Y + 2.0, -2.2, x, Y + 1.05, 0.5);
				context.waitTicks(8);
				context.takeScreenshot("figure_%02d_%s_0_front".formatted(i, FIGURES.get(i)));
				look(sp, x - 2.4, Y + 1.35, -0.9, x, Y + 1.1, 0.5);
				context.waitTicks(8);
				context.takeScreenshot("figure_%02d_%s_1_side".formatted(i, FIGURES.get(i)));
				look(sp, x + 1.6, Y + 1.9, 2.8, x, Y + 1.05, 0.5);
				context.waitTicks(8);
				context.takeScreenshot("figure_%02d_%s_2_back".formatted(i, FIGURES.get(i)));
			}
			double mid = (FIGURES.size() - 1) * STEP / 2.0 + 0.5;
			look(sp, mid, Y + 6, -26, mid, Y + 1, 0.5);
			context.waitTicks(20);
			context.takeScreenshot("figure_all");
		}
	}
}
