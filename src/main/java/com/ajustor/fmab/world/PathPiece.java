package com.ajustor.fmab.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Un chemin d'un bourg, d'une porte à la place, large de deux blocs : il épouse le relief, colonne
 * par colonne, en remplaçant la terre, l'herbe ou le sable de la surface, et des réverbères le
 * bordent de loin en loin. Les maisons, posées après lui, le recouvrent là où elles s'élèvent.
 */
public class PathPiece extends StructurePiece {
	/** La matière d'un chemin et de ses réverbères, selon le bourg. */
	public enum Style {
		/** Terre battue, entre les champs de Resembool. */
		RESEMBOOL(Blocks.OAK_FENCE, Blocks.DIRT_PATH, Blocks.DIRT_PATH, Blocks.DIRT_PATH, Blocks.COARSE_DIRT),
		/** Des pavés, à Dublith. */
		DUBLITH(Blocks.DARK_OAK_FENCE, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.COBBLESTONE, Blocks.POLISHED_ANDESITE),
		/** Du grès, à Rush Valley. */
		RUSH_VALLEY(Blocks.JUNGLE_FENCE, Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.SANDSTONE,
				Blocks.CUT_SANDSTONE);

		private final Block post;
		private final Block[] paving;

		Style(Block post, Block... paving) {
			this.post = post;
			this.paving = paving;
		}
	}

	/** Un réverbère tous les ce nombre de pas, d'un côté puis de l'autre : la rue n'est jamais noire. */
	private static final int LAMP_EVERY = 6;
	/** Le chemin ne cherche pas le sol plus loin que ça de la hauteur du bourg. */
	private static final int REACH = 24;

	private final Style style;
	private final int x0;
	private final int z0;
	private final int x1;
	private final int z1;

	/** Un chemin de (x0, z0) à (x1, z1), autour de la hauteur {@code y} du bourg. */
	public PathPiece(Style style, int x0, int z0, int x1, int z1, int y) {
		super(FmabStructures.PATH_PIECE, 0, new BoundingBox(Math.min(x0, x1) - 3, y - REACH, Math.min(z0, z1) - 3,
				Math.max(x0, x1) + 3, y + REACH, Math.max(z0, z1) + 3));
		this.style = style;
		this.x0 = x0;
		this.z0 = z0;
		this.x1 = x1;
		this.z1 = z1;
	}

	public PathPiece(CompoundTag tag) {
		super(FmabStructures.PATH_PIECE, tag);
		this.style = Style.values()[Math.floorMod(tag.getIntOr("style", 0), Style.values().length)];
		this.x0 = tag.getIntOr("x0", 0);
		this.z0 = tag.getIntOr("z0", 0);
		this.x1 = tag.getIntOr("x1", 0);
		this.z1 = tag.getIntOr("z1", 0);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("style", style.ordinal());
		tag.putInt("x0", x0);
		tag.putInt("z0", z0);
		tag.putInt("x1", x1);
		tag.putInt("z1", z1);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
		int dx = x1 - x0, dz = z1 - z0;
		int steps = Math.max(Math.abs(dx), Math.abs(dz));
		// Large de deux : le pas de côté se fait en travers de la direction principale.
		boolean alongX = Math.abs(dx) >= Math.abs(dz);
		for (int i = 0; i <= steps; i++) {
			int x = x0 + (steps == 0 ? 0 : Math.round((float) dx * i / steps));
			int z = z0 + (steps == 0 ? 0 : Math.round((float) dz * i / steps));
			pave(level, chunkBB, x, z);
			pave(level, chunkBB, alongX ? x : x + 1, alongX ? z + 1 : z);
			if (i % LAMP_EVERY == LAMP_EVERY / 2 && i < steps - 2) {
				// Un réverbère sur le bas-côté, d'un côté puis de l'autre.
				int side = (i / LAMP_EVERY) % 2 == 0 ? -1 : 2;
				lamp(level, chunkBB, alongX ? x : x + side, alongX ? z + side : z);
			}
		}
	}

	/** Le sol naturel de la surface, là où un chemin peut passer. */
	private static boolean natural(BlockState state) {
		return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || NATURAL.contains(state.getBlock());
	}

	/** Ce que les étiquettes ne rangent pas toutes dans la terre ou le sable, selon les versions. */
	private static final java.util.Set<Block> NATURAL = java.util.Set.of(Blocks.GRASS_BLOCK, Blocks.DIRT,
			Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.ROOTED_DIRT, Blocks.SAND, Blocks.RED_SAND,
			Blocks.GRAVEL, Blocks.SNOW_BLOCK);

	/** La surface en (x, z) : le bloc du dessus, s'il est naturel et dans ce chunk, sinon null. */
	private BlockPos surface(WorldGenLevel level, BoundingBox chunkBB, int x, int z) {
		if (!chunkBB.isInside(x, chunkBB.minY(), z)) {
			return null;
		}
		// La surface finale : présente dès la pose des structures en génération, et dans un monde déjà
		// généré (commande /place), où celle de la génération n'est plus tenue à jour.
		BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
		if (Math.abs(top.getY() - boundingBox.getCenter().getY()) > REACH) {
			return null;
		}
		// Sous l'herbe haute ou les fleurs, le vrai sol est un cran plus bas.
		for (int i = 0; i < 3 && level.getBlockState(top).canBeReplaced() && !level.getBlockState(top).isAir(); i++) {
			top = top.below();
		}
		return natural(level.getBlockState(top)) ? top : null;
	}

	private void pave(WorldGenLevel level, BoundingBox chunkBB, int x, int z) {
		BlockPos ground = surface(level, chunkBB, x, z);
		if (ground == null) {
			return;
		}
		// Le pavage varie d'un bloc à l'autre, toujours le même au même endroit.
		long h = (x * 341873128712L) ^ (z * 132897987541L);
		Block block = style.paving[(int) Math.floorMod(h ^ (h >>> 17), style.paving.length)];
		level.setBlock(ground, block.defaultBlockState(), Block.UPDATE_CLIENTS);
		// Ce qui poussait dessus disparaît.
		for (BlockPos above = ground.above(); level.getBlockState(above).canBeReplaced()
				&& !level.getBlockState(above).isAir(); above = above.above()) {
			level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	private void lamp(WorldGenLevel level, BoundingBox chunkBB, int x, int z) {
		BlockPos ground = surface(level, chunkBB, x, z);
		if (ground == null || !level.getBlockState(ground.above()).canBeReplaced()) {
			return;
		}
		level.setBlock(ground.above(), style.post.defaultBlockState(), Block.UPDATE_CLIENTS);
		level.setBlock(ground.above(2), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
	}
}
