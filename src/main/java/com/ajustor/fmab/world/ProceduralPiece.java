package com.ajustor.fmab.world;

import net.minecraft.core.registries.Registries;
import com.ajustor.fmab.compat.WorldgenCompat;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
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
	/** Les coffres et les créatures du plan, calculés au premier chunk (pas sauvegardés). */
	private List<Blueprint.Chest> chests;
	private List<Blueprint.Spawn> spawns;

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

	/** Position du monde d'un point du plan (x, z), au niveau de son sol. */
	BlockPos worldAt(int x, int z) {
		return toWorld(new BlockPos(x, 0, z));
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
		blueprint.biome().ifPresent(key -> WorldgenCompat.paint(level.getChunk(chunkPos.x(), chunkPos.z()),
				level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key),
				(x, z) -> x >= minX && x <= maxX && z >= minZ && z <= maxZ));
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		List<BlockPos> connected = new ArrayList<>();
		for (int wx = minX; wx <= maxX; wx++) {
			for (int wz = minZ; wz <= maxZ; wz++) {
				// Sans tableau par colonne : la génération appelle ceci pour chaque colonne de chaque chunk.
				int dx = wx - boundingBox.minX(), dz = wz - boundingBox.minZ();
				int lx = localX(rotation, sizeX, sizeZ, dx, dz), lz = localZ(rotation, sizeX, sizeZ, dx, dz);
				for (int wy = minY; wy <= maxY; wy++) {
					BlockState state = blueprint.at(lx, wy - boundingBox.minY() - ground, lz, plot);
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
		if (chests == null) {
			// Coffres et créatures se calculent une fois par pièce, pas à chaque chunk qu'elle traverse.
			chests = blueprint.chests(plot);
			spawns = blueprint.spawns(plot);
		}
		for (Blueprint.Chest chest : chests) {
			BlockPos at = toWorld(chest.local());
			if (chunkBB.isInside(at)) {
				fill(level, at, chest.table());
			}
		}
		for (Blueprint.Spawn spawn : spawns) {
			BlockPos at = toWorld(spawn.local());
			if (chunkBB.isInside(at)) {
				spawn(level, spawn, at);
			}
		}
	}

	/** Pose le coffre (tourné vers l'avant du bâtiment) ; sa table de butin le remplira à l'ouverture. */
	private void fill(WorldGenLevel level, BlockPos at, ResourceKey<LootTable> table) {
		level.setBlock(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).rotate(rotation),
				2);
		if (level.getBlockEntity(at) instanceof ChestBlockEntity chest) {
			chest.setLootTable(table, seed ^ at.asLong());
		}
	}

	private static void spawn(WorldGenLevel level, Blueprint.Spawn spawn, BlockPos at) {
		ServerLevel server = level.getLevel();
		Entity entity = spawn.type().create(server, EntitySpawnReason.STRUCTURE);
		if (entity == null) {
			return;
		}
		entity.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		if (entity instanceof Villager villager) {
			// Le type de son biome (désert, plaine, taïga...) ; il prendra le métier du poste voisin.
			villager.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.STRUCTURE, null);
		}
		if (entity instanceof Mob mob) {
			mob.setPersistenceRequired();
		}
		level.addFreshEntityWithPassengers(entity);
	}

	/** Position du monde d'une position locale (y compté depuis le sol). */
	private BlockPos toWorld(BlockPos local) {
		int[] d = offset(rotation, sizeX, sizeZ, local.getX(), local.getZ());
		return new BlockPos(boundingBox.minX() + d[0], boundingBox.minY() + ground + local.getY(), boundingBox.minZ() + d[1]);
	}

	/**
	 * Position locale (x, z) dans un plan de {@code sx} sur {@code sz}, tourné de {@code rotation}, d'une
	 * position (dx, dz) relative au coin de sa boîte englobante (qui mesure {@code sz} sur {@code sx}
	 * quand le plan est tourné d'un quart).
	 */
	static int[] local(Rotation rotation, int sx, int sz, int dx, int dz) {
		return new int[]{localX(rotation, sx, sz, dx, dz), localZ(rotation, sx, sz, dx, dz)};
	}

	/** Le x local de {@link #local}, sans tableau. */
	static int localX(Rotation rotation, int sx, int sz, int dx, int dz) {
		return switch (rotation) {
			case NONE -> dx;
			case CLOCKWISE_90 -> dz;
			case CLOCKWISE_180 -> sx - 1 - dx;
			case COUNTERCLOCKWISE_90 -> sx - 1 - dz;
		};
	}

	/** Le z local de {@link #local}, sans tableau. */
	static int localZ(Rotation rotation, int sx, int sz, int dx, int dz) {
		return switch (rotation) {
			case NONE -> dz;
			case CLOCKWISE_90 -> sz - 1 - dx;
			case CLOCKWISE_180 -> sz - 1 - dz;
			case COUNTERCLOCKWISE_90 -> dx;
		};
	}

	/** L'inverse de {@link #local} : la position relative au coin de la boîte d'une position locale. */
	static int[] offset(Rotation rotation, int sx, int sz, int lx, int lz) {
		return switch (rotation) {
			case NONE -> new int[]{lx, lz};
			case CLOCKWISE_90 -> new int[]{sz - 1 - lz, lx};
			case CLOCKWISE_180 -> new int[]{sx - 1 - lx, sz - 1 - lz};
			case COUNTERCLOCKWISE_90 -> new int[]{lz, sx - 1 - lx};
		};
	}
}
