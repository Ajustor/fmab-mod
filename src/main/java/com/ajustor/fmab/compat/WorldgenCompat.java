package com.ajustor.fmab.compat;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.function.Predicate;

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

	/**
	 * Le contexte dans lequel la génération pose les structures de ce monde, hors de toute génération :
	 * pour savoir, sans rien bâtir, où un lieu se poserait.
	 */
	public static Structure.GenerationContext context(ServerLevel level, ChunkPos chunk, Predicate<Holder<Biome>> validBiome) {
		ServerChunkCache chunks = level.getChunkSource();
		ChunkGenerator generator = chunks.getGenerator();
		//? if >=26.3 {
		/*return new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(),
				chunks.randomState().createClimateSampler(
						net.minecraft.world.level.levelgen.densityfunction.SamplerContext.builder().enableCaches().build()),
				chunks.randomState(), level.getStructureTemplateManager(), level.getSeed(),
				chunk, level, validBiome);
		*///?} else {
		return new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(),
				chunks.randomState(), level.getStructureManager(), level.getSeed(), chunk, level, validBiome);
		//?}
	}
}
