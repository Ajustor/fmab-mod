package com.ajustor.fmab.gametest;

import com.ajustor.fmab.Fmab;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

/**
 * Les blocs du mod, chacun dans sa vignette : trois exemplaires en équerre (pour voir comment les
 * textures se raccordent), vus de près de trois quarts. Puis tous ensemble, et de nuit ceux qui
 * luisent. Lancer avec {@code ./gradlew runClientGameTest -PfmabGametest=blocks}.
 */
public class BlockScenes implements FabricClientGameTest {
	/** Le sol du monde plat : l'herbe est en -61, on pose dessus. */
	private static final int Y = -60;
	/** L'écart entre deux vignettes. */
	private static final int STEP = 6;

	/** Les blocs à montrer : tous ceux du mod, sauf le cercle (il n'existe que tracé sur une surface). */
	private static List<Identifier> blocks() {
		return BuiltInRegistries.BLOCK.keySet().stream()
				.filter(id -> id.getNamespace().equals(Fmab.MOD_ID) && !id.getPath().equals("transmutation_circle"))
				.sorted()
				.toList();
	}

	private static void set(TestSingleplayerContext sp, int x, int y, int z, String block) {
		sp.getServer().runCommand("setblock %d %d %d %s".formatted(x, y, z, block));
	}

	/** Les blocs qui luisent : on les revoit de nuit. */
	private static final List<String> GLOWING = List.of("blood_crest", "father_seal", "gate_darkness", "red_stone_ore");
	/** Les meubles : un seul suffit. */
	private static final List<String> FURNITURE = List.of("alchemist_table", "automail_bench");

	/**
	 * Une vignette : le bloc, un voisin à droite et un autre par-dessus ; les tuyaux font un coude, les
	 * meubles restent seuls.
	 */
	private static void vignette(TestSingleplayerContext sp, int x, String id) {
		sp.getServer().runCommand("fill %d %d -2 %d %d 2 minecraft:polished_deepslate".formatted(x - 2, Y - 1, x + 3, Y - 1));
		if (id.endsWith("father_pipe")) {
			for (int i = -1; i <= 1; i++) {
				set(sp, x + i, Y, 0, id + "[axis=x]");
			}
			set(sp, x + 2, Y, 0, id + "[axis=y]");
			set(sp, x + 2, Y + 1, 0, id + "[axis=y]");
			set(sp, x, Y, 1, id + "[axis=z]");
			return;
		}
		set(sp, x, Y, 0, id);
		if (FURNITURE.contains(id.substring(id.indexOf(':') + 1))) {
			return;
		}
		set(sp, x + 1, Y, 0, id);
		set(sp, x, Y + 1, 0, id);
	}

	/** Se poste à (x, y, z) et regarde le point (tx, ty, tz). */
	private static void look(TestSingleplayerContext sp, double x, double y, double z, double tx, double ty, double tz) {
		double dx = tx - x, dy = ty - y, dz = tz - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
		sp.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y - 1.62, z, yaw, pitch));
	}

	/** De près, de trois quarts ; un meuble seul se cadre plus serré. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext sp, int i, String path, String suffix) {
		int x = i * STEP;
		if (FURNITURE.contains(path)) {
			look(sp, x - 0.8, Y + 1.6, -0.9, x + 0.5, Y + 0.6, 0.5);
		} else {
			look(sp, x - 1.4, Y + 2.4, -2.3, x + 0.9, Y + 0.6, 0.6);
		}
		context.waitTicks(i == 0 && suffix.isEmpty() ? 60 : 12);
		context.takeScreenshot("blocks_%02d_%s%s".formatted(i, path, suffix));
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("blocks")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("weather clear");
			List<Identifier> ids = blocks();
			for (int i = 0; i < ids.size(); i++) {
				vignette(sp, i * STEP, ids.get(i).toString());
			}
			// En spectateur : il reste où on le pose, sans tomber.
			sp.getServer().runCommand("gamemode spectator @p");
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
			for (int i = 0; i < ids.size(); i++) {
				shoot(context, sp, i, ids.get(i).getPath(), "");
			}
			double mid = (ids.size() - 1) * STEP / 2.0;
			double far = ids.size() * STEP * 0.7;
			look(sp, mid, Y + far * 0.45, -far, mid, Y, 1);
			context.waitTicks(40);
			context.takeScreenshot("blocks_all_day");
			sp.getServer().runCommand("time set 18000");
			for (int i = 0; i < ids.size(); i++) {
				if (GLOWING.contains(ids.get(i).getPath())) {
					shoot(context, sp, i, ids.get(i).getPath(), "_night");
				}
			}
		}
	}
}
