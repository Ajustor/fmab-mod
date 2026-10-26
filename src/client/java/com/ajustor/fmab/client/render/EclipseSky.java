package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.stone.Eclipse;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import org.joml.Vector4f;

//? if >=26.3 {
/*import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
*///?} else {
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
//?}

/**
 * La lune de l'éclipse dans le ciel du client (voir SkyRendererMixin et {@link Eclipse#separation}).
 *
 * <p>Minecraft dessine le soleil et la lune en les ajoutant au ciel : une lune sombre ne cache rien.
 * La lune de l'éclipse est donc dessinée deux fois, au même endroit et à la même taille : pâle et
 * ajoutée au ciel tant qu'elle est loin du soleil, comme une lune de jour, puis en silhouette opaque
 * (le pipeline {@link #MASK}) qui mord le soleil et finit par le couvrir. À la totalité, un second
 * soleil plus large, derrière elle, ne laisse dépasser qu'un liseré : la couronne.
 */
public final class EclipseSky {
	/**
	 * Comme le pipeline des astres (RenderPipelines.CELESTIAL, dont il reprend la recette), mais la lune
	 * recouvre ce qu'il y a derrière au lieu de s'y ajouter.
	 */
	public static final RenderPipeline MASK = RenderPipeline.builder()
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			//? if >=26.3 {
			/*.withBindGroupLayout(BindGroupLayouts.PROJECTION)
			.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			*///?} else {
			.withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
			//?}
			.withLocation(Fmab.id("pipeline/eclipse_moon"))
			.withVertexShader("core/position_tex")
			.withFragmentShader("core/position_tex")
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.build();

	/** La taille de la lune de l'éclipse (celle d'une lune ordinaire : 20), pour que son cœur couvre le soleil. */
	public static final float MOON_SIZE = 36;
	/**
	 * La taille de la silhouette : le fond de la texture de la lune est noir et opaque, la silhouette
	 * recouvrirait donc tout son carré. Elle est réduite à son cœur (8 pixels sur 32 de la lune pâle).
	 */
	public static final float SILHOUETTE_SIZE = MOON_SIZE / 4;
	/** La couronne : le soleil redessiné une fois et demie plus grand, derrière la lune. */
	public static final float CORONA_SCALE = 1.5f;

	/** Ce que le ciel dessine en ce moment, en plus de lui-même. */
	public enum Drawing { NONE, CORONA, MASK }

	private static boolean active;
	private static float mask;
	private static float corona;
	private static Drawing drawing = Drawing.NONE;

	private EclipseSky() {
	}

	/** Le ciel de cette image : la lune de l'éclipse à cette avance sur le soleil, ou la lune ordinaire. */
	public static void update(float separation, float coverage) {
		active = !Float.isNaN(separation);
		if (!active) {
			mask = 0;
			corona = 0;
			return;
		}
		mask = maskOpacity(separation);
		corona = (float) Math.pow(coverage, 8);
	}

	/**
	 * L'opacité de la silhouette : nulle quand la lune est à plus de deux fois la distance du premier
	 * contact, pleine au contact. En deçà, la lune pâle s'efface d'autant.
	 */
	static float maskOpacity(float separation) {
		float contact = Eclipse.SUN_HALF + Eclipse.MOON_HALF;
		return Math.clamp((2 * contact - Math.abs(separation)) / contact, 0, 1);
	}

	public static boolean active() {
		return active;
	}

	public static float mask() {
		return mask;
	}

	public static float corona() {
		return corona;
	}

	/** La taille de la lune à dessiner, ou celle d'une lune ordinaire hors du jour de l'éclipse. */
	public static float moonSize(float vanilla) {
		if (drawing == Drawing.MASK) {
			return SILHOUETTE_SIZE;
		}
		return active ? MOON_SIZE : vanilla;
	}

	public static Drawing drawing() {
		return drawing;
	}

	public static void drawing(Drawing now) {
		drawing = now;
	}

	/** La teinte et l'opacité de la lune de l'éclipse, selon ce qu'on en dessine. */
	public static Vector4f moonColor(Vector4f vanilla) {
		if (drawing == Drawing.MASK) {
			return new Vector4f(0.03f, 0.03f, 0.045f, mask);
		}
		return new Vector4f(0.6f, 0.65f, 0.75f, vanilla.w() * (1 - mask));
	}

	/**
	 * L'opacité du voile de l'écran pendant l'éclipse (0 à 255). Sans shaders, le ciel et la lumière
	 * s'assombrissent déjà : un léger voile suffit. Un shader pack calcule les siens d'après l'heure et
	 * ignore l'éclipse : le voile fait alors tout le travail, et cache même son soleil à la totalité.
	 */
	public static int veilAlpha(float strength, boolean shaders) {
		return Math.round((shaders ? 0xB0 : 0x40) * Math.clamp(strength, 0, 1));
	}

	/** La couronne, d'un blanc chaud. */
	public static Vector4f coronaColor(Vector4f vanilla) {
		return new Vector4f(1, 0.92f, 0.78f, vanilla.w());
	}
}
