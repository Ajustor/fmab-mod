package com.ajustor.fmab.item;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le calque d'un gant porté (textures/entity/worn, voir HandsLayer) : il couvre aussi la couche des
 * manches, sinon une peau aux manches longues le cache, et les bras fins ont le leur.
 */
class GloveLayerTest {
	@Test
	void everyGloveCoversTheSleevesOfWideAndSlimArms() throws IOException {
		for (GloveItem.Kind kind : GloveItem.Kind.values()) {
			String name = "glove_" + kind.name().toLowerCase(Locale.ROOT);
			for (String variant : new String[]{name, name + "_slim"}) {
				BufferedImage image = layer(variant);
				// Les manches : droite en (40, 32), gauche en (48, 48), les quatre lignes du bas.
				assertTrue(painted(image, 40, 32 + 4 + 8, 16, 4), variant + " : manche droite");
				assertTrue(painted(image, 48, 48 + 4 + 8, 16, 4), variant + " : manche gauche");
			}
		}
	}

	private static BufferedImage layer(String name) throws IOException {
		try (InputStream in = GloveLayerTest.class.getResourceAsStream("/assets/fmab/textures/entity/worn/" + name + ".png")) {
			assertNotNull(in, name + ".png manquant");
			return ImageIO.read(in);
		}
	}

	private static boolean painted(BufferedImage image, int x0, int y0, int w, int h) {
		for (int y = y0; y < y0 + h; y++) {
			for (int x = x0; x < x0 + w; x++) {
				if ((image.getRGB(x, y) >>> 24) != 0) {
					return true;
				}
			}
		}
		return false;
	}
}
