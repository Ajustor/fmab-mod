package com.ajustor.fmab.gametest;

import com.ajustor.fmab.entity.LustEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Les tempéraments des personnages : chacun au repos, puis en garde, puis en marchant vers la
 * caméra ; et le geste des doigts-lames de Lust, qui vise puis frappe.
 * Lancer avec {@code ./gradlew runClientGameTest -PfmabGametest=animations}.
 */
public class AnimationScenes implements FabricClientGameTest {
	private static final List<String> FIGURES = List.of("lust", "gluttony", "sloth", "greed", "wrath", "envy", "pride",
			"izumi", "winry", "may_chang", "olivier", "hohenheim", "marcoh", "cornello", "scar", "amestrian_soldier",
			"immortal_soldier");
	private static final int Y = -60;
	private static final int STEP = 3;

	private static void look(TestSingleplayerContext sp, double x, double y, double z, double tx, double ty, double tz) {
		double dx = tx - x, dy = ty - y, dz = tz - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
		sp.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y - 1.62, z, yaw, pitch));
	}

	/** Chacun de près, de trois quarts ; Sloth, géant, avec plus de recul. */
	private static void row(ClientGameTestContext context, TestSingleplayerContext sp, String name) {
		for (int i = 0; i < FIGURES.size(); i++) {
			double x = i * STEP + 0.5;
			double k = FIGURES.get(i).equals("sloth") ? 2.2 : 1;
			look(sp, x - 1.6 * k, Y + 1.9 * k, 0.5 - 2.8 * k, x, Y + 1.0 * k, 0.5);
			context.waitTicks(3);
			context.takeScreenshot("anim_%s_%02d_%s".formatted(name, i, FIGURES.get(i)));
		}
	}

	/**
	 * Chacun fait quelques pas vers la caméra (un vrai déplacement, que le client anime comme une
	 * marche), et on le photographie en route, à deux moments du pas ; puis il revient à sa place.
	 */
	private static void walk(ClientGameTestContext context, TestSingleplayerContext sp) {
		for (int i = 0; i < FIGURES.size(); i++) {
			int index = i;
			double x = i * STEP + 0.5;
			double k = FIGURES.get(i).equals("sloth") ? 2.2 : 1;
			for (int t = 1; t <= 14; t++) {
				double z = 0.5 - t * 0.12;
				sp.getServer().runOnServer(server -> mob(server, index).move(MoverType.SELF, new Vec3(0, 0, -0.12)));
				look(sp, x - 1.6 * k, Y + 1.9 * k, z - 2.8 * k, x, Y + 1.0 * k, z);
				context.waitTick();
				if (t == 10 || t == 14) {
					context.takeScreenshot("anim_2_walk_%s_%02d_%s".formatted(t == 10 ? "a" : "b", i, FIGURES.get(i)));
				}
			}
			sp.getServer().runOnServer(server -> mob(server, index).teleportTo(x, Y, 0.5));
		}
	}

	/** Le personnage posé à cette place du rang. */
	private static Mob mob(MinecraftServer server, int index) {
		double x = index * STEP + 0.5;
		return server.overworld().getEntitiesOfClass(Mob.class, new AABB(x - 1, Y - 1, -4, x + 1, Y + 4, 2)).getFirst();
	}

	@SuppressWarnings("unchecked")
	private static void lance(TestSingleplayerContext sp, LustEntity.Lance lance) {
		sp.getServer().runOnServer(server -> server.overworld().getEntities(com.ajustor.fmab.registry.FmabEntities.LUST,
				e -> true).forEach(lust -> {
			try {
				var field = LustEntity.class.getDeclaredField("LANCE_STATE");
				field.setAccessible(true);
				lust.getEntityData().set((EntityDataAccessor<Integer>) field.get(null), lance.ordinal());
			} catch (ReflectiveOperationException ex) {
				throw new AssertionError(ex);
			}
		}));
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("animations")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("gamerule doMobSpawning false");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("gamemode spectator @p");
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
			for (int i = 0; i < FIGURES.size(); i++) {
				sp.getServer().runCommand(String.format(Locale.ROOT,
						"summon fmab:%s %d.5 %d 0.5 {NoAI:1b,NoGravity:1b,Silent:1b,Invulnerable:1b,Rotation:[180f,0f]}",
						FIGURES.get(i), i * STEP, Y));
			}
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(40);
			row(context, sp, "0_idle");

			sp.getServer().runOnServer(server -> server.overworld().getAllEntities().forEach(e -> {
				if (e instanceof Mob mob) {
					mob.setAggressive(true);
				}
			}));
			context.waitTicks(10);
			row(context, sp, "1_guard");

			sp.getServer().runOnServer(server -> server.overworld().getAllEntities().forEach(e -> {
				if (e instanceof Mob mob) {
					mob.setAggressive(false);
				}
			}));
			walk(context, sp);

			// Lust vise, puis frappe : de près, de trois quarts.
			look(sp, -2, Y + 2.0, -2.6, 0.5, Y + 1.2, 0.5);
			lance(sp, LustEntity.Lance.AIMING);
			context.waitTicks(10);
			context.takeScreenshot("anim_3_lust_aiming");
			lance(sp, LustEntity.Lance.STRIKING);
			context.waitTicks(4);
			context.takeScreenshot("anim_4_lust_striking");
			look(sp, -3.5, Y + 1.6, 0.5, 0.5, Y + 1.2, 0.5);
			context.waitTicks(4);
			context.takeScreenshot("anim_5_lust_striking_side");
		}
	}
}
