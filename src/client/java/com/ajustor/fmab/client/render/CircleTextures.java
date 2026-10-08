package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Raster;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Textures générées à partir des tracés : une par tracé distinct, gardées tant qu'on s'en sert.
 * Les plus anciennes sont libérées au-delà de {@link #MAX_TEXTURES}.
 */
public final class CircleTextures {
	public static final int SIZE = 128;
	private static final int MAX_TEXTURES = 64;
	private static final Map<Key, Identifier> TEXTURES = new LinkedHashMap<>(16, 0.75f, true);
	private static int counter;

	private record Key(Drawing drawing, int color) {
	}

	private CircleTextures() {
	}

	/**
	 * @param color couleur ARGB du trait ; le fond reste transparent
	 */
	public static Identifier get(Drawing drawing, int color) {
		Key key = new Key(drawing, color);
		Identifier id = TEXTURES.get(key);
		if (id != null) {
			return id;
		}
		boolean[] mask = Raster.rasterize(drawing, SIZE, 2.2);
		NativeImage image = new NativeImage(SIZE, SIZE, true);
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				if (mask[y * SIZE + x]) {
					image.setPixel(x, y, color);
				}
			}
		}
		id = Fmab.id("dynamic/circle_" + counter++);
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
		TEXTURES.put(key, id);
		evict();
		return id;
	}

	private static void evict() {
		Iterator<Map.Entry<Key, Identifier>> it = TEXTURES.entrySet().iterator();
		while (TEXTURES.size() > MAX_TEXTURES && it.hasNext()) {
			Identifier old = it.next().getValue();
			it.remove();
			Minecraft.getInstance().getTextureManager().release(old);
		}
	}
}
