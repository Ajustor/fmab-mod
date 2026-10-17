package com.ajustor.fmab.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionfc;

/**
 * Les appels de rendu dont la forme change d'une version de Minecraft à l'autre. Comme
 * {@code com.ajustor.fmab.compat}, le seul endroit du client où vivent les conditions de version.
 */
public final class RenderCompat {
	private RenderCompat() {
	}

	/** Tourne la pose courante. */
	public static void rotate(PoseStack pose, Quaternionfc rotation) {
		//? if >=26.3 {
		/*pose.rotate(rotation);
		*///?} else {
		pose.mulPose(rotation);
		//?}
	}

	/** Soumet un modèle au rendu, avec sa texture déjà résolue en type de rendu. */
	public static <S> void submitModel(OrderedSubmitNodeCollector collector, Model<? super S> model, S state, PoseStack pose,
			RenderType type, int light, int overlay, int outline) {
		//? if >=26.3 {
		/*collector.submitModel(model, state, pose, type, light, overlay, outline);
		*///?} else {
		collector.submitModel(model, state, pose, type, light, overlay, outline, null);
		//?}
	}

	/** Soumet un modèle au rendu, avec sa texture. */
	public static <S> void submitModel(OrderedSubmitNodeCollector collector, Model<? super S> model, S state, PoseStack pose,
			Identifier texture, int light, int overlay, int outline) {
		//? if >=26.3 {
		/*collector.submitModel(model, state, pose, texture, light, overlay, outline);
		*///?} else {
		collector.submitModel(model, state, pose, texture, light, overlay, outline, null);
		//?}
	}
}
