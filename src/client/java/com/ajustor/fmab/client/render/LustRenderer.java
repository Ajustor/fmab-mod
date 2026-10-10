package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.LustEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Lust : longue robe noire, cheveux noirs, l'Ouroboros sur la poitrine. Quand elle vise, elle tend la
 * main droite vers sa cible et ses ongles noirs pointent ; au coup, ses cinq doigts filent en lames.
 */
public class LustRenderer extends FigureRenderer<LustEntity, LustRenderer.State, LustRenderer.Model> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/lust.png");
	/** La longueur des lames lancées, en multiples de celle des ongles. */
	private static final float REACH = 9;

	public static class State extends FigureRenderer.State {
		LustEntity.Lance lance = LustEntity.Lance.NONE;
	}

	public static class Model extends FigureRenderer.Model<State> {
		private final ModelPart lance;

		Model(ModelPart root) {
			super(root, Temper.LUST);
			lance = rightArm.getChild("lance");
		}

		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			lance.visible = state.lance != LustEntity.Lance.NONE;
			if (!lance.visible) {
				return;
			}
			// Le bras tendu vers ce qu'elle regarde, comme on vise.
			rightArm.xRot = -Mth.HALF_PI + head.xRot;
			rightArm.yRot = head.yRot - 0.1f;
			rightArm.zRot = 0;
			boolean striking = state.lance == LustEntity.Lance.STRIKING;
			lance.yScale = striking ? REACH : 1 + Mth.sin(state.ageInTicks * 0.9f) * 0.15f;
		}
	}

	public LustRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(BossModels.lust()));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(LustEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.lance = entity.lance();
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURE;
	}
}
