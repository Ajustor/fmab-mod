package com.ajustor.fmab.gametest;

import com.ajustor.fmab.client.render.EclipseSky;
import com.ajustor.fmab.stone.Eclipse;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

import java.util.Locale;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Le jour de l'éclipse, la lune traverse le ciel jusqu'au soleil : loin et pâle au matin, elle le mord
 * vers dix heures, le couvre à midi (la couronne autour), puis s'en éloigne l'après-midi. Une capture
 * par heure, le regard entre le soleil et la lune. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=eclipse}.
 */
public class EclipseSkyScenes implements FabricClientGameTest {
	private static final long DAY = 7 * 24000L;
	/** Les heures photographiées (6000 = midi) et ce qu'on doit y voir. */
	private static final long[] HOURS = {1000, 3500, 4400, 4800, 6000, 8400, 10500};

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("eclipse")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("weather clear");
			sp.getConnection().waitForChunksRender();
			context.runOnClient(mc -> RenderShowcase.hideGui(mc, true));
			for (long hour : HOURS) {
				sp.getServer().runCommand("time set " + (DAY + hour));
				look(context, hour);
				context.waitTicks(20);
				String seen = context.computeOnClient(mc -> "lune=" + EclipseSky.active() + " silhouette="
						+ (EclipseSky.mask() > 0) + " couronne=" + (EclipseSky.corona() > 0.5f));
				check(seen.equals(expected(hour)), "à " + hour + ", on devrait voir " + expected(hour) + " : " + seen);
				context.takeScreenshot(String.format(Locale.ROOT, "eclipse_%05d", hour));
			}
			sp.getServer().runCommand("time set " + (DAY - 24000 + 6000));
			context.waitTicks(20);
			check(!context.computeOnClient(mc -> EclipseSky.active()), "la veille, la lune est la lune ordinaire");
			context.runOnClient(mc -> RenderShowcase.hideGui(mc, false));
		}
	}

	/** La lune est là toute la journée ; sa silhouette dès qu'elle approche ; la couronne à la totalité. */
	private static String expected(long hour) {
		float coverage = Eclipse.coverage(DAY + hour);
		boolean near = Math.abs(Eclipse.separation(DAY + hour)) < 2 * (Eclipse.SUN_HALF + Eclipse.MOON_HALF);
		return "lune=true silhouette=" + near + " couronne=" + (Math.pow(coverage, 8) > 0.5);
	}

	/**
	 * Regarde le milieu entre le soleil et la lune. Le soleil se lève à l'est (+X) et monte à la verticale
	 * à midi ; la lune est sur sa route, {@link Eclipse#separation} plus loin.
	 */
	private static void look(ClientGameTestContext context, long hour) {
		double sun = 180.0 * hour / 12000;
		double middle = sun + Math.toDegrees(Eclipse.separation(DAY + hour)) / 2;
		float yaw = middle <= 90 ? -90 : 90;
		float pitch = (float) -(middle <= 90 ? middle : 180 - middle);
		context.runOnClient(mc -> {
			mc.player.setYRot(yaw);
			mc.player.yRotO = yaw;
			mc.player.setXRot(pitch);
			mc.player.xRotO = pitch;
		});
	}
}
