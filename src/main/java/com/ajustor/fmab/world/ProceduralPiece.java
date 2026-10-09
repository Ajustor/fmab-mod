package com.ajustor.fmab.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Pièce de structure construite par un {@link Blueprint}. Elle ne garde que de quoi recalculer
 * chaque bloc : la sorte de plan, sa taille, son orientation et sa graine. Chaque chunk pose sa
 * part de la pièce, indépendamment des autres.
 */
public class ProceduralPiece extends StructurePiece {
	private final String kind;
	private final int sizeX;
	private final int sizeY;
	private final int sizeZ;
	private final int ground;
	private final Rotation rotation;
	private final long seed;

	/**
	 * @param origin coin de la pièce au niveau du sol (le bas des fondations est {@code ground}
	 *               blocs plus bas)
	 */
	public ProceduralPiece(String kind, BlockPos origin, int sizeX, int sizeY, int sizeZ, int ground,
			Rotation rotation, long seed) {
		super(FmabStructures.PROCEDURAL_PIECE, 0, box(origin, sizeX, sizeY, sizeZ, ground, rotation));
		this.kind = kind;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		this.ground = ground;
		this.rotation = rotation;
		this.seed = seed;
	}

	public ProceduralPiece(CompoundTag tag) {
		super(FmabStructures.PROCEDURAL_PIECE, tag);
		this.kind = tag.getStringOr("kind", "");
		this.sizeX = tag.getIntOr("sx", 1);
		this.sizeY = tag.getIntOr("sy", 1);
		this.sizeZ = tag.getIntOr("sz", 1);
		this.ground = tag.getIntOr("ground", 0);
		this.rotation = Rotation.values()[Math.floorMod(tag.getIntOr("rot", 0), Rotation.values().length)];
		this.seed = tag.getLongOr("seed", 0);
	}

	private static BoundingBox box(BlockPos origin, int sx, int sy, int sz, int ground, Rotation rotation) {
		boolean turned = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
		int wx = turned ? sz : sx;
		int wz = turned ? sx : sz;
		return new BoundingBox(origin.getX(), origin.getY() - ground, origin.getZ(),
				origin.getX() + wx - 1, origin.getY() + sy - 1, origin.getZ() + wz - 1);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putString("kind", kind);
		tag.putInt("sx", sizeX);
		tag.putInt("sy", sizeY);
		tag.putInt("sz", sizeZ);
		tag.putInt("ground", ground);
		tag.putInt("rot", rotation.ordinal());
		tag.putLong("seed", seed);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
		Blueprint blueprint = Blueprints.get(kind);
		if (blueprint == null || !boundingBox.intersects(chunkBB)) {
			return;
		}
		Blueprint.Plot plot = new Blueprint.Plot(sizeX, sizeY, sizeZ, ground, seed);
		int minX = Math.max(boundingBox.minX(), chunkBB.minX());
		int maxX = Math.min(boundingBox.maxX(), chunkBB.maxX());
		int minZ = Math.max(boundingBox.minZ(), chunkBB.minZ());
		int maxZ = Math.min(boundingBox.maxZ(), chunkBB.maxZ());
		int minY = Math.max(boundingBox.minY(), chunkBB.minY());
		int maxY = Math.min(boundingBox.maxY(), chunkBB.maxY());
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		List<BlockPos> connected = new ArrayList<>();
		for (int wx = minX; wx <= maxX; wx++) {
			for (int wz = minZ; wz <= maxZ; wz++) {
				int[] local = toLocal(wx - boundingBox.minX(), wz - boundingBox.minZ());
				for (int wy = minY; wy <= maxY; wy++) {
					BlockState state = blueprint.at(local[0], wy - boundingBox.minY() - ground, local[1], plot);
					if (state == null) {
						continue;
					}
					pos.set(wx, wy, wz);
					BlockState placed = state.rotate(rotation);
					if (level.getBlockState(pos) != placed) {
						level.setBlock(pos, placed, 2);
					}
					if (placed.getBlock() instanceof CrossCollisionBlock || placed.getBlock() instanceof WallBlock) {
						connected.add(pos.immutable());
					}
				}
			}
		}
		// Une fois tout posé, ce qui se raccorde à ses voisins (vitres, barreaux, clôtures) les regarde.
		for (BlockPos at : connected) {
			BlockState shaped = Block.updateFromNeighbourShapes(level.getBlockState(at), level, at);
			level.setBlock(at, shaped, 2);
		}
		for (Blueprint.Chest chest : blueprint.chests(plot)) {
			BlockPos at = toWorld(chest.local());
			if (chunkBB.isInside(at)) {
				fill(level, at, chest.contents().apply(plot));
			}
		}
		for (Blueprint.Spawn spawn : blueprint.spawns(plot)) {
			BlockPos at = toWorld(spawn.local());
			if (chunkBB.isInside(at)) {
				spawn(level, spawn, at);
			}
		}
	}

	/** Pose le coffre (tourné vers l'avant du bâtiment) et range son contenu. */
	private void fill(WorldGenLevel level, BlockPos at, List<ItemStack> contents) {
		level.setBlock(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).rotate(rotation),
				2);
		if (level.getBlockEntity(at) instanceof ChestBlockEntity chest) {
			for (int i = 0; i < contents.size() && i < chest.getContainerSize(); i++) {
				chest.setItem(i, contents.get(i));
			}
		}
	}

	private static void spawn(WorldGenLevel level, Blueprint.Spawn spawn, BlockPos at) {
		ServerLevel server = level.getLevel();
		Entity entity = spawn.type().create(server, EntitySpawnReason.STRUCTURE);
		if (entity == null) {
			return;
		}
		entity.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		if (entity instanceof Mob mob) {
			mob.setPersistenceRequired();
		}
		level.addFreshEntityWithPassengers(entity);
	}

	/** Position locale (x, z) d'une position relative au coin de la boîte englobante. */
	private int[] toLocal(int dx, int dz) {
		return switch (rotation) {
			case NONE -> new int[]{dx, dz};
			case CLOCKWISE_90 -> new int[]{dz, sizeZ - 1 - dx};
			case CLOCKWISE_180 -> new int[]{sizeX - 1 - dx, sizeZ - 1 - dz};
			case COUNTERCLOCKWISE_90 -> new int[]{sizeX - 1 - dz, dx};
		};
	}

	/** Position du monde d'une position locale (y compté depuis le sol). */
	private BlockPos toWorld(BlockPos local) {
		int lx = local.getX(), lz = local.getZ();
		int dx, dz;
		switch (rotation) {
			case CLOCKWISE_90 -> {
				dx = sizeZ - 1 - lz;
				dz = lx;
			}
			case CLOCKWISE_180 -> {
				dx = sizeX - 1 - lx;
				dz = sizeZ - 1 - lz;
			}
			case COUNTERCLOCKWISE_90 -> {
				dx = lz;
				dz = sizeX - 1 - lx;
			}
			default -> {
				dx = lx;
				dz = lz;
			}
		}
		return new BlockPos(boundingBox.minX() + dx, boundingBox.minY() + ground + local.getY(), boundingBox.minZ() + dz);
	}
}
