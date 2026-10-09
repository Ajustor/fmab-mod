package com.ajustor.fmab.client;

import com.ajustor.fmab.network.CinematicPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Les cinématiques, jouées à l'écran par-dessus le monde et l'interface (voir {@code HudMixin}) :
 * bandes noires de cinéma, fondus, et des images dessinées à la main (l'œil de la Vérité, le torrent du savoir, le titre du Jour promis).
 * Le serveur met en scène le reste (bras noirs, sons, mouvements).
 */
public final class Cinematics {
	/** Les symboles qui déferlent quand on voit ce qu'il y a derrière la Porte. */
	private static final String SYMBOLS = "☉☽♂♀☿♃♄△▽✡⚗∞Ω∴⊕⊗☯✶";
	private static final int BAR = 28;

	/** Le plan en cours (l'aspiration, le savoir, un titre), ou null. */
	private static String kind;
	private static int length;
	private static int age;
	/** Le fond : les bandes noires de la visite de la Porte, sous les autres plans. */
	private static int backgroundLength;
	private static int backgroundAge;

	private Cinematics() {
	}

	public static void start(CinematicPayload payload) {
		if (CinematicPayload.GATE.equals(payload.kind())) {
			backgroundLength = payload.ticks();
			backgroundAge = 0;
			return;
		}
		kind = payload.kind();
		length = payload.ticks();
		age = 0;
	}

	public static boolean active() {
		return kind != null || backgroundAge < backgroundLength;
	}

	/** Le temps des cinématiques s'arrête avec le jeu : la pause ne les fait pas filer. */
	public static void tick() {
		if (Minecraft.getInstance().isPaused()) {
			return;
		}
		if (backgroundAge < backgroundLength) {
			backgroundAge++;
		}
		if (kind != null && ++age > length) {
			kind = null;
		}
	}

	public static void stop() {
		kind = null;
		backgroundLength = 0;
		backgroundAge = 0;
	}

	/** Le dessin d'une image de la cinématique en cours. */
	public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		float partial = delta.getGameTimeDeltaPartialTick(false);
		int w = graphics.guiWidth(), h = graphics.guiHeight();
		if (kind != null) {
			float t = age + partial;
			switch (kind) {
				case CinematicPayload.GATE_PULL -> {
					// Le monde s'assombrit par les bords, puis tout devient noir.
					vignette(graphics, w, h, Mth.clamp(t / length, 0, 1));
					fade(graphics, w, h, 0xFF000000, Mth.clamp((t - (length - 15)) / 15, 0, 1));
				}
				case CinematicPayload.GATE_KNOWLEDGE -> knowledge(graphics, w, h, t);
				case CinematicPayload.PROMISED_DAY -> {
					fade(graphics, w, h, 0xFF600008, 0.35f * pulse(t));
					title(graphics, w, h, t, Component.translatable("cinematic.fmab.promised_day"),
							Component.translatable("cinematic.fmab.promised_day.sub"), 0xFFE02030);
				}
				case CinematicPayload.FATHER_FALL -> {
					fade(graphics, w, h, 0xFFFFFFFF, Mth.clamp(1 - Math.abs(t - 20) / 20, 0, 1));
					title(graphics, w, h, t, Component.translatable("cinematic.fmab.father_fall"),
							Component.translatable("cinematic.fmab.father_fall.sub"), 0xFFF0E0B0);
				}
				default -> {
				}
			}
		}
		// Les bandes suivent le fond s'il y en a un, sinon le plan en cours.
		if (backgroundAge < backgroundLength) {
			bars(graphics, w, h, backgroundAge + partial, backgroundLength);
		} else if (kind != null) {
			bars(graphics, w, h, age + partial, length);
		}
	}

	/** Les bandes noires, qui glissent en entrant et en sortant. */
	private static void bars(GuiGraphicsExtractor graphics, int w, int h, float t, int span) {
		float in = Mth.clamp(t / 10, 0, 1) * Mth.clamp((span - t) / 10, 0, 1);
		int bar = (int) (BAR * in);
		graphics.fill(0, 0, w, bar, 0xFF000000);
		graphics.fill(0, h - bar, w, h, 0xFF000000);
	}

	private static void fade(GuiGraphicsExtractor graphics, int w, int h, int color, float amount) {
		int alpha = (int) (Mth.clamp(amount, 0, 1) * ((color >>> 24) & 0xFF));
		if (alpha > 0) {
			graphics.fill(0, 0, w, h, alpha << 24 | color & 0xFFFFFF);
		}
	}

	/** Un voile noir qui gagne depuis les bords. */
	private static void vignette(GuiGraphicsExtractor graphics, int w, int h, float amount) {
		int step = Math.max(8, Math.max(w, h) / 60);
		double reach = Math.hypot(w, h) / 2;
		for (int y = 0; y < h; y += step) {
			for (int x = 0; x < w; x += step) {
				double d = Math.hypot(x - w / 2.0, y - h / 2.0) / reach;
				int alpha = (int) (Mth.clamp((d - 1 + amount) * 2.5, 0, 1) * 235);
				if (alpha > 0) {
					graphics.fill(x, y, x + step, y + step, alpha << 24);
				}
			}
		}
	}

	private static float pulse(float t) {
		return 0.6f + 0.4f * Mth.sin(t * 0.15f);
	}

	/**
	 * Derrière la Porte : le noir, un œil immense qui s'ouvre, puis le torrent des symboles qui vient
	 * vers vous, de plus en plus vite, jusqu'au blanc.
	 */
	private static void knowledge(GuiGraphicsExtractor graphics, int w, int h, float t) {
		float in = Mth.clamp(t / 8, 0, 1);
		fade(graphics, w, h, 0xFF000000, in * 0.94f);
		int cx = w / 2, cy = h / 2;
		// L'œil : une amande blanche qui s'ouvre, l'iris sombre au centre.
		float open = Mth.clamp((t - 6) / 18, 0, 1) * Mth.clamp((length - 18 - t) / 10, 0, 1);
		int rx = (int) (Math.min(w, h) * 0.32f);
		int ry = (int) (rx * 0.45f * open);
		for (int dy = -ry; dy <= ry; dy += 2) {
			double k = ry == 0 ? 0 : 1 - (double) (dy * dy) / ((double) ry * ry);
			int half = (int) (rx * Math.sqrt(Math.max(0, k)));
			graphics.fill(cx - half, cy + dy, cx + half, cy + dy + 2, 0xFFEDEDE6);
		}
		int iris = (int) (rx * 0.28f * open);
		for (int dy = -iris; dy <= iris; dy += 2) {
			int half = (int) Math.sqrt(Math.max(0, iris * iris - dy * dy));
			graphics.fill(cx - half, cy + dy, cx + half, cy + dy + 2, 0xFF1A1018);
		}
		// Le torrent : des symboles qui jaillissent du centre, de plus en plus nombreux.
		Font font = Minecraft.getInstance().font;
		float rush = Mth.clamp((t - 20) / (length - 40f), 0, 1);
		int count = (int) (rush * 140);
		RandomSource random = RandomSource.create(7);
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double speed = 0.6 + random.nextDouble() * 1.6;
			double phase = random.nextDouble();
			double d = ((t * 0.04 * speed + phase) % 1.0);
			double dist = d * d * Math.hypot(w, h) * 0.6;
			int x = cx + (int) (Math.cos(angle) * dist);
			int y = cy + (int) (Math.sin(angle) * dist);
			int alpha = (int) (Mth.clamp(d * 3, 0, 1) * 255);
			char c = SYMBOLS.charAt(random.nextInt(SYMBOLS.length()));
			int color = random.nextInt(5) == 0 ? 0xC02030 : 0xF0F0F0;
			float scale = (float) (0.8 + d * 2.4);
			graphics.pose().pushMatrix();
			graphics.pose().translate(x, y);
			graphics.pose().scale(scale, scale);
			graphics.text(font, String.valueOf(c), -3, -4, alpha << 24 | color, false);
			graphics.pose().popMatrix();
		}
		// Des éclairs blancs, puis le blanc qui avale tout.
		if (rush > 0.3f && ((int) t) % 7 == 0) {
			fade(graphics, w, h, 0xFFFFFFFF, 0.25f);
		}
		fade(graphics, w, h, 0xFFFFFFFF, Mth.clamp((t - (length - 14)) / 10, 0, 1));
	}

	/** Un titre centré, grand, qui apparaît et s'efface, et une ligne en dessous. */
	private static void title(GuiGraphicsExtractor graphics, int w, int h, float t, Component title, Component sub,
			int color) {
		float alpha = Mth.clamp((t - 10) / 15, 0, 1) * Mth.clamp((length - 10 - t) / 15, 0, 1);
		if (alpha <= 0.02f) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		int a = (int) (alpha * 255) << 24;
		graphics.pose().pushMatrix();
		graphics.pose().translate(w / 2f, h / 2f - 20);
		graphics.pose().scale(3, 3);
		graphics.centeredText(font, title, 0, -4, a | color & 0xFFFFFF);
		graphics.pose().popMatrix();
		graphics.centeredText(font, sub, w / 2, h / 2 + 4, a | 0xE0E0E0);
	}
}
