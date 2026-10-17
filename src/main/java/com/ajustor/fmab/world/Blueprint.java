package com.ajustor.fmab.world;

import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import com.ajustor.fmab.Fmab;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Plan d'un bâtiment ou d'un ouvrage : quel bloc mettre à chaque position locale. Une fonction
 * pure de la position (et de la graine de la pièce), ce qui permet de générer chaque chunk
 * indépendamment.
 *
 * <p>Coordonnées locales : x et z de 0 à la taille moins un, y de 0 (le sol de la pièce) vers le
 * haut. L'avant du bâtiment (sa porte) est du côté z = 0 ; la pièce tourne le plan selon son
 * orientation.
 */
public interface Blueprint {
	/**
	 * @return l'état à poser, ou null pour laisser le monde tel quel
	 */
	BlockState at(int x, int y, int z, Plot plot);

	/**
	 * Coffres à poser et à remplir, en coordonnées locales. Le contenu est une fonction de la graine,
	 * pour que deux maisons d'une même sorte n'aient pas les mêmes trésors.
	 */
	default List<Chest> chests(Plot plot) {
		return List.of();
	}

	/** Créatures à faire apparaître, en coordonnées locales. */
	default List<Spawn> spawns(Plot plot) {
		return List.of();
	}

	/**
	 * La parcelle : taille et graine de la pièce, de quoi varier les bâtiments d'une même sorte.
	 *
	 * @param sizeY  hauteur au-dessus du sol
	 * @param ground nombre de couches sous le sol, comprises dans la pièce (fondations, terrassement)
	 */
	record Plot(int sizeX, int sizeY, int sizeZ, int ground, long seed) {
		/** Hasard déterministe : toujours le même pour une même position et une même graine. */
		public int noise(int x, int y, int z, int bound) {
			long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
			h ^= h >>> 33;
			h *= 0xFF51AFD7ED558CCDL;
			h ^= h >>> 33;
			return (int) Math.floorMod(h, bound);
		}

		/** Une variante fixe pour toute la pièce. */
		public int variant(int bound) {
			return noise(0, 0, 0, bound);
		}
	}

	record Spawn(EntityType<?> type, BlockPos local) {
	}

	/**
	 * Un coffre et sa table de butin ({@code data/fmab/loot_table/chests}), qu'un pack de données peut
	 * changer ; il fait face à l'avant du bâtiment, et se remplit à sa première ouverture.
	 */
	record Chest(BlockPos local, ResourceKey<LootTable> table) {
		static Chest loot(BlockPos local, String table) {
			return new Chest(local, ResourceKey.create(Registries.LOOT_TABLE, Fmab.id("chests/" + table)));
		}
	}

	/**
	 * Une place pour un coffre : de l'air posé sur du plein, sous de l'air, à cette hauteur, en partant
	 * du fond du plan (loin de la porte). Le plan étant une fonction pure, on la trouve sans le monde.
	 */
	static java.util.Optional<BlockPos> freeSpot(Blueprint plan, Plot p, int y) {
		for (int z = p.sizeZ() - 2; z >= 1; z--) {
			for (int x = 1; x < p.sizeX() - 1; x++) {
				BlockState here = plan.at(x, y, z, p), below = plan.at(x, y - 1, z, p), above = plan.at(x, y + 1, z, p);
				if (here != null && here.isAir() && below != null && below.isSolid() && above != null && above.isAir()) {
					return java.util.Optional.of(new BlockPos(x, y, z));
				}
			}
		}
		return java.util.Optional.empty();
	}

	/** Ce plan, plus un coffre à cette hauteur (à la première place libre, voir {@link #freeSpot}). */
	static Blueprint withChest(Blueprint plan, int y, String table) {
		return new Blueprint() {
			@Override
			public BlockState at(int x, int yy, int z, Plot p) {
				return plan.at(x, yy, z, p);
			}

			@Override
			public List<Chest> chests(Plot p) {
				List<Chest> out = new java.util.ArrayList<>(plan.chests(p));
				freeSpot(plan, p, y).ifPresent(at -> out.add(Chest.loot(at, table)));
				return out;
			}

			@Override
			public List<Spawn> spawns(Plot p) {
				return plan.spawns(p);
			}
		};
	}
}
