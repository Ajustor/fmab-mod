package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.PrideEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Pride : Selim, un garçon sage aux cheveux noirs, en culottes courtes, la tête un peu grosse
 * d'un enfant. Révélé, ses ombres se dressent dans son dos et un masque noir lui mange le visage.
 */
public class PrideRenderer extends HumanoidMobRenderer<PrideEntity, PrideRenderer.State, PrideRenderer.Model> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/pride.png");

	public static class State extends FigureRenderer.State {
		boolean revealed;
	}

	public static class Model extends HumanoidModel<State> {
		/** Une tête d'enfant, un peu grosse pour son corps. */
		private static final float CHILD_HEAD = 1.15f;
		private final ModelPart shadows;
		private final ModelPart mask;

		Model(ModelPart root) {
			super(root);
			shadows = body.getChild("shadows");
			mask = head.getChild("mask");
		}

		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			Temper.PRIDE.apply(this, state, state.aggressive);
			shadows.visible = state.revealed;
			mask.visible = state.revealed;
			head.xScale = head.yScale = head.zScale = CHILD_HEAD;
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
		state.aggressive = entity.isAggressive();
		state.revealed = entity.revealed();
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURE;
	}
}
