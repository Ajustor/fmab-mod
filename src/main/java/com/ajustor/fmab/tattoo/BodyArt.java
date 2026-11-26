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

/**
 * Ce qu'un emplacement du corps porte : tatoué dans la peau ou gravé dans l'acier d'un automail. Des
 * traits en couleurs, dessinés à la main (cosmétiques), et au plus un tampon : un cercle du carnet,
 * posé où l'on veut et à la taille voulue, qui fait de l'emplacement un cercle alchimique.
 *
 * <p>Tout est en coordonnées de grille, de 0 à {@link Drawing#GRID} : la grille couvre le carré de
 * l'emplacement ({@link BodySpot}). Le cercle du tampon garde son tracé d'origine : c'est lui que lit
 * l'alchimie, la pose ne sert qu'au rendu.
 */
public record BodyArt(List<Stroke> strokes, Optional<Stamp> stamp) {
	/** L'encre d'un tatouage : un noir violacé, qui se lit sur toutes les carnations. */
	public static final int SKIN_INK = 0x1B1626;
	/** Le trait d'une gravure : l'acier creusé, plus sombre que la pièce. */
	public static final int STEEL_INK = 0x23272C;
	/** Traits au plus : la limite d'un tracé du carnet. */
	public static final int MAX_STROKES = Drawing.MAX_PRIMITIVES;
	/** Points au plus, tous traits confondus : ce qui tient dans un paquet. */
	public static final int MAX_POINTS = 1024;
	/** Le plus petit tampon : assez grand pour que le cercle se lise encore. */
	public static final double MIN_STAMP_HALF = 4;
	public static final double FULL_STAMP_HALF = Drawing.GRID / 2.0;
	/** Le trait du rendu, en pixels d'une texture de 256. */
	public static final double THICKNESS = 4.4;

	public static final BodyArt EMPTY = new BodyArt(List.of(), Optional.empty());

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
	 * @param center où son centre se pose
	 * @param half   sa demi-largeur une fois posé ({@link #FULL_STAMP_HALF} : toute la grille)
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

	public static final Codec<BodyArt> CODEC = RecordCodecBuilder.create(i -> i.group(
			Stroke.CODEC.listOf(0, MAX_STROKES).optionalFieldOf("strokes", List.of()).forGetter(BodyArt::strokes),
			Stamp.CODEC.optionalFieldOf("stamp").forGetter(BodyArt::stamp)
	).apply(i, BodyArt::new));

	/**
	 * Ce que contenaient les sauvegardes avant les dessins : un cercle seul. Il se relit en tampon
	 * plein cadre, de l'encre des tatouages.
	 */
	public static final Codec<BodyArt> CODEC_OR_CIRCLE = Codec.either(CODEC, FmabCodecs.DRAWING).xmap(
			e -> e.map(art -> art, circle -> ofCircle(circle, SKIN_INK)),
			com.mojang.datafixers.util.Either::left);

	public BodyArt {
		strokes = List.copyOf(strokes);
	}

	/** Un cercle seul, posé en tampon sur toute la grille : ce que fait le rituel du cercle au sol. */
	public static BodyArt ofCircle(Drawing circle, int color) {
		return EMPTY.withStamp(new Stamp(circle, Drawing.CENTER_POINT, FULL_STAMP_HALF, color));
	}

	/** Le cercle alchimique que porte l'emplacement, s'il en porte un. */
	public Optional<Drawing> circle() {
		return stamp.map(Stamp::circle).filter(c -> !c.isEmpty());
	}

	public BodyArt withStamp(Stamp value) {
		return new BodyArt(strokes, Optional.ofNullable(value));
	}

	public boolean isEmpty() {
		return strokes.isEmpty() && circle().isEmpty();
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

	/**
	 * Un dessin que le serveur accepte : pas trop lourd, tout dans la grille, un tampon lisible. Le
	 * serveur vérifie à part que le cercle du tampon vient du carnet du joueur.
	 */
	public boolean valid() {
		if (strokes.size() > MAX_STROKES || points() > MAX_POINTS) {
			return false;
		}
		for (Stroke s : strokes) {
			if (!inside(s.shape())) {
				return false;
			}
		}
		if (stamp.isPresent()) {
			Stamp st = stamp.get();
			return st.half() >= MIN_STAMP_HALF && st.half() <= FULL_STAMP_HALF && inside(st.center())
					&& st.circle().primitives().size() <= Drawing.MAX_PRIMITIVES;
		}
		return true;
	}

	private static boolean inside(Primitive p) {
		return switch (p) {
			case Primitive.Line l -> inside(l.from()) && inside(l.to());
			case Primitive.Polyline l -> l.points().stream().allMatch(BodyArt::inside);
			case Primitive.Polygon g -> g.points().stream().allMatch(BodyArt::inside);
			case Primitive.Circle c -> inside(c.center()) && c.radius() > 0 && c.radius() <= Drawing.GRID;
			case Primitive.Arc a -> inside(a.center()) && a.radius() > 0 && a.radius() <= Drawing.GRID;
			case Primitive.Dot d -> inside(d.at());
		};
	}

	private static boolean inside(Vec2 v) {
		return Double.isFinite(v.x()) && Double.isFinite(v.y())
				&& v.x() >= 0 && v.y() >= 0 && v.x() <= Drawing.GRID && v.y() <= Drawing.GRID;
	}

	/**
	 * L'image du dessin, en ARGB ligne par ligne ({@code pixels[y * size + x]}), fond transparent : le
	 * tampon d'abord, puis les traits dans l'ordre où on les a tracés, chacun par-dessus les autres.
	 */
	public int[] paint(int size) {
		int[] pixels = new int[size * size];
		stamp.ifPresent(st -> Raster.trace(st.placed(), size, THICKNESS, i -> pixels[i] = 0xFF000000 | st.color()));
		for (Stroke s : strokes) {
			int argb = 0xFF000000 | s.color();
			Raster.trace(List.of(s.shape()), size, THICKNESS, i -> pixels[i] = argb);
		}
		return pixels;
	}

	/** Le même dessin sans le trait d'indice {@code index}. */
	public BodyArt withoutStroke(int index) {
		List<Stroke> out = new ArrayList<>(strokes);
		out.remove(index);
		return new BodyArt(out, stamp);
	}
}
