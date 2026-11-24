package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.entity.GreatCircleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.ToLongFunction;

/**
 * Des cercles qui se répondent : le même cercle tracé en plusieurs points autour d'un cercle central
 * forme avec lui un cercle géant, à l'échelle d'une ville. Allumer le cercle central allume les
 * relais : la transmutation porte jusqu'à eux. C'est ainsi qu'on trace un cercle trop grand pour être
 * dessiné d'un trait, comme le cercle de Xerxès ou celui qui couvre un pays entier.
 *
 * <p>Un cercle géant se trace à la taille {@link CircleSize#GREAT}, la plus grande de la craie : le
 * centre comme ses relais. Un relais est un cercle inscrit à cette taille qui porte exactement le même
 * tracé que le cercle central. Il en
 * faut au moins {@link #MIN_RELAYS}, à peu près à la même distance du centre (le cercle géant passe
 * par eux), et répartis tout autour : un arc ne referme pas un cercle.
 */
public final class GreatCircle {
	/** Un triangle : le moins qu'il faille pour enclore le centre. */
	public static final int MIN_RELAYS = 3;
	/** Au-delà, on garde les plus proches : le cercle géant n'a que tant de points. */
	public static final int MAX_RELAYS = 12;
	/** Jusqu'où l'on cherche des relais autour du centre. */
	public static final int MAX_RADIUS = 160;
	/** Plus près, ce n'est pas un cercle géant, juste des cercles voisins. */
	public static final int MIN_RADIUS = 8;
	/** Écart toléré entre la distance d'un relais et le rayon moyen. */
	public static final double TOLERANCE = 0.25;
	/** Le plus grand vide entre deux relais voisins, vu du centre : au-delà, le cercle reste ouvert. */
	public static final double MAX_GAP = Math.toRadians(150);

	/**
	 * Un cercle géant.
	 *
	 * @param relays les relais, dans l'ordre des angles autour du centre
	 * @param radius le rayon moyen, horizontal, du centre aux relais
	 */
	public record Ring(BlockPos center, List<BlockPos> relays, double radius) {
		public Ring {
			relays = List.copyOf(relays);
		}
	}

	private GreatCircle() {
	}

	/**
	 * Le cercle géant que forment ces relais autour du centre, s'ils en forment un : assez nombreux,
	 * à la même distance à peu près, et tout autour.
	 */
	public static Optional<Ring> of(BlockPos center, List<BlockPos> relays) {
		List<BlockPos> sorted = new ArrayList<>(relays.stream()
				.filter(p -> distance(center, p) >= MIN_RADIUS && distance(center, p) <= MAX_RADIUS)
				.sorted(Comparator.comparingDouble(p -> distance(center, p)))
				.limit(MAX_RELAYS)
				.toList());
		if (sorted.size() < MIN_RELAYS) {
			return Optional.empty();
		}
		double radius = sorted.stream().mapToDouble(p -> distance(center, p)).average().orElse(0);
		if (sorted.stream().anyMatch(p -> Math.abs(distance(center, p) - radius) > radius * TOLERANCE)) {
			return Optional.empty();
		}
		sorted.sort(Comparator.comparingDouble(p -> angle(center, p)));
		for (int i = 0; i < sorted.size(); i++) {
			double a = angle(center, sorted.get(i));
			double b = angle(center, sorted.get((i + 1) % sorted.size()));
			double gap = i == sorted.size() - 1 ? b + 2 * Math.PI - a : b - a;
			if (gap > MAX_GAP) {
				return Optional.empty();
			}
		}
		return Optional.of(new Ring(center.immutable(), sorted, radius));
	}

	/** Le cercle géant dont ce cercle inscrit est le centre, s'il en est un. */
	public static Optional<Ring> around(ServerLevel level, BlockPos center) {
		if (!(level.getBlockEntity(center) instanceof TransmutationCircleBlockEntity core) || core.drawing().isEmpty()
				|| !core.size().great()) {
			return Optional.empty();
		}
		return of(center, relays(level, center, core.drawing()));
	}

	/** Les cercles inscrits, chargés, qui portent le même tracé que le centre, à portée. */
	static List<BlockPos> relays(ServerLevel level, BlockPos center, Drawing drawing) {
		List<BlockPos> out = copies(level, center, drawing, MAX_RADIUS);
		out.remove(center);
		return out;
	}

	/**
	 * Les cercles géants inscrits, dans les tronçons chargés, qui portent exactement ce tracé, à moins de
	 * {@code reach} blocs (à l'horizontale) d'un point. Côté serveur, les relais d'un centre ; côté
	 * client, ceux que le joueur a déjà tracés, pour l'aider à placer les suivants.
	 */
	public static List<BlockPos> copies(Level level, BlockPos center, Drawing drawing, int reach) {
		List<BlockPos> out = new ArrayList<>();
		int chunks = (reach >> 4) + 1;
		int cx = center.getX() >> 4, cz = center.getZ() >> 4;
		for (int x = cx - chunks; x <= cx + chunks; x++) {
			for (int z = cz - chunks; z <= cz + chunks; z++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof TransmutationCircleBlockEntity circle && circle.size().great()
							&& circle.drawing().equals(drawing)
							&& distance(center, be.getBlockPos()) <= reach) {
						out.add(be.getBlockPos().immutable());
					}
				}
			}
		}
		return out;
	}

	/**
	 * Le tour du cercle géant, au niveau du centre, bloc à bloc et sans trou : ce que suit un mur, une
	 * douve ou un anneau de flammes.
	 */
	public static List<BlockPos> circumference(BlockPos center, double radius) {
		java.util.LinkedHashSet<BlockPos> out = new java.util.LinkedHashSet<>();
		int steps = Math.max(8, (int) Math.ceil(2 * Math.PI * radius * 2));
		for (int i = 0; i < steps; i++) {
			double a = 2 * Math.PI * i / steps;
			out.add(center.offset((int) Math.round(Math.cos(a) * radius), 0, (int) Math.round(Math.sin(a) * radius)));
		}
		return new ArrayList<>(out);
	}

	/** Combien de relais l'aide au placement propose : six, les pointes d'un hexagramme. */
	public static final int PLANNED_RELAYS = 6;

	/**
	 * Un emplacement de relais proposé.
	 *
	 * @param filled un relais y est déjà tracé
	 */
	public record Spot(BlockPos pos, boolean filled) {
	}

	/**
	 * L'aide au placement d'un cercle géant : où sont le centre et les relais déjà tracés, et où
	 * tracer les suivants.
	 *
	 * @param radius le rayon prévu : celui des relais déjà tracés, ou, sans relais, la distance du
	 *               joueur au centre (il trace le premier où il se tient) ; zéro s'il en est trop près
	 * @param spots  les emplacements prévus, à intervalles réguliers sur le tour
	 * @param relays les relais déjà tracés
	 * @param ready  le cercle géant est déjà complet : on peut l'allumer
	 */
	public record Plan(BlockPos center, double radius, List<Spot> spots, List<BlockPos> relays, boolean ready) {
		public Plan {
			spots = List.copyOf(spots);
			relays = List.copyOf(relays);
		}

		/** L'emplacement libre le plus proche de ce point, s'il en reste. */
		public Optional<Spot> next(BlockPos from) {
			return spots.stream().filter(s -> !s.filled())
					.min(Comparator.comparingDouble(s -> distance(from, s.pos())));
		}
	}

	/** Comme {@link #plan(List, ToLongFunction, BlockPos)}, sans savoir quand chaque copie a été tracée. */
	public static Optional<Plan> plan(List<BlockPos> circles, BlockPos player) {
		return plan(circles, p -> 0, player);
	}

	/**
	 * Le plan d'un cercle géant, d'après les copies d'un même cercle déjà tracées et la place du
	 * joueur qui en trace une de plus. Le centre est la copie autour de laquelle les autres se
	 * rangent le mieux ; à égalité (deux copies, ou un triangle), la première tracée, puis celle dont
	 * le joueur s'est éloigné (il vient de tracer l'autre).
	 *
	 * @param drawnAt quand chaque copie a été tracée, en temps de jeu
	 */
	public static Optional<Plan> plan(List<BlockPos> circles, ToLongFunction<BlockPos> drawnAt, BlockPos player) {
		if (circles.isEmpty()) {
			return Optional.empty();
		}
		BlockPos center = circles.stream().min(Comparator
				.comparingInt((BlockPos c) -> -candidates(c, circles).size())
				// Des relais à un bloc près sur le même cercle valent un cercle parfait : à un dixième près.
				.thenComparingLong(c -> Math.round(spread(c, candidates(c, circles)) * 10))
				.thenComparingLong(drawnAt)
				.thenComparingDouble(c -> -distance(c, player))).orElseThrow();
		List<BlockPos> relays = new ArrayList<>(candidates(center, circles));
		relays.sort(Comparator.comparingDouble(p -> angle(center, p)));
		double radius;
		double start;
		if (relays.isEmpty()) {
			// Pas encore de relais : le premier se trace là où se tient le joueur.
			radius = distance(center, player);
			if (radius < MIN_RADIUS || radius > MAX_RADIUS) {
				return Optional.of(new Plan(center, 0, List.of(), List.of(), false));
			}
			start = angle(center, player);
		} else {
			radius = relays.stream().mapToDouble(p -> distance(center, p)).average().orElse(0);
			start = angle(center, relays.getFirst());
		}
		double near = Math.max(2, radius * 0.12);
		List<Spot> spots = new ArrayList<>();
		for (int i = 0; i < PLANNED_RELAYS; i++) {
			double a = start + 2 * Math.PI * i / PLANNED_RELAYS;
			BlockPos at = center.offset((int) Math.round(Math.cos(a) * radius), 0, (int) Math.round(Math.sin(a) * radius));
			boolean filled = relays.stream().anyMatch(r -> distance(r, at) <= near);
			spots.add(new Spot(at, filled));
		}
		return Optional.of(new Plan(center, radius, spots, relays, of(center, relays).isPresent()));
	}

	/** Les autres copies à la bonne distance de celle-ci pour en être les relais. */
	private static List<BlockPos> candidates(BlockPos center, List<BlockPos> circles) {
		return circles.stream().filter(c -> !c.equals(center))
				.filter(c -> distance(center, c) >= MIN_RADIUS && distance(center, c) <= MAX_RADIUS).toList();
	}

	/** L'écart relatif des distances au centre : nul quand les relais sont tous sur le même cercle. */
	private static double spread(BlockPos center, List<BlockPos> relays) {
		if (relays.isEmpty()) {
			return 0;
		}
		double min = relays.stream().mapToDouble(p -> distance(center, p)).min().orElse(0);
		double max = relays.stream().mapToDouble(p -> distance(center, p)).max().orElse(0);
		return (max - min) / Math.max(1, max);
	}

	/** Le cercle géant s'allume : ses traits courent sur le sol d'un relais à l'autre. */
	public static void flash(ServerLevel level, Ring ring, boolean red) {
		GreatCircleEntity.spawn(level, ring, red);
	}

	/** La distance horizontale : un cercle géant se trace sur le sol, à plat. */
	public static double distance(BlockPos center, BlockPos p) {
		double dx = p.getX() - center.getX(), dz = p.getZ() - center.getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static double angle(BlockPos center, BlockPos p) {
		double a = Math.atan2(p.getZ() - center.getZ(), p.getX() - center.getX());
		return a < 0 ? a + 2 * Math.PI : a;
	}
}
