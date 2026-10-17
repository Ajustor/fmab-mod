package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.client.compat.RenderCompat;
import com.ajustor.fmab.gate.BodyPart;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import java.util.EnumMap;
import java.util.Map;

/**
 * Ce qu'un joueur porte sur lui, vu de tous : ses gants, et ses cercles tatoués. Chaque pièce est un
 * calque au gabarit d'une peau de joueur ({@code textures/entity/worn/}, voir tools/skins), transparent
 * sauf là où elle se pose, dessiné sur un modèle qui ne montre que le membre qui la porte. Comme les
 * automails, ce modèle reprend la pose des bras et du buste du vrai modèle du joueur.
 */
public class HandsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private final Model model;

	public HandsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
		super(parent);
		model = new Model(context.bakeLayer(ModelLayers.PLAYER));
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot,
			float xRot) {
		BodyHolder holder = (BodyHolder) state;
		if (holder.fmab$worn().isEmpty() || state.isInvisible) {
			return;
		}
		PlayerModel skeleton = getParentModel();
		Map<BodyPart, PartPose> pose = new EnumMap<>(BodyPart.class);
		pose.put(BodyPart.RIGHT_ARM, skeleton.rightArm.storePose());
		pose.put(BodyPart.LEFT_ARM, skeleton.leftArm.storePose());
		pose.put(BodyPart.RIGHT_LEG, skeleton.rightLeg.storePose());
		pose.put(BodyPart.LEFT_LEG, skeleton.leftLeg.storePose());
		pose.put(BodyPart.BODY, skeleton.body.storePose());
		holder.fmab$setSkeleton(pose);
		for (BodyHolder.Worn worn : holder.fmab$worn()) {
			if (holder.fmab$lostLimbs().contains(worn.part())) {
				continue;
			}
			RenderCompat.submitModel(collector.order(1), new Shown(model, worn.part()), state, poseStack,
					RenderTypes.entityCutout(Fmab.id("textures/entity/worn/" + worn.texture() + ".png")), light,
					LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
		}
	}

	/** Le modèle du joueur, réduit au membre qui porte la pièce. */
	static class Model extends HumanoidModel<AvatarRenderState> {
		BodyPart shown = BodyPart.BODY;

		Model(ModelPart root) {
			super(root);
		}

		@Override
		public void setupAnim(AvatarRenderState state) {
			Map<BodyPart, PartPose> skeleton = ((BodyHolder) state).fmab$skeleton();
			AutomailLayer.Model.load(rightArm, skeleton.get(BodyPart.RIGHT_ARM));
			AutomailLayer.Model.load(leftArm, skeleton.get(BodyPart.LEFT_ARM));
			AutomailLayer.Model.load(body, skeleton.get(BodyPart.BODY));
			head.visible = false;
			hat.visible = false;
			rightLeg.visible = false;
			leftLeg.visible = false;
			body.visible = shown == BodyPart.BODY;
			rightArm.visible = shown == BodyPart.RIGHT_ARM;
			leftArm.visible = shown == BodyPart.LEFT_ARM;
		}
	}

	/**
	 * Le même modèle, montrant un autre membre : le dessin est différé, le membre à montrer doit donc
	 * voyager avec la soumission plutôt que rester sur le modèle partagé.
	 */
	static class Shown extends HumanoidModel<AvatarRenderState> {
		private final Model model;
		private final BodyPart part;

		Shown(Model model, BodyPart part) {
			super(model.root());
			this.model = model;
			this.part = part;
		}

		@Override
		public void setupAnim(AvatarRenderState state) {
			model.shown = part;
			model.setupAnim(state);
		}
	}
}
