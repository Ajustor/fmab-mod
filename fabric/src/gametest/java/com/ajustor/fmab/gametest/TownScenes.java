package com.ajustor.fmab.gametest;

import com.ajustor.fmab.world.ProceduralPiece;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Locale;

/**
 * Les lieux du mod, posés un à un loin les uns des autres (/place structure), vus d'avion, de trois
 * quarts puis de la rue. Lancer avec {@code ./gradlew runClientGameTest -PfmabGametest=towns} ;
 * ajouter {@code -PfmabTowns=central,liore} pour n'en montrer que certains.
 */
public class TownScenes implements FabricClientGameTest {
	/** Le sol du monde plat : l'herbe est en -61, on bâtit dessus. */
	private static final int GROUND = -60;
	/** Jusqu'où chercher l'emprise d'un lieu autour de l'endroit où on l'a posé. */
	private static final int SCAN = 110;

	private static final List<String> SITES = List.of("central", "resembool", "rush_valley", "dublith", "liore",
			"xing", "fort_briggs", "laboratory_5", "devils_nest", "marcoh_clinic", "ishval_ruins", "xerxes_ruins",
			"blood_crest", "yock_island");

	/** L'emprise d'un lieu posé : son centre et sa demi-largeur. */
	private record Footprint(double x, double z, double half) {
	}

	/** Le temps que les tronçons s'affichent ; s'ils traînent, on photographie quand même. */
	private static void settle(ClientGameTestContext context) {
		try {
			context.waitFor(mc -> mc.levelRenderer.hasRenderedAllSections(), 300);
		} catch (AssertionError e) {
			// Tant pis : quelques tronçons lointains manqueront.
		}
		context.waitTicks(10);
	}

	/** Attend que le serveur ait chargé les tronçons autour de (x, z), de quoi y poser un lieu entier. */
	private static void loaded(TestSingleplayerContext sp, int x, int z) {
		int r = 6;
		sp.getServer().waitFor(server -> {
			ServerLevel level = server.overworld();
			for (int i = -r; i <= r; i++) {
				for (int j = -r; j <= r; j++) {
					if (!level.hasChunk((x >> 4) + i, (z >> 4) + j)) {
						return false;
					}
				}
			}
			return true;
		}, 1200);
	}

	private static void tp(TestSingleplayerContext sp, double x, double y, double z, float yaw, float pitch) {
		sp.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %.1f %.1f %.1f %.1f", x, y, z, yaw, pitch));
	}

	/**
	 * Pose le lieu ici. Les lieux du mod refusent certains endroits (près d'un emplacement possible de
	 * Central, ou d'un autre lieu qu'ils évitent) : on essaie alors un peu plus loin.
	 */
	private static boolean place(ClientGameTestContext context, TestSingleplayerContext sp, String id, int x, int[] z) {
		for (int attempt = 0; attempt < 6; attempt++) {
			int at = z[0] = attempt * 240;
			tp(sp, x, -40, at, 0, 0);
			loaded(sp, x, at);
			boolean placed = sp.getServer().computeOnServer(server -> {
				try {
					server.getCommands().getDispatcher().execute(
							"place structure fmab:%s %d %d %d".formatted(id, x, GROUND, at),
							server.createCommandSourceStack().withSuppressedOutput());
					return true;
				} catch (CommandSyntaxException e) {
					return false;
				}
			});
			if (placed) {
				return true;
			}
		}
		return false;
	}

	/** L'île de Yock ne se pose qu'en mer : on creuse un bras de mer et on y bâtit l'île directement. */
	private static void island(MinecraftServer server, int x, int z) {
		ServerLevel level = server.overworld();
		int size = 25;
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
				"fill %d -63 %d %d -61 %d minecraft:water".formatted(x - 30, z - 30, x + 30, z + 30));
		ProceduralPiece piece = new ProceduralPiece("yock_island", new BlockPos(x - size / 2, GROUND, z - size / 2),
				size, 10, size, 4, Rotation.NONE, 1L);
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(),
				piece.getBoundingBox(), new ChunkPos(x >> 4, z >> 4), new BlockPos(x, GROUND, z));
	}

	/** L'emprise du lieu : toutes les colonnes qui dépassent du sol autour de (x, z). */
	private static Footprint footprint(MinecraftServer server, int x, int z) {
		ServerLevel level = server.overworld();
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
		for (int dx = -SCAN; dx <= SCAN; dx++) {
			for (int dz = -SCAN; dz <= SCAN; dz++) {
				if (level.getHeight(Heightmap.Types.WORLD_SURFACE, x + dx, z + dz) > GROUND) {
					minX = Math.min(minX, x + dx);
					maxX = Math.max(maxX, x + dx);
					minZ = Math.min(minZ, z + dz);
					maxZ = Math.max(maxZ, z + dz);
				}
			}
		}
		if (minX > maxX) {
			return new Footprint(x, z, 16);
		}
		return new Footprint((minX + maxX + 1) / 2.0, (minZ + maxZ + 1) / 2.0,
				Math.max(maxX - minX, maxZ - minZ) / 2.0 + 1);
	}

	/**
	 * Où se tenir dans la rue : au sud du centre, à découvert, avec quelques blocs libres devant soi
	 * vers le nord. Rend la position des yeux, ou {@code null}.
	 */
	private static double[] street(MinecraftServer server, Footprint f) {
		ServerLevel level = server.overworld();
		int cx = (int) Math.floor(f.x()), cz = (int) Math.floor(f.z());
		for (int side : new int[]{0, 3, -3, 6, -6, 10, -10}) {
			for (int k = (int) (f.half() * 0.3); k < f.half(); k++) {
				int x = cx + side, z = cz + k;
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
				if (y > GROUND + 2) {
					continue;
				}
				boolean clear = true;
				for (int ahead = 1; ahead <= 4 && clear; ahead++) {
					clear = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z - ahead) <= y + 1;
				}
				if (clear) {
					return new double[]{x + 0.5, y + 1.6, z + 0.5};
				}
			}
		}
		return null;
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("towns")) {
			return;
		}
		String only = System.getProperty("fmab.towns", "");
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("gamerule doMobSpawning false");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("gamemode spectator @p");
			context.runOnClient(mc -> {
				mc.options.renderDistance().set(12);
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
			int index = 0;
			for (String id : SITES) {
				if (!only.isBlank() && !List.of(only.split(",")).contains(id)) {
					continue;
				}
				int x = 1000 + index++ * 700;
				int[] z = {0};
				if (id.equals("yock_island")) {
					tp(sp, x, -40, 0, 0, 0);
					loaded(sp, x, 0);
					sp.getServer().runOnServer(server -> island(server, x, 0));
				} else if (!place(context, sp, id, x, z)) {
					throw new AssertionError("Impossible de poser " + id);
				}
				context.waitTicks(10);
				Footprint f = sp.getServer().computeOnServer(server -> footprint(server, x, z[0]));
				double half = f.half();
				// D'avion : depuis le sud, en plongée sur le centre.
				double dist = half * 1.25 + 10, height = half * 0.95 + 12;
				float pitch = (float) Math.toDegrees(Math.atan2(height, dist));
				tp(sp, f.x(), GROUND + height, f.z() + dist, 180, pitch);
				settle(context);
				context.takeScreenshot("town_%s_0_aerial".formatted(id));
				// De trois quarts, depuis le sud-ouest, regard vers le nord-est.
				double d = half * 0.85 + 8, h = half * 0.45 + 6;
				tp(sp, f.x() - d, GROUND + h, f.z() + d, -135,
						(float) Math.toDegrees(Math.atan2(h - 2, d * Math.sqrt(2))));
				settle(context);
				context.takeScreenshot("town_%s_1_three_quarter".formatted(id));
				// Dans la rue, à hauteur d'homme, face au nord.
				double[] eye = sp.getServer().computeOnServer(server -> street(server, f));
				if (eye != null) {
					tp(sp, eye[0], eye[1], eye[2], 180, 4);
					settle(context);
					context.takeScreenshot("town_%s_2_street".formatted(id));
				}
			}
		}
	}
}
