package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.FatherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/**
 * Père sous ses trois formes : le corps qu'il a copié sur Hohenheim (même modèle, même peau), la
 * silhouette sans visage, la forme divine fissurée. Les bras levés quand il forge son soleil.
 */
public class FatherRenderer extends HumanoidMobRenderer<FatherEntity, FatherRenderer.State, FatherRenderer.Model> {
	private static final Identifier[] FORMS = {
			// Père a pris le corps de Hohenheim : sa première forme en est la copie exacte.
			Fmab.id("textures/entity/hohenheim.png"),
			Fmab.id("textures/entity/father_faceless.png"),
			Fmab.id("textures/entity/father_divine.png")};

	public static class State extends HumanoidRenderState {
		int phase;
		boolean forging;
	}

	public static class Model extends HumanoidModel<State> {
		Model(ModelPart root) {
			super(root);
		}

		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			if (state.forging) {
				// Les deux mains levées au-dessus de la tête, autour du soleil.
				rightArm.xRot = (float) Math.PI;
				leftArm.xRot = (float) Math.PI;
				rightArm.zRot = 0.25f;
				leftArm.zRot = -0.25f;
			}
		}
	}

	/** Le corps de Hohenheim : le modèle de joueur complet, seconde couche de peau comprise. */
	private final Model human;
	/** Les formes qui ne doivent plus rien à Hohenheim, avec leur crinière. */
	private final Model own;

	public FatherRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(BossModels.father()), 0.5f);
		own = model;
		human = new Model(context.bakeLayer(ModelLayers.PLAYER));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		model = state.phase == 0 ? human : own;
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(FatherEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.phase = Math.clamp(entity.phase(), 0, FORMS.length - 1);
		state.forging = entity.sun() > 0;
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return FORMS[state.phase];
	}
}
