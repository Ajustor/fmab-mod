package com.ajustor.fmab.world;

import com.ajustor.fmab.compat.WorldgenCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Les lieux de l'histoire n'existent qu'une fois par monde : un seul Central (et donc un seul Père),
 * une seule Resembool… Leur placement reste celui du jeu (une place possible par case d'une grille),
 * mais seule la première place qui convient, en partant de l'origine du monde, est bâtie : le lieu y
 * vérifie ses propres règles (relief, mer, distance à Central) et son biome. Les autres places
 * refusent, et {@code /locate} comme les cartes des cartographes les sautent : ils mènent au seul lieu.
 *
 * <p>Le choix ne dépend que de la graine et du terrain : il est le même à chaque chargement, et
 * calculé une fois par monde, au premier chunk qui le demande.
 */
final class UniqueSite {
	/** Jusqu'où l'on cherche, en anneaux de cases autour de l'origine. */
	static final int MAX_RINGS = 24;

	/** Un lieu, sur un chunk donné, si le terrain s'y prête (sans regarder le biome). */
	@FunctionalInterface
	interface Attempt {
		Optional<Structure.GenerationStub> at(Structure.GenerationContext context, ChunkPos chunk);
	}

	/** Le choix de chaque lieu, par graine de monde. */
	private static final Map<Structure, Map<Long, Optional<ChunkPos>>> CHOSEN = new WeakHashMap<>();

	private UniqueSite() {
	}

	/** Le lieu se bâtit ici seulement si ce chunk est la place choisie (ou si on le pose à la main). */
	static Optional<Structure.GenerationStub> only(Structure structure, Structure.GenerationContext context, Attempt attempt) {
		if (UniqueSites.forced()) {
			return attempt.at(context, context.chunkPos());
		}
		Optional<ChunkPos> chosen = chosen(structure, context, attempt);
		if (chosen.isEmpty() || !chosen.get().equals(context.chunkPos())) {
			return Optional.empty();
		}
		return attempt.at(context, context.chunkPos());
	}

	/** La place choisie pour ce lieu dans ce monde, ou rien si aucune ne convient. */
	static Optional<ChunkPos> chosen(Structure structure, Structure.GenerationContext context, Attempt attempt) {
		Map<Long, Optional<ChunkPos>> bySeed;
		synchronized (CHOSEN) {
			bySeed = CHOSEN.computeIfAbsent(structure, s -> new ConcurrentHashMap<>());
		}
		Optional<ChunkPos> known = bySeed.get(context.seed());
		if (known != null) {
			return known;
		}
		// Calculé hors de toute map verrouillée : la recherche peut en consulter d'autres (Clearance).
		synchronized (bySeed) {
			known = bySeed.get(context.seed());
			if (known == null) {
				known = search(structure, context, attempt);
				bySeed.put(context.seed(), known);
			}
			return known;
		}
	}

	private static Optional<ChunkPos> search(Structure structure, Structure.GenerationContext context, Attempt attempt) {
		Optional<RandomSpreadStructurePlacement> placement = placementOf(structure, context);
		if (placement.isEmpty()) {
			return Optional.empty();
		}
		RandomSpreadStructurePlacement spread = placement.get();
		for (int ring = 0; ring <= MAX_RINGS; ring++) {
			List<ChunkPos> candidates = new ArrayList<>();
			for (int[] cell : ring(ring)) {
				candidates.add(spread.getPotentialStructureChunk(context.seed(), cell[0] * spread.spacing(),
						cell[1] * spread.spacing()));
			}
			candidates.sort(Comparator.comparingLong(UniqueSite::distanceFromOrigin));
			for (ChunkPos chunk : candidates) {
				Optional<Structure.GenerationStub> stub = attempt.at(context, chunk);
				if (stub.isPresent() && context.validBiome().test(WorldgenCompat.biomeAt(context, stub.get().position()))) {
					return Optional.of(chunk);
				}
			}
		}
		return Optional.empty();
	}

	/** Le placement de l'ensemble de structures qui contient ce lieu (une grille du jeu). */
	private static Optional<RandomSpreadStructurePlacement> placementOf(Structure structure, Structure.GenerationContext context) {
		return context.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).listElements()
				.map(holder -> holder.value())
				.filter(set -> set.structures().stream().anyMatch(entry -> entry.structure().value() == structure))
				.map(StructureSet::placement)
				.filter(RandomSpreadStructurePlacement.class::isInstance)
				.map(RandomSpreadStructurePlacement.class::cast)
				.findFirst();
	}

	/** Les cases de la grille à exactement {@code ring} cases de celle de l'origine. */
	static List<int[]> ring(int ring) {
		List<int[]> cells = new ArrayList<>();
		for (int x = -ring; x <= ring; x++) {
			for (int z = -ring; z <= ring; z++) {
				if (Math.max(Math.abs(x), Math.abs(z)) == ring) {
					cells.add(new int[]{x, z});
				}
			}
		}
		return cells;
	}

	static long distanceFromOrigin(ChunkPos chunk) {
		return (long) chunk.x() * chunk.x() + (long) chunk.z() * chunk.z();
	}
}
