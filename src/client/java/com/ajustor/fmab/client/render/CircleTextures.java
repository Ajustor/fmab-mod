package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Raster;
import com.ajustor.fmab.util.PinnedLru;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Textures générées à partir des tracés : une par tracé distinct, gardées tant qu'on s'en sert.
 *
 * <ul>
 * <li>Au-delà de {@link #MAX_TEXTURES}, les moins récemment servies sont libérées, sauf celles
 * qu'on a épinglées ({@link #pin}) : les cinématiques épinglent les leurs le temps de se jouer.</li>
 * <li>Une texture évincée n'est libérée qu'à l'image suivante : celle-ci a pu s'en servir déjà, et
 * l'interface ne se dessine qu'une fois l'image entièrement décrite.</li>
 * <li>Au plus {@link #RASTERS_PER_FRAME} tracés nouveaux sont rastérisés par image : au-delà, un
 * cercle reste invisible une image ou deux, le temps que les autres soient prêts, plutôt que de
 * figer le jeu (80 cercles nouveaux à l'écran : 20 images).</li>
 * <li>La clé d'un tracé est calculée une fois par instance de {@link Drawing} : son empreinte n'est
 * pas recalculée à chaque image.</li>
 * </ul>
 */
public final class CircleTextures {
	public static final int SIZE = 256;
	/** Assez pour 80 cercles à l'écran sans va-et-vient (256 × 256 × 4 octets chacun : ~25 Mo). */
	private static final int MAX_TEXTURES = 96;
	/** Rastérisations nouvelles au plus par image ; les cercles épinglés ne comptent pas. */
	public static final int RASTERS_PER_FRAME = 4;
	/** Au-delà, l'index des clés par instance repart de zéro (les tracés oubliés n'y traînent pas). */
	private static final int KEY_INDEX_LIMIT = 512;

	private static final PinnedLru<Key, Identifier> TEXTURES = new PinnedLru<>(MAX_TEXTURES);
	private static final Map<Drawing, Map<Integer, Key>> KEYS = new IdentityHashMap<>();
	private static final List<Identifier> RELEASE_NEXT_FRAME = new ArrayList<>();
	private static int counter;
	private static long frame = Long.MIN_VALUE;
	private static int rastersThisFrame;
	private static Identifier pending;

	/** Un tracé et sa couleur, avec son empreinte calculée une fois. */
	private record Key(Drawing drawing, int color, int hash) {
		static Key of(Drawing drawing, int color) {
			return new Key(drawing, color, 31 * drawing.hashCode() + color);
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Key k && k.hash == hash && k.color == color && k.drawing.equals(drawing);
		}
	}

	private CircleTextures() {
	}

	/**
	 * @param color couleur ARGB du trait ; le fond reste transparent
	 * @return la texture du tracé, ou une texture vide si le budget de l'image est épuisé
	 */
	public static Identifier get(Drawing drawing, int color) {
		newFrame();
		Key key = key(drawing, color);
		Identifier id = TEXTURES.get(key);
		if (id != null) {
			return id;
		}
		if (rastersThisFrame >= RASTERS_PER_FRAME) {
			return pending();
		}
		rastersThisFrame++;
		return create(key);
	}

	/**
	 * Épingle la texture d'un tracé, rastérisée tout de suite s'il le faut (hors budget) : elle ne sera
	 * pas libérée avant autant d'appels à {@link #unpin}.
	 */
	public static Identifier pin(Drawing drawing, int color) {
		newFrame();
		Key key = key(drawing, color);
		Identifier id = TEXTURES.get(key);
		if (id == null) {
			id = create(key);
		}
		TEXTURES.pin(key);
		return id;
	}

	public static void unpin(Drawing drawing, int color) {
		RELEASE_NEXT_FRAME.addAll(TEXTURES.unpin(key(drawing, color)));
	}

	private static Key key(Drawing drawing, int color) {
		if (KEYS.size() > KEY_INDEX_LIMIT) {
			KEYS.clear();
		}
		return KEYS.computeIfAbsent(drawing, d -> new HashMap<>()).computeIfAbsent(color, c -> Key.of(drawing, c));
	}

	/** Une nouvelle image : on libère ce que la précédente a évincé, et le budget repart. */
	private static void newFrame() {
		long now = Minecraft.getInstance().getFrameTimeNs();
		if (now == frame) {
			return;
		}
		frame = now;
		rastersThisFrame = 0;
		for (Identifier old : RELEASE_NEXT_FRAME) {
			Minecraft.getInstance().getTextureManager().release(old);
		}
		RELEASE_NEXT_FRAME.clear();
	}

	private static Identifier create(Key key) {
		boolean[] mask = Raster.rasterize(key.drawing(), SIZE, 4.4);
		NativeImage image = new NativeImage(SIZE, SIZE, true);
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				if (mask[y * SIZE + x]) {
					image.setPixel(x, y, key.color());
				}
			}
		}
		Identifier id = Fmab.id("dynamic/circle_" + counter++);
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
		RELEASE_NEXT_FRAME.addAll(TEXTURES.put(key, id));
		return id;
	}

	/** Une texture vide, le temps qu'un cercle attende son tour. */
	private static Identifier pending() {
		if (pending == null) {
			pending = Fmab.id("dynamic/circle_pending");
			Minecraft.getInstance().getTextureManager().register(pending,
					new DynamicTexture(pending::toString, new NativeImage(1, 1, true)));
		}
		return pending;
	}
}
