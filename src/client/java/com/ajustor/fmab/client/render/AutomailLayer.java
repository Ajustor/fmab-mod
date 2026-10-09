package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.item.AutomailItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
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
 * Les automails sur le corps du joueur : à la place du membre perdu, le bras ou la jambe de métal,
 * qui suit la marche et les gestes de transmutation. Les bras sont toujours larges, même sur une
 * peau fine : une prothèse ne se taille pas sur la peau.
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
		Map<BodyPart, AutomailItem.Model> automails = ((BodyHolder) state).fmab$automails();
		if (automails.isEmpty() || state.isInvisible) {
			return;
		}
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

		@Override
		public void setupAnim(AvatarRenderState state) {
			super.setupAnim(state);
			TransmutationGestures.apply(this, state);
			Map<BodyPart, AutomailItem.Model> automails = ((BodyHolder) state).fmab$automails();
			head.visible = false;
			hat.visible = false;
			body.visible = false;
			rightArm.visible = automails.get(BodyPart.RIGHT_ARM) == kind;
			leftArm.visible = automails.get(BodyPart.LEFT_ARM) == kind;
			rightLeg.visible = automails.get(BodyPart.RIGHT_LEG) == kind;
			leftLeg.visible = automails.get(BodyPart.LEFT_LEG) == kind;
		}
	}
}
