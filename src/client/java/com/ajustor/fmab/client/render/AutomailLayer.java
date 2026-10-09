package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.item.AutomailItem;
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
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * Les automails sur le corps du joueur : à la place du membre perdu, le bras ou la jambe de métal.
 * Les bras sont toujours larges, même sur une peau fine : une prothèse ne se taille pas sur la peau.
 *
 * <p><strong>Un squelette recopié, pas une animation rejouée.</strong> L'automail ne calcule pas sa
 * pose : il reprend os par os celle du vrai modèle du joueur, tel que tout l'a posé (marche, gestes de
 * transmutation, accroupi, et l'animation qu'un autre mod y ajouterait). Rejouer l'animation de son
 * côté, c'était en tenir une seconde copie, qui divergerait au premier geste qu'elle ne connaît pas.
 *
 * <p>La pose est relevée dans {@link #submit}, où vanilla vient de poser le modèle du joueur pour cette
 * entité-là, et rangée dans son état de rendu. Elle ne peut pas être lue plus tard : le dessin est
 * différé, et quand il a lieu, le modèle partagé du joueur porte la pose du dernier joueur soumis.
 */
public class AutomailLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private final Map<AutomailItem.Model, Model> models = new EnumMap<>(AutomailItem.Model.class);

	public AutomailLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
		super(parent);
		for (AutomailItem.Model kind : AutomailItem.Model.values()) {
			models.put(kind, new Model(context.bakeLayer(ModelLayers.PLAYER), kind));
		}
	}

	/** La texture d'un modèle d'automail, au gabarit d'une peau de joueur (bras et jambes). */
	public static Identifier texture(AutomailItem.Model kind) {
		return Fmab.id("textures/entity/automail/" + kind.serializedName() + ".png");
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot,
			float xRot) {
		BodyHolder holder = (BodyHolder) state;
		Map<BodyPart, AutomailItem.Model> automails = holder.fmab$automails();
		if (automails.isEmpty() || state.isInvisible) {
			return;
		}
		PlayerModel skeleton = getParentModel();
		Map<BodyPart, PartPose> pose = new EnumMap<>(BodyPart.class);
		pose.put(BodyPart.RIGHT_ARM, skeleton.rightArm.storePose());
		pose.put(BodyPart.LEFT_ARM, skeleton.leftArm.storePose());
		pose.put(BodyPart.RIGHT_LEG, skeleton.rightLeg.storePose());
		pose.put(BodyPart.LEFT_LEG, skeleton.leftLeg.storePose());
		holder.fmab$setSkeleton(pose);
		for (AutomailItem.Model kind : EnumSet.copyOf(automails.values())) {
			Model model = models.get(kind);
			collector.order(1).submitModel(model, state, poseStack, RenderTypes.entityCutout(model.texture), light,
					LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor, null);
		}
	}

	/** Un modèle de joueur dont on ne montre que les membres équipés d'un automail de ce modèle. */
	static class Model extends HumanoidModel<AvatarRenderState> {
		private final AutomailItem.Model kind;
		private final Identifier texture;

		Model(ModelPart root, AutomailItem.Model kind) {
			super(root);
			this.kind = kind;
			this.texture = texture(kind);
		}

		/** Pas d'animation propre : les membres prennent la pose relevée sur le squelette du joueur. */
		@Override
		public void setupAnim(AvatarRenderState state) {
			BodyHolder holder = (BodyHolder) state;
			Map<BodyPart, PartPose> skeleton = holder.fmab$skeleton();
			load(rightArm, skeleton.get(BodyPart.RIGHT_ARM));
			load(leftArm, skeleton.get(BodyPart.LEFT_ARM));
			load(rightLeg, skeleton.get(BodyPart.RIGHT_LEG));
			load(leftLeg, skeleton.get(BodyPart.LEFT_LEG));
			Map<BodyPart, AutomailItem.Model> automails = holder.fmab$automails();
			head.visible = false;
			hat.visible = false;
			body.visible = false;
			rightArm.visible = automails.get(BodyPart.RIGHT_ARM) == kind;
			leftArm.visible = automails.get(BodyPart.LEFT_ARM) == kind;
			rightLeg.visible = automails.get(BodyPart.RIGHT_LEG) == kind;
			leftLeg.visible = automails.get(BodyPart.LEFT_LEG) == kind;
		}

		private static void load(ModelPart part, PartPose pose) {
			if (pose == null) {
				part.resetPose();
			} else {
				part.loadPose(pose);
			}
		}
	}
}
