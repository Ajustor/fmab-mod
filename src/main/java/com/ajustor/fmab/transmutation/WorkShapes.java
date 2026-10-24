package com.ajustor.fmab.transmutation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Les formes des ouvrages de l'alchimie au quotidien (voir {@link Works}) : quel arbre abattre,
 * quelles cases creuser pour un tunnel, où poser un pont, quelles cases aplanir. Rien ici ne touche au
 * monde : on dit seulement où agir, le monde est lu par les prédicats qu'on reçoit.
 */
public final class WorkShapes {
	/** Un abattage ne prend jamais plus de bûches, même dans une forêt dont toutes les branches se touchent. */
	public static final int TREE_LIMIT = 1024;

	private WorkShapes() {
	}

	/**
	 * L'arbre auquel appartient cette bûche : toutes les bûches qui s'y touchent, même en diagonale
	 * (les branches d'acacia et de chêne noir), dans la zone {@code within}. En forêt dense, les branches
	 * des arbres voisins se touchent et tout le bosquet ne fait qu'un : la zone le borne, et l'on n'en
	 * prend que ce qui est proche du cercle. Vide si ce n'est pas un vrai arbre : aucune de ses bûches ne
	 * touche de feuilles poussées d'elles-mêmes (une poutre, une maison en rondins).
	 *
	 * @param log            ce qui est une bûche
	 * @param naturalLeaves  ce qui est une feuille que personne n'a posée
	 * @param within         jusqu'où l'abattage peut aller
	 */
	public static Set<BlockPos> tree(BlockPos start, Predicate<BlockPos> log, Predicate<BlockPos> naturalLeaves,
			Predicate<BlockPos> within) {
		if (!log.test(start) || !within.test(start)) {
			return Set.of();
		}
		Set<BlockPos> found = new LinkedHashSet<>();
		Deque<BlockPos> next = new ArrayDeque<>();
		found.add(start.immutable());
		next.add(start.immutable());
		boolean leafy = false;
		while (!next.isEmpty()) {
			BlockPos p = next.poll();
			for (Direction d : Direction.values()) {
				leafy |= naturalLeaves.test(p.relative(d));
			}
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos q = p.offset(dx, dy, dz);
						if (found.size() < TREE_LIMIT && !found.contains(q) && within.test(q) && log.test(q)) {
							found.add(q);
							next.add(q);
						}
					}
				}
			}
		}
		return leafy ? found : Set.of();
	}

	/**
	 * La zone d'un abattage : à {@code reach} blocs du centre à l'horizontale, d'un peu sous le cercle
	 * jusqu'à la cime des plus grands arbres.
	 */
	public static Predicate<BlockPos> felling(BlockPos center, int reach) {
		return p -> Math.abs(p.getX() - center.getX()) <= reach && Math.abs(p.getZ() - center.getZ()) <= reach
				&& p.getY() >= center.getY() - 2 && p.getY() <= center.getY() + 40;
	}

	/**
	 * Un tunnel de trois sur trois, du plus près au plus loin.
	 *
	 * @param start   la première tranche du tunnel
	 * @param forward où il s'enfonce
	 * @param bottom  décalage de sa tranche, vers le haut, par rapport à {@code start} : 0 pour un tunnel
	 *                qui part au ras du sol (les pieds, la tête et un bloc au-dessus), −1 pour un tunnel
	 *                centré sur {@code start}. Ignoré pour un puits vertical, centré.
	 */
	public static List<BlockPos> tunnel(BlockPos start, Direction forward, int length, int bottom) {
		List<BlockPos> out = new ArrayList<>();
		boolean vertical = forward.getAxis().isVertical();
		Direction side = vertical ? Direction.EAST : forward.getClockWise();
		Direction up = vertical ? Direction.SOUTH : Direction.UP;
		int low = vertical ? -1 : bottom;
		for (int i = 0; i < length; i++) {
			BlockPos slice = start.relative(forward, i);
			for (int a = -1; a <= 1; a++) {
				for (int b = low; b <= low + 2; b++) {
					out.add(slice.relative(side, a).relative(up, b));
				}
			}
		}
		return out;
	}

	/** Le tablier d'un pont de trois de large, posé de niveau devant {@code start}, du plus près au plus loin. */
	public static List<BlockPos> bridge(BlockPos start, Direction forward, int length) {
		Direction side = forward.getClockWise();
		List<BlockPos> out = new ArrayList<>();
		for (int i = 1; i <= length; i++) {
			for (int a = -1; a <= 1; a++) {
				out.add(start.relative(forward, i).relative(side, a));
			}
		}
		return out;
	}

	/** Les cases à raboter et à combler, dans l'ordre où le faire. */
	public record Leveling(List<BlockPos> cut, List<BlockPos> fill) {
	}

	/**
	 * Ce qu'un aplanissement rabote et comble autour de {@code center}, sur un carré de
	 * {@code 2 × radius + 1} de côté : au-dessus du plan, du haut vers le bas pour que rien ne reste
	 * en l'air ; au-dessous, du bas vers le haut pour que le sable ne tombe pas dans le vide.
	 *
	 * @param plane la hauteur du sol à obtenir : le dessus du bloc qui porte le cercle
	 */
	public static Leveling leveling(BlockPos center, int plane, int radius, int above, int below) {
		List<BlockPos> cut = new ArrayList<>();
		List<BlockPos> fill = new ArrayList<>();
		for (int dy = above; dy >= 1; dy--) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					cut.add(new BlockPos(center.getX() + dx, plane + dy, center.getZ() + dz));
				}
			}
		}
		for (int dy = -below; dy <= 0; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					fill.add(new BlockPos(center.getX() + dx, plane + dy, center.getZ() + dz));
				}
			}
		}
		return new Leveling(cut, fill);
	}
}
