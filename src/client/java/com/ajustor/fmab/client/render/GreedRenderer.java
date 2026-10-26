package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.client.compat.RenderCompat;
import com.ajustor.fmab.entity.GreedEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Greed : gilet à col de fourrure, lunettes noires, les mains dans les poches. En combat, son
 * Bouclier ultime se déploie : le carbone noir gagne les bras, puis le buste, les jambes et enfin le
 * visage, et se retire dans l'ordre inverse. Chaque partie est dessinée une seule fois, de chair ou de
 * carbone, pour que les deux peaux ne se disputent pas la même surface.
 */
public class GreedRenderer extends FigureRenderer<GreedEntity, GreedRenderer.State, GreedRenderer.Model> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/greed.png");
	private static final Identifier SHIELD = Fmab.id("textures/entity/greed_shield.png");

	public static class State extends FigureRenderer.State {
		/** La part du corps sous le carbone, de 0 à 1. */
		float cover;
	}

	/** Les seuils où le carbone recouvre chaque partie : les bras, le buste, les jambes, la tête. */
	static final float ARMS = 0, BODY = 0.3f, LEGS = 0.55f, HEAD = 0.8f;

	/** La partie, à ce seuil, est-elle déjà de carbone ? */
	static boolean covered(float cover, float threshold) {
		return cover > threshold;
	}

	/** Le corps de Greed, de chair (shell faux) ou de carbone (shell vrai) selon l'avancée du bouclier. */
	public static class Model extends FigureRenderer.Model<State> {
		private final boolean shell;

		Model(boolean shell) {
			super(BossModels.greed(), Temper.GREED);
			this.shell = shell;
		}

		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			rightArm.visible = covered(state.cover, ARMS) == shell;
			leftArm.visible = covered(state.cover, ARMS) == shell;
			body.visible = covered(state.cover, BODY) == shell;
			rightLeg.visible = covered(state.cover, LEGS) == shell;
			leftLeg.visible = covered(state.cover, LEGS) == shell;
			head.visible = covered(state.cover, HEAD) == shell;
			hat.visible = head.visible;
		}
	}

	public GreedRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(false));
		addLayer(new ShieldLayer(this, new Model(true)));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GreedEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.cover = entity.cover(partialTicks);
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURE;
	}

	/** Les parties déjà gagnées par le carbone, avec la peau du bouclier. */
	private static class ShieldLayer extends RenderLayer<State, Model> {
		private final Model model;

		ShieldLayer(RenderLayerParent<State, Model> parent, Model model) {
			super(parent);
			this.model = model;
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, State state, float yRot,
				float xRot) {
			if (state.cover <= 0 || state.isInvisible) {
				return;
			}
			RenderCompat.submitModel(collector.order(1), model, state, poseStack, RenderTypes.entityCutoutNoCull(SHIELD),
					light, LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
		}
	}
}
