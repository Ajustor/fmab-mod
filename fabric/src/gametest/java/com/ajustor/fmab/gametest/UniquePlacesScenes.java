package com.ajustor.fmab.gametest;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.world.Maps;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les lieux de l'histoire sont uniques : de deux points éloignés du monde, chercher Central, Resembool,
 * Briggs… mène au même endroit (un seul Père, une seule ville natale), et leurs cartes y mènent sans
 * fouiller le monde. Les points de sang, eux, restent nombreux. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=unique_places}.
 */
public class UniquePlacesScenes implements FabricClientGameTest {
	private static final List<String> UNIQUE = List.of("central", "resembool", "dublith", "rush_valley", "liore",
			"marcoh_clinic", "laboratory_5", "devils_nest", "ishval_ruins", "xerxes_ruins", "xing", "fort_briggs",
			"yock_island");
	private static final BlockPos HERE = new BlockPos(0, 64, 0);
	private static final BlockPos FAR = new BlockPos(6000, 64, -6000);
	/** Au-delà, une carte fige le serveur : le premier calcul d'un lieu se fait en quelques secondes au plus. */
	private static final long SLOW_MAP_MS = 5000;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("unique_places")) {
			return;
		}
		// Un monde normal (les mondes de test sont plats, sans nos lieux), à graine fixe.
		try (TestSingleplayerContext sp = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(c -> {
			c.setWorldType(c.getNormalPresetList().getFirst());
			c.setSeed("fmab");
		}).create()) {
			for (String name : UNIQUE) {
				BlockPos[] found = sp.getServer().computeOnServer(server -> {
					ServerLevel level = server.overworld();
					return new BlockPos[]{locate(level, name, HERE), locate(level, name, FAR)};
				});
				check(found[0] != null, name + " devrait exister dans le monde");
				check(found[0].equals(found[1]), name + " devrait être unique : " + found[0] + " et " + found[1]);
				Fmab.LOGGER.info("Lieu unique {} : {}", name, found[0]);
			}
			// Les cartes trouvent chaque lieu par sa place connue, sans fouiller le monde : le même chunk
			// que la recherche du jeu, de partout, et vite (la recherche du jeu figeait le serveur).
			long start = System.nanoTime();
			for (String name : UNIQUE) {
				BlockPos[] mapped = sp.getServer().computeOnServer(server -> new BlockPos[]{
						map(server.overworld(), name, HERE), map(server.overworld(), name, FAR),
						locate(server.overworld(), name, HERE)});
				check(mapped[0] != null && mapped[1] != null, "la carte de " + name + " devrait mener quelque part");
				check(sameChunk(mapped[0], mapped[2]) && sameChunk(mapped[1], mapped[2]),
						"la carte de " + name + " devrait mener à " + mapped[2] + ", pas à " + mapped[0] + " / " + mapped[1]);
			}
			long ms = (System.nanoTime() - start) / 1_000_000;
			Fmab.LOGGER.info("Cartes des lieux uniques (avec la recherche du jeu en témoin) : {} ms", ms);
			BlockPos[] crests = sp.getServer().computeOnServer(server -> new BlockPos[]{
					locate(server.overworld(), "blood_crest", HERE), locate(server.overworld(), "blood_crest", FAR)});
			check(crests[0] != null && crests[1] != null && !crests[0].equals(crests[1]),
					"les points de sang devraient rester nombreux");
		}
	}

	/** Où mène une carte de ce lieu, achetée depuis ce point. */
	private static BlockPos map(ServerLevel level, String name, BlockPos from) {
		long start = System.nanoTime();
		BlockPos found = Maps.find(level, from,
				TagKey.create(Registries.STRUCTURE, Fmab.id("on_" + name + "_maps"))).orElse(null);
		long ms = (System.nanoTime() - start) / 1_000_000;
		check(ms < SLOW_MAP_MS, "la carte de " + name + " a pris " + ms + " ms");
		return found;
	}

	private static boolean sameChunk(BlockPos a, BlockPos b) {
		return a != null && b != null && a.getX() >> 4 == b.getX() >> 4 && a.getZ() >> 4 == b.getZ() >> 4;
	}

	private static BlockPos locate(ServerLevel level, String name, BlockPos from) {
		Holder<Structure> structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
				.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Fmab.id(name)));
		Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
				.findNearestMapStructure(level, HolderSet.direct(structure), from, 100, false);
		return found == null ? null : found.getFirst();
	}
}
