package com.ajustor.fmab.tattoo;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Raster;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.data.FmabCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Ce qu'un emplacement du corps porte : tatoué dans la peau ou gravé dans l'acier d'un automail. Des
 * traits en couleurs, dessinés à la main (cosmétiques), et des tampons : des cercles du carnet, posés
 * où l'on veut et à la taille voulue, qui font de l'emplacement un support de cercles alchimiques. La
 * plupart des emplacements n'en portent qu'un ; le tour d'un bras en porte plusieurs
 * ({@link TattooSlot#maxStamps()}), comme le bras de Scar.
 *
 * <p>Tout est en coordonnées de grille, sur la page de l'emplacement ({@link Canvas}). Le cercle d'un
 * tampon garde son tracé d'origine : c'est lui que lit l'alchimie, la pose ne sert qu'au rendu.
 */
public record BodyArt(List<Stroke> strokes, List<Stamp> stamps) {
	/** L'encre d'un tatouage : un noir violacé, qui se lit sur toutes les carnations. */
	public static final int SKIN_INK = 0x1B1626;
	/** Le trait d'une gravure : l'acier creusé, plus sombre que la pièce. */
	public static final int STEEL_INK = 0x23272C;
	/** Traits au plus : la limite d'un tracé du carnet. */
	public static final int MAX_STROKES = Drawing.MAX_PRIMITIVES;
	/** Points au plus, tous traits confondus : ce qui tient dans un paquet. */
	public static final int MAX_POINTS = 1024;
	/** Tampons au plus, quel que soit l'emplacement. */
	public static final int MAX_STAMPS = 3;
	/** Le plus petit tampon : assez grand pour que le cercle se lise encore. */
	public static final double MIN_STAMP_HALF = 4;
	/** La demi-largeur d'un cercle dans le carnet : un tampon de cette taille garde la sienne. */
	public static final double FULL_STAMP_HALF = Drawing.GRID / 2.0;
	/** Le trait du rendu, en pixels de texture ({@link Canvas#TEXELS_PER_CELL} par case). */
	public static final double THICKNESS = 4.4;

	public static final BodyArt EMPTY = new BodyArt(List.of(), List.of());

	/**
	 * Un trait à main levée ou à l'outil, de sa couleur.
	 *
	 * @param color couleur RGB (l'opacité est ajoutée au rendu)
	 */
	public record Stroke(Primitive shape, int color) {
		public static final Codec<Stroke> CODEC = RecordCodecBuilder.create(i -> i.group(
				FmabCodecs.PRIMITIVE.fieldOf("shape").forGetter(Stroke::shape),
				Codec.INT.fieldOf("color").forGetter(Stroke::color)
		).apply(i, Stroke::new));

		public Stroke {
			color &= 0xFFFFFF;
		}
	}

	/**
	 * Un cercle du carnet posé comme un tampon.
	 *
	 * @param circle le tracé d'origine, centré sur la grille comme dans le carnet
	 * @param center où son centre se pose, sur la page de l'emplacement
	 * @param half   sa demi-largeur une fois posé ({@link #FULL_STAMP_HALF} : sa taille dans le carnet)
	 */
	public record Stamp(Drawing circle, Vec2 center, double half, int color) {
		private static final Codec<Vec2> POINT = Codec.DOUBLE.listOf(2, 2)
				.xmap(l -> new Vec2(l.get(0), l.get(1)), v -> List.of(v.x(), v.y()));

		public static final Codec<Stamp> CODEC = RecordCodecBuilder.create(i -> i.group(
				FmabCodecs.DRAWING.fieldOf("circle").forGetter(Stamp::circle),
				POINT.fieldOf("center").forGetter(Stamp::center),
				Codec.DOUBLE.fieldOf("half").forGetter(Stamp::half),
				Codec.INT.fieldOf("color").forGetter(Stamp::color)
		).apply(i, Stamp::new));

		public Stamp {
			color &= 0xFFFFFF;
		}

		/** Le cercle tel qu'il se pose : ramené à sa taille et à sa place. */
		public List<Primitive> placed() {
			double k = half / FULL_STAMP_HALF;
			return circle.primitives().stream()
					.map(p -> p.map(v -> center.add(v.sub(Drawing.CENTER_POINT).scale(k))))
					.toList();
		}
	}

	/**
	 * Les tampons s'écrivent en liste ({@code stamps}) ; les sauvegardes d'avant n'en avaient qu'un
	 * ({@code stamp}), qui se relit en tête de liste.
	 */
	public static final Codec<BodyArt> CODEC = RecordCodecBuilder.create(i -> i.group(
			Stroke.CODEC.listOf(0, MAX_STROKES).optionalFieldOf("strokes", List.of()).forGetter(BodyArt::strokes),
			Stamp.CODEC.listOf(0, MAX_STAMPS).optionalFieldOf("stamps", List.of()).forGetter(BodyArt::stamps),
			Stamp.CODEC.optionalFieldOf("stamp").forGetter(art -> Optional.empty())
	).apply(i, (strokes, stamps, legacy) -> {
		List<Stamp> all = new ArrayList<>();
		legacy.ifPresent(all::add);
		all.addAll(stamps);
		return new BodyArt(strokes, all);
	}));

	/**
	 * Ce que contenaient les sauvegardes avant les dessins : un cercle seul. Il se relit en tampon
	 * plein cadre, de l'encre des tatouages.
	 */
	public static final Codec<BodyArt> CODEC_OR_CIRCLE = Codec.either(CODEC, FmabCodecs.DRAWING).xmap(
			e -> e.map(art -> art, circle -> ofCircle(circle, SKIN_INK)),
			com.mojang.datafixers.util.Either::left);

	public BodyArt {
		strokes = List.copyOf(strokes);
		stamps = List.copyOf(stamps);
	}

	/** Un cercle seul, posé en tampon sur toute une page carrée : ce que fait le rituel du cercle au sol. */
	public static BodyArt ofCircle(Drawing circle, int color) {
		return EMPTY.withStamp(fullStamp(circle, color, Canvas.SQUARE));
	}

	/** Le tampon d'un cercle posé au milieu de la page, aussi grand qu'elle le permet. */
	public static Stamp fullStamp(Drawing circle, int color, Canvas canvas) {
		return new Stamp(circle, canvas.center(), canvas.maxStampHalf(), color);
	}

	/** Le premier cercle alchimique que porte l'emplacement, s'il en porte un. */
	public Optional<Drawing> circle() {
		return circles().stream().findFirst();
	}

	/** Tous les cercles alchimiques de l'emplacement, dans l'ordre où on les a posés. */
	public List<Drawing> circles() {
		return stamps.stream().map(Stamp::circle).filter(c -> !c.isEmpty()).toList();
	}

	/** Le même dessin, avec ce seul tampon ({@code null} : sans tampon). */
	public BodyArt withStamp(Stamp value) {
		return new BodyArt(strokes, value == null ? List.of() : List.of(value));
	}

	/** Le même dessin, avec un tampon de plus. */
	public BodyArt plusStamp(Stamp value) {
		List<Stamp> out = new ArrayList<>(stamps);
		out.add(value);
		return new BodyArt(strokes, out);
	}

	/** Le même dessin sans le tampon d'indice {@code index}. */
	public BodyArt withoutStamp(int index) {
		List<Stamp> out = new ArrayList<>(stamps);
		out.remove(index);
		return new BodyArt(strokes, out);
	}

	public boolean isEmpty() {
		return strokes.isEmpty() && circles().isEmpty();
	}

	/** Points de contrôle de tous les traits (ce qui pèse dans un paquet). */
	public int points() {
		int n = 0;
		for (Stroke s : strokes) {
			n += points(s.shape());
		}
		return n;
	}

	private static int points(Primitive p) {
		return switch (p) {
			case Primitive.Polyline l -> l.points().size();
			case Primitive.Polygon g -> g.points().size();
			case Primitive.Line l -> 2;
			default -> 1;
		};
	}

	/** Un dessin valide sur une page carrée. */
	public boolean valid() {
		return valid(Canvas.SQUARE);
	}

	/**
	 * Un dessin que le serveur accepte sur cette page : pas trop lourd, tout sur la page, des tampons
	 * lisibles. Le serveur vérifie à part que les cercles nouveaux viennent du carnet du joueur, et
	 * combien de tampons l'emplacement porte.
	 */
	public boolean valid(Canvas canvas) {
		if (strokes.size() > MAX_STROKES || points() > MAX_POINTS || stamps.size() > MAX_STAMPS) {
			return false;
		}
		for (Stroke s : strokes) {
			if (!inside(s.shape(), canvas)) {
				return false;
			}
		}
		for (Stamp st : stamps) {
			if (st.half() < MIN_STAMP_HALF || st.half() > canvas.maxStampHalf() || !canvas.contains(st.center())
					|| st.circle().primitives().size() > Drawing.MAX_PRIMITIVES) {
				return false;
			}
		}
		return true;
	}

	private static boolean inside(Primitive p, Canvas canvas) {
		int reach = Math.max(canvas.width(), canvas.height());
		return switch (p) {
			case Primitive.Line l -> canvas.contains(l.from()) && canvas.contains(l.to());
			case Primitive.Polyline l -> l.points().stream().allMatch(canvas::contains);
			case Primitive.Polygon g -> g.points().stream().allMatch(canvas::contains);
			case Primitive.Circle c -> canvas.contains(c.center()) && c.radius() > 0 && c.radius() <= reach;
			case Primitive.Arc a -> canvas.contains(a.center()) && a.radius() > 0 && a.radius() <= reach;
			case Primitive.Dot d -> canvas.contains(d.at());
		};
	}

	/**
	 * L'image du dessin sur une page carrée de {@code size} pixels de côté, en ARGB ligne par ligne
	 * ({@code pixels[y * size + x]}), fond transparent.
	 */
	public int[] paint(int size) {
		return paint(size, size, size / (double) Drawing.GRID);
	}

	/** L'image du dessin sur cette page, à {@link Canvas#TEXELS_PER_CELL} pixels par case. */
	public int[] paint(Canvas canvas) {
		return paint(canvas.textureWidth(), canvas.textureHeight(), Canvas.TEXELS_PER_CELL);
	}

	/**
	 * Les tampons d'abord, puis les traits dans l'ordre où on les a tracés, chacun par-dessus les autres.
	 * L'épaisseur du trait suit l'échelle : à {@link Canvas#TEXELS_PER_CELL} pixels par case, celle
	 * de {@link #THICKNESS}.
	 */
	private int[] paint(int width, int height, double scale) {
		int[] pixels = new int[width * height];
		double thickness = THICKNESS * scale / Canvas.TEXELS_PER_CELL;
		for (Stamp st : stamps) {
			int argb = 0xFF000000 | st.color();
			Raster.trace(st.placed(), width, height, scale, thickness, i -> pixels[i] = argb);
		}
		for (Stroke s : strokes) {
			int argb = 0xFF000000 | s.color();
			Raster.trace(List.of(s.shape()), width, height, scale, thickness, i -> pixels[i] = argb);
		}
		return pixels;
	}

	/** Le même dessin sans le trait d'indice {@code index}. */
	public BodyArt withoutStroke(int index) {
		List<Stroke> out = new ArrayList<>(strokes);
		out.remove(index);
		return new BodyArt(out, stamps);
	}

	/**
	 * Le même dessin déplacé sur une autre page : chaque point passe par {@code f}, une similitude de
	 * rapport {@code k} (les cercles restent ronds, les tampons changent de taille d'autant).
	 */
	public BodyArt moved(UnaryOperator<Vec2> f, double k) {
		List<Stroke> movedStrokes = strokes.stream().map(s -> new Stroke(s.shape().map(f), s.color())).toList();
		List<Stamp> movedStamps = stamps.stream()
				.map(st -> new Stamp(st.circle(), f.apply(st.center()), st.half() * k, st.color()))
				.toList();
		return new BodyArt(movedStrokes, movedStamps);
	}
}
