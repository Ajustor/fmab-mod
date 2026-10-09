package com.ajustor.fmab.world;

import com.ajustor.fmab.Fmab;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/**
 * Les lieux du mod se tiennent à distance les uns des autres. L'exclusion d'un ensemble de
 * structures du jeu ne vise qu'un seul autre ensemble ; celle-ci en vise autant qu'on veut, de la
 * même façon : il suffit qu'un emplacement possible de l'autre lieu (bâti ou non) soit à moins de
 * tant de chunks pour renoncer.
 */
final class Clearance {
	/** Central tient trop de place pour qu'un autre lieu s'y pose. */
	static final ResourceKey<StructureSet> CENTRAL = ResourceKey.create(Registries.STRUCTURE_SET, Fmab.id("central"));
	/** Rayon de la ville (75 blocs) plus celui d'un autre lieu, en chunks. */
	static final int CENTRAL_CHUNKS = 8;

	private Clearance() {
	}

	static boolean nearCentral(Structure.GenerationContext context) {
		return near(context, CENTRAL, CENTRAL_CHUNKS);
	}

	/** Un emplacement possible de cet ensemble est-il à moins de {@code chunks} chunks d'ici ? */
	static boolean near(Structure.GenerationContext context, ResourceKey<StructureSet> set, int chunks) {
		StructureSet structures = context.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(set);
		if (structures == null || !(structures.placement() instanceof RandomSpreadStructurePlacement spread)) {
			return false;
		}
		ChunkPos here = context.chunkPos();
		int spacing = spread.spacing();
		int gx = Math.floorDiv(here.x(), spacing), gz = Math.floorDiv(here.z(), spacing);
		// Un emplacement par case de la grille : les cases voisines suffisent (chunks < espacement).
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				ChunkPos other = spread.getPotentialStructureChunk(context.seed(), (gx + i) * spacing, (gz + j) * spacing);
				if (Math.abs(other.x() - here.x()) <= chunks && Math.abs(other.z() - here.z()) <= chunks) {
					return true;
				}
			}
		}
		return false;
	}
}
