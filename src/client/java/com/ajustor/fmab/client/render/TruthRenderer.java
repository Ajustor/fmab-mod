package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.TruthEntity;
import com.ajustor.fmab.gate.BodyPart;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * La Vérité : la silhouette de l'alchimiste qu'elle reçoit, toute blanche, avec son sourire. Ce
 * qu'elle a volé, elle le porte : ces parties-là sont dessinées avec la peau de l'alchimiste.
 */
public class TruthRenderer extends HumanoidMobRenderer<TruthEntity, TruthRenderer.State, TruthRenderer.Model> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/truth.png");

	/** Ce qu'il faut savoir pour dessiner la Vérité : ce qu'elle a volé, et à qui. */
	public static class State extends HumanoidRenderState {
		Set<BodyPart> stolen = EnumSet.noneOf(BodyPart.class);
		Identifier skin = DefaultPlayerSkin.getDefaultTexture();
	}

	/**
	 * Le modèle de joueur, dont on ne montre qu'une partie : le blanc (ce qui n'a pas été volé) ou la
	 * peau (ce qui l'a été).
	 */
	public static class Model extends HumanoidModel<State> {
		private final boolean stolenPass;

		Model(ModelPart root, boolean stolenPass) {
			super(root);
			this.stolenPass = stolenPass;
		}

		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			boolean body = state.stolen.contains(BodyPart.BODY);
			show(head, body || state.stolen.contains(BodyPart.SIGHT));
			show(hat, body || state.stolen.contains(BodyPart.SIGHT));
			show(this.body, body || state.stolen.contains(BodyPart.ORGANS));
			show(rightArm, body || state.stolen.contains(BodyPart.RIGHT_ARM));
			show(leftArm, body || state.stolen.contains(BodyPart.LEFT_ARM));
			show(rightLeg, body || state.stolen.contains(BodyPart.RIGHT_LEG));
			show(leftLeg, body || state.stolen.contains(BodyPart.LEFT_LEG));
		}

		private void show(ModelPart part, boolean stolen) {
			part.visible = stolen == stolenPass;
		}
	}

	public TruthRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(context.bakeLayer(ModelLayers.PLAYER), false), 0.0f);
		addLayer(new StolenLayer(this, new Model(context.bakeLayer(ModelLayers.PLAYER), true)));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TruthEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.stolen = entity.stolen();
		state.skin = entity.owner().map(TruthRenderer::skinOf).orElse(DefaultPlayerSkin.getDefaultTexture());
	}

	/** La peau de l'alchimiste, s'il est connu du client ; sinon celle qu'on lui donnerait par défaut. */
	private static Identifier skinOf(UUID owner) {
		var connection = Minecraft.getInstance().getConnection();
		PlayerInfo info = connection == null ? null : connection.getPlayerInfo(owner);
		return (info != null ? info.getSkin() : DefaultPlayerSkin.get(owner)).body().texturePath();
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURE;
	}

	/** Les parties volées, avec la peau de leur ancien propriétaire. */
	private static class StolenLayer extends RenderLayer<State, Model> {
		private final Model model;

		StolenLayer(RenderLayerParent<State, Model> parent, Model model) {
			super(parent);
			this.model = model;
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, State state, float yRot,
				float xRot) {
			if (state.stolen.isEmpty() || state.isInvisible) {
				return;
			}
			collector.order(1).submitModel(model, state, poseStack, RenderTypes.entityTranslucent(state.skin), light,
					LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor, null);
		}
	}
}
