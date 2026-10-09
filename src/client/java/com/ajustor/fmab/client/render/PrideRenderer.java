package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.PrideEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Pride : Selim, un garçon sage aux cheveux noirs, en culottes courtes. Révélé, ses ombres se
 * dressent dans son dos.
 */
public class PrideRenderer extends HumanoidMobRenderer<PrideEntity, PrideRenderer.State, PrideRenderer.Model> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/pride.png");

	public static class State extends HumanoidRenderState {
		boolean revealed;
	}

	public static class Model extends HumanoidModel<State> {
		private final ModelPart shadows;

		Model(ModelPart root) {
			super(root);
			shadows = body.getChild("shadows");
		}

		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			shadows.visible = state.revealed;
			// Elles ondulent, comme des flammes noires.
			shadows.xRot = 0.15f + (float) Math.sin(state.ageInTicks * 0.2f) * 0.08f;
		}
	}

	public PrideRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(BossModels.pride()), 0.5f);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(PrideEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.revealed = entity.revealed();
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURE;
	}
}
