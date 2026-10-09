package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.FatherEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Père sous ses trois formes : le vieillard à la longue barbe blanche, la silhouette sans visage, la
 * forme divine fissurée. Les bras levés quand il forge son soleil.
 */
public class FatherRenderer extends HumanoidMobRenderer<FatherEntity, FatherRenderer.State, FatherRenderer.Model> {
	private static final Identifier[] FORMS = {
			Fmab.id("textures/entity/father.png"),
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

	public FatherRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
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
