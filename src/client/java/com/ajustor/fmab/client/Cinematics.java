package com.ajustor.fmab.client;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.ForbiddenCircles;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.client.render.CircleTextures;
import com.ajustor.fmab.network.CinematicPayload;
import com.ajustor.fmab.transmutation.AlchemyRules;
import net.minecraft.client.DeltaTracker;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Les cinématiques, jouées à l'écran par-dessus le monde et l'interface (voir {@code HudMixin}) :
 * bandes noires de cinéma, fondus, et des images dessinées à la main (l'œil de la Vérité, le torrent du savoir, le titre du Jour promis).
 * Le serveur met en scène le reste (bras noirs, sons, mouvements).
 */
public final class Cinematics {
	/** Les symboles qui déferlent quand on voit ce qu'il y a derrière la Porte. */
	private static final String SYMBOLS = "☉☽♂♀☿♃♄△▽✡⚗∞Ω∴⊕⊗☯✶";
	private static final int BAR = 28;
	/** L'œil de la Porte (128x64, la pupille au centre) et un bras d'ombre (32x128, la main en haut). */
	private static final Identifier EYE = Fmab.id("textures/gui/cinematic/eye.png");
	private static final Identifier ARM = Fmab.id("textures/gui/cinematic/arm.png");
	private static final int CIRCLE_SIZE = CircleTextures.SIZE;
	/** Les glyphes qu'on voit défiler derrière la Porte : tous ceux du monde, tracés comme dans le Traité. */
	private static List<Identifier> glyphs = List.of();
	/** Le voile noir : un dégradé radial calculé une fois, qu'on étire à l'écran (voir {@link #vignette}). */
	private static final int VIGNETTE_SIZE = 128;
	private static Identifier vignette;
	/** Ce que dit la Vérité, en sous-titre, et depuis combien de temps c'est affiché. */
	private static Component subtitle;
	private static int subtitleAge;
	private static int subtitleLength;

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
			// Les glyphes du savoir se tracent maintenant, devant la Porte encore fermée, plutôt qu'au
			// moment où ils doivent défiler.
			glyphs = glyphTextures();
			return;
		}
		kind = payload.kind();
		length = payload.ticks();
		age = 0;
		if (CinematicPayload.GATE_KNOWLEDGE.equals(kind) && glyphs.isEmpty()) {
			glyphs = glyphTextures();
		}
	}

	private static List<Identifier> glyphTextures() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return List.of();
		}
		List<Identifier> out = new ArrayList<>();
		for (Glyph g : AlchemyRules.of(mc.level.registryAccess()).glyphs()) {
			Drawing d = new Drawing(g.primitives().stream()
					.map(p -> p.map(v -> v.scale(13).add(new Vec2(Drawing.CENTER, Drawing.CENTER))))
					.toList());
			out.add(CircleTextures.get(d, 0xFFF2F0EA));
		}
		return out;
	}

	/**
	 * Une réplique de la Vérité (une clé de traduction {@code truth.fmab.*}) : on la sous-titre si une
	 * cinématique est en cours, le temps de la lire.
	 */
	public static void subtitle(Component message) {
		if (!active() || !(message.getContents() instanceof TranslatableContents t) || !t.getKey().startsWith("truth.fmab.")) {
			return;
		}
		subtitle = message;
		subtitleAge = 0;
		subtitleLength = 50 + message.getString().length();
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
		if (subtitle != null && ++subtitleAge > subtitleLength) {
			subtitle = null;
		}
		if (kind != null && ++age > length) {
			kind = null;
		}
	}

	public static void stop() {
		subtitle = null;
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
				case CinematicPayload.GATE_PULL -> pull(graphics, w, h, t);
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
		if (subtitle != null) {
			subtitles(graphics, w, h, subtitleAge + partial);
		}
	}

	/** La réplique, centrée dans la bande du bas (au-dessus si elle tient sur plusieurs lignes). */
	private static void subtitles(GuiGraphicsExtractor graphics, int w, int h, float t) {
		float alpha = Mth.clamp(t / 6, 0, 1) * Mth.clamp((subtitleLength - t) / 10, 0, 1);
		if (alpha <= 0.03f) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		List<FormattedCharSequence> lines = font.split(subtitle, (int) (w * 0.8f));
		int lineHeight = font.lineHeight + 2;
		int block = lines.size() * lineHeight - 2;
		// Centré dans la bande du bas ; s'il déborde, il remonte d'autant.
		int y = Math.min(h - BAR / 2 - block / 2, h - block - 4);
		int a = (int) (alpha * 255) << 24;
		for (FormattedCharSequence line : lines) {
			int x = (w - font.width(line)) / 2;
			// Une ombre noire épaisse : lisible sur le blanc de l'Espace comme sur le noir des bandes.
			graphics.fill(x - 3, y - 1, x + font.width(line) + 3, y + font.lineHeight, (int) (alpha * 150) << 24);
			graphics.text(font, line, x, y, a | 0xECE8DE, false);
			y += lineHeight;
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

	/**
	 * Un voile noir qui gagne depuis les bords : la texture du dégradé, centrée, de plus en plus petite
	 * (le cercle encore clair se resserre), et du noir tout autour d'elle.
	 */
	private static void vignette(GuiGraphicsExtractor graphics, int w, int h, float amount) {
		if (amount <= 0) {
			return;
		}
		float diagonal = (float) Math.hypot(w, h);
		int size = (int) (diagonal * Mth.lerp(amount, 2.4f, 0.45f));
		int alpha = (int) (Math.min(1, amount * 1.6f) * 245);
		int x0 = (w - size) / 2, y0 = (h - size) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, vignetteTexture(), x0, y0, 0, 0, size, size, VIGNETTE_SIZE,
				VIGNETTE_SIZE, VIGNETTE_SIZE, VIGNETTE_SIZE, alpha << 24 | 0xFFFFFF);
		int black = alpha << 24;
		if (y0 > 0) {
			graphics.fill(0, 0, w, y0, black);
			graphics.fill(0, y0 + size, w, h, black);
		}
		if (x0 > 0) {
			graphics.fill(0, Math.max(0, y0), x0, Math.min(h, y0 + size), black);
			graphics.fill(x0 + size, Math.max(0, y0), w, Math.min(h, y0 + size), black);
		}
	}

	private static Identifier vignetteTexture() {
		if (vignette == null) {
			NativeImage image = new NativeImage(VIGNETTE_SIZE, VIGNETTE_SIZE, true);
			float c = (VIGNETTE_SIZE - 1) / 2f;
			for (int y = 0; y < VIGNETTE_SIZE; y++) {
				for (int x = 0; x < VIGNETTE_SIZE; x++) {
					float d = (float) Math.hypot(x - c, y - c) / c;
					float k = Mth.clamp((d - 0.3f) / 0.65f, 0, 1);
					image.setPixel(x, y, (int) (k * k * (3 - 2 * k) * 255) << 24);
				}
			}
			vignette = Fmab.id("dynamic/vignette");
			Minecraft.getInstance().getTextureManager().register(vignette, new DynamicTexture(vignette::toString, image));
		}
		return vignette;
	}

	/**
	 * Le cercle happe l'alchimiste : le voile noir se resserre au rythme d'un cœur qui s'emballe, des
	 * bras d'ombre rampent depuis les bords de l'écran jusqu'à lui, puis tout devient noir.
	 */
	private static void pull(GuiGraphicsExtractor graphics, int w, int h, float t) {
		float p = Mth.clamp(t / length, 0, 1);
		// Le cœur : un battement double, de plus en plus rapproché.
		float period = Mth.lerp(p, 18, 9);
		float beat = t % period;
		float thump = (float) (Math.exp(-beat * 0.6) + 0.6 * Math.exp(-(beat - 4) * (beat - 4) * 0.4));
		vignette(graphics, w, h, Mth.clamp(p * 0.9f + thump * 0.08f, 0, 1));
		// Les bras : du bas et des côtés, chacun avec son retard et son ondulation.
		RandomSource random = RandomSource.create(13);
		int arms = 9;
		float cx = w / 2f, cy = h / 2f;
		for (int i = 0; i < arms; i++) {
			// Répartis sur le bas et les flancs de l'écran (pas le haut : ils sortent du sol).
			double angle = Math.PI * (0.05 + 0.9 * (i + random.nextFloat() * 0.6) / arms);
			float ax = cx - (float) Math.cos(angle) * w * 0.62f;
			float ay = cy + (float) Math.sin(angle) * h * 0.75f;
			float delay = random.nextFloat() * 14;
			float e = Mth.clamp((t - delay) / (length * 0.75f), 0, 1);
			e = e * e * (3 - 2 * e);
			if (e <= 0) {
				continue;
			}
			float dx = cx - ax, dy = cy - ay;
			float dist = (float) Math.sqrt(dx * dx + dy * dy);
			float reach = dist * Mth.lerp(e, 0.15f, 0.92f);
			float width = h * 0.16f;
			float wobble = Mth.sin(t * 0.25f + i * 1.7f) * 0.12f * (1 - e * 0.5f);
			graphics.pose().pushMatrix();
			graphics.pose().translate(ax, ay);
			// La texture a la main en haut (-y) : on la tourne vers le centre de l'écran.
			graphics.pose().rotate((float) Math.atan2(dx, -dy) + wobble);
			graphics.pose().scale(width / 32f, reach / 128f);
			graphics.blit(RenderPipelines.GUI_TEXTURED, ARM, -16, -128, 0, 0, 32, 128, 32, 128, 32, 128,
					(int) (Mth.clamp(e * 3, 0, 1) * 255) << 24 | 0xFFFFFF);
			graphics.pose().popMatrix();
		}
		fade(graphics, w, h, 0xFF000000, Mth.clamp((t - (length - 12)) / 12, 0, 1));
	}

	private static float pulse(float t) {
		return 0.6f + 0.4f * Mth.sin(t * 0.15f);
	}

	/**
	 * Derrière la Porte : le noir, un œil immense qui s'ouvre et dans lequel on plonge ; au fond de la
	 * pupille, un tunnel de cercles de transmutation qui fonce vers vous, et tous les glyphes du monde
	 * qui jaillissent l'un après l'autre (c'est à ce moment qu'on les comprend), jusqu'au blanc.
	 */
	private static void knowledge(GuiGraphicsExtractor graphics, int w, int h, float t) {
		fade(graphics, w, h, 0xFF000000, Mth.clamp(t / 4, 0, 1));
		float cx = w / 2f, cy = h / 2f;
		float diagonal = (float) Math.hypot(w, h);
		float tunnelStart = length * 0.42f;
		float flood = length - 14;
		// Le tunnel et le torrent, sous l'œil : on les découvre quand la pupille avale l'écran.
		float rush = Mth.clamp((t - tunnelStart) / (flood - tunnelStart), 0, 1);
		if (t > tunnelStart - 4) {
			tunnel(graphics, cx, cy, diagonal, t, rush);
			streaks(graphics, cx, cy, diagonal, t, rush);
			glyphRush(graphics, cx, cy, diagonal, t, tunnelStart, flood);
			symbols(graphics, cx, cy, w, h, t, rush);
			// Des éclairs blancs, au rythme du savoir qui entre.
			if (rush > 0.15f && ((int) t) % 9 == 0) {
				fade(graphics, w, h, 0xFFFFFFFF, 0.22f);
			}
		}
		// L'œil : il s'ouvre, regarde, puis on plonge dans sa pupille.
		float open = Mth.clamp((t - 3) / 14, 0, 1);
		open = 1 - (1 - open) * (1 - open);
		float dive = Mth.clamp((t - (tunnelStart - 10)) / 14, 0, 1);
		float zoom = 1 + dive * dive * dive * 40;
		if (open > 0 && dive < 1) {
			float scale = w * 0.62f / 128 * zoom;
			// Il regarde un peu autour de lui avant de vous fixer.
			float look = Mth.sin(t * 0.35f) * 3 * (1 - dive) * Mth.clamp((t - 8) / 6, 0, 1);
			graphics.pose().pushMatrix();
			graphics.pose().translate(cx + look * scale * 0.2f, cy);
			graphics.pose().scale(scale, scale * open);
			graphics.blit(RenderPipelines.GUI_TEXTURED, EYE, -64, -32, 0, 0, 128, 64, 128, 64, 128, 64,
					(int) (Mth.clamp(open * 2, 0, 1) * 255) << 24 | 0xFFFFFF);
			graphics.pose().popMatrix();
		}
		fade(graphics, w, h, 0xFFFFFFFF, Mth.clamp((t - flood) / 10, 0, 1));
	}

	/** Le cercle qui l'a amené ici, répété en anneaux qui foncent vers lui en tournant. */
	private static void tunnel(GuiGraphicsExtractor graphics, float cx, float cy, float diagonal, float t, float rush) {
		Identifier circle = CircleTextures.get(ForbiddenCircles.HUMAN_TRANSMUTATION, 0xFFD8D4CC);
		int rings = 5;
		float speed = 0.016f + rush * 0.04f;
		for (int i = 0; i < rings; i++) {
			float d = (float) ((i / (double) rings + t * speed) % 1.0);
			// La profondeur : petit et pâle au fond, immense en passant.
			float size = diagonal * 0.04f * (float) Math.exp(d * 4.2);
			// Les anneaux du fond restent sombres : le tunnel garde sa profondeur.
			float alpha = d * Mth.clamp((1 - d) * 4, 0, 1) * 0.55f;
			if (alpha <= 0.02f) {
				continue;
			}
			graphics.pose().pushMatrix();
			graphics.pose().translate(cx, cy);
			graphics.pose().rotate((i % 2 == 0 ? 1 : -1) * t * 0.03f + i);
			graphics.pose().scale(size / CIRCLE_SIZE, size / CIRCLE_SIZE);
			graphics.blit(RenderPipelines.GUI_TEXTURED, circle, -CIRCLE_SIZE / 2, -CIRCLE_SIZE / 2, 0, 0, CIRCLE_SIZE,
					CIRCLE_SIZE, CIRCLE_SIZE, CIRCLE_SIZE, CIRCLE_SIZE, CIRCLE_SIZE, (int) (alpha * 255) << 24 | 0xFFFFFF);
			graphics.pose().popMatrix();
		}
	}

	/** Des traînées de vitesse qui filent du centre vers les bords. */
	private static void streaks(GuiGraphicsExtractor graphics, float cx, float cy, float diagonal, float t, float rush) {
		RandomSource random = RandomSource.create(29);
		int count = (int) (20 + rush * 50);
		for (int i = 0; i < count; i++) {
			float angle = random.nextFloat() * Mth.TWO_PI;
			float speed = 0.02f + random.nextFloat() * 0.03f;
			float d = (float) ((random.nextFloat() + t * speed * (1 + rush * 2)) % 1.0);
			float r0 = diagonal * 0.5f * d * d;
			float len = diagonal * (0.02f + 0.12f * d * d) * (0.5f + rush);
			int alpha = (int) (Mth.clamp(d * 3, 0, 1) * (0.25f + rush * 0.45f) * 255);
			int color = random.nextInt(6) == 0 ? 0xC02030 : 0xEDEAE2;
			graphics.pose().pushMatrix();
			graphics.pose().translate(cx, cy);
			graphics.pose().rotate(angle);
			graphics.fill((int) r0, 0, (int) (r0 + len), 1, alpha << 24 | color);
			graphics.pose().popMatrix();
		}
	}

	/** Les glyphes du monde, un à un, qui jaillissent du fond en tournoyant et grandissent en passant. */
	private static void glyphRush(GuiGraphicsExtractor graphics, float cx, float cy, float diagonal, float t,
			float start, float end) {
		int n = glyphs.size();
		if (n == 0) {
			return;
		}
		// Deux passages : chaque glyphe revient une seconde fois, plus vite.
		float span = (end - start) / (n * 1.6f);
		float life = 16;
		for (int k = 0; k < n * 2; k++) {
			float born = k < n ? start + k * span : start + n * span + (k - n) * span * 0.6f;
			float p = (t - born) / life;
			if (p <= 0 || p >= 1) {
				continue;
			}
			double angle = k * 2.39996 + 0.4;
			float dist = diagonal * 0.48f * p * p;
			float size = diagonal * (0.05f + 0.22f * p * p);
			float alpha = Mth.sin(p * Mth.PI);
			graphics.pose().pushMatrix();
			graphics.pose().translate(cx + (float) Math.cos(angle) * dist, cy + (float) Math.sin(angle) * dist);
			graphics.pose().rotate((k % 2 == 0 ? 1 : -1) * p * 1.2f);
			graphics.pose().scale(size / CIRCLE_SIZE, size / CIRCLE_SIZE);
			graphics.blit(RenderPipelines.GUI_TEXTURED, glyphs.get(k % n), -CIRCLE_SIZE / 2, -CIRCLE_SIZE / 2, 0, 0,
					CIRCLE_SIZE, CIRCLE_SIZE, CIRCLE_SIZE, CIRCLE_SIZE, CIRCLE_SIZE, CIRCLE_SIZE,
					(int) (alpha * 255) << 24 | (k % 5 == 3 ? 0xFF6070 : 0xFFFFFF));
			graphics.pose().popMatrix();
		}
	}

	/** La poussière de symboles : de petits signes alchimiques qui accompagnent le torrent. */
	private static void symbols(GuiGraphicsExtractor graphics, float cx, float cy, int w, int h, float t, float rush) {
		Font font = Minecraft.getInstance().font;
		int count = (int) (rush * 70);
		RandomSource random = RandomSource.create(7);
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double speed = 0.6 + random.nextDouble() * 1.6;
			double phase = random.nextDouble();
			double d = ((t * 0.04 * speed + phase) % 1.0);
			double dist = d * d * Math.hypot(w, h) * 0.6;
			int alpha = (int) (Mth.clamp(d * 3, 0, 1) * 200);
			char c = SYMBOLS.charAt(random.nextInt(SYMBOLS.length()));
			int color = random.nextInt(5) == 0 ? 0xC02030 : 0xF0F0F0;
			float scale = (float) (0.8 + d * 2);
			graphics.pose().pushMatrix();
			graphics.pose().translate(cx + (float) (Math.cos(angle) * dist), cy + (float) (Math.sin(angle) * dist));
			graphics.pose().scale(scale, scale);
			graphics.text(font, String.valueOf(c), -3, -4, alpha << 24 | color, false);
			graphics.pose().popMatrix();
		}
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
