package com.ajustor.fmab.compat;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * La génération du monde, là où sa forme change d'une version de Minecraft à l'autre (voir
 * ARCHITECTURE.md).
 */
public final class WorldgenCompat {
	private WorldgenCompat() {
	}

	/** Le biome de ce chunk devient celui-ci partout où {@code inside} accepte la colonne (x, z en blocs). */
	public static void paint(ChunkAccess chunk, Holder<Biome> biome, java.util.function.BiPredicate<Integer, Integer> inside) {
		//? if >=26.3 {
		/*chunk.fillBiomesFromNoise((qx, qy, qz) -> inside.test(QuartPos.toBlock(qx), QuartPos.toBlock(qz))
				? biome : chunk.getNoiseBiome(qx, qy, qz));
		*///?} else {
		chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> inside.test(QuartPos.toBlock(qx), QuartPos.toBlock(qz))
				? biome : chunk.getNoiseBiome(qx, qy, qz), null);
		//?}
	}

	/** Le biome qu'aura ce point, d'après le bruit du monde (le chunk n'a pas besoin d'exister). */
	public static Holder<Biome> biomeAt(Structure.GenerationContext context, BlockPos pos) {
		int x = QuartPos.fromBlock(pos.getX()), y = QuartPos.fromBlock(pos.getY()), z = QuartPos.fromBlock(pos.getZ());
		//? if >=26.3 {
		/*return context.biomeResolver().getNoiseBiome(x, y, z);
		*///?} else {
		return context.biomeSource().getNoiseBiome(x, y, z, context.randomState().sampler());
		//?}
	}
}
