package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.client.compat.RenderCompat;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.item.GloveItem;
import com.ajustor.fmab.registry.FmabComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Ce qu'un joueur porte sur lui, vu de tous : ses gants, et ses cercles tatoués. Chaque pièce est un
 * calque au gabarit d'une peau de joueur ({@code textures/entity/worn/}, voir tools/skins), transparent
 * sauf là où elle se pose, dessiné sur un modèle qui ne montre que le membre qui la porte. Comme les
 * automails, ce modèle reprend la pose des bras et du buste du vrai modèle du joueur.
 *
 * <p>Un gant se pose aussi sur la couche des manches, pour qu'une peau aux manches longues ne le cache
 * pas, et le cercle qu'on y a inscrit se dessine au dos de la main, sur la face extérieure du bras. À la première personne, la main
 * ordinaire reçoit les mêmes ({@link #submitHand}).
 */
public class HandsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	/** Le dos de la main, sur la face extérieure du bras : entre ses lignes 6 et 10, sur ses 4 pixels de profondeur. */
	private static final float CIRCLE_Y = 8f / 16;
	private static final float CIRCLE_HALF = 2f / 16;
	/** De combien le cercle se tient au-dessus de la face : plus que la manche (un quart de pixel). */
	private static final float LIFT = 0.3f / 16;

	private final Model wide;
	private final Model slim;

	public HandsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
		super(parent);
		wide = new Model(context.bakeLayer(ModelLayers.PLAYER));
		slim = new Model(context.bakeLayer(ModelLayers.PLAYER_SLIM));
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
		boolean thin = state.skin != null && state.skin.model() == PlayerModelType.SLIM;
		Model model = thin ? slim : wide;
		for (BodyHolder.Worn worn : holder.fmab$worn()) {
			if (holder.fmab$lostLimbs().contains(worn.part())) {
				continue;
			}
			RenderCompat.submitModel(collector.order(1), new Shown(model, worn.part()), state, poseStack,
					RenderTypes.entityCutout(texture(worn, thin)), light,
					LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
			ModelPart arm = worn.part() == BodyPart.RIGHT_ARM ? skeleton.rightArm
					: worn.part() == BodyPart.LEFT_ARM ? skeleton.leftArm : null;
			if (arm != null && worn.circle() != null) {
				submitCircle(collector.order(2), poseStack, arm, worn, thin, light);
			}
		}
	}

	/**
	 * La main vue de ses propres yeux, gantée : le calque du gant sur le bras que Minecraft vient de
	 * soumettre, puis son cercle.
	 */
	public static void submitHand(SubmitNodeCollector collector, PoseStack poseStack, ModelPart arm, BodyHolder.Worn worn,
			boolean thin, int light) {
		collector.order(1).submitModelPart(arm, poseStack, RenderTypes.entityCutout(texture(worn, thin)), light,
				OverlayTexture.NO_OVERLAY, null);
		if (worn.circle() != null) {
			submitCircle(collector.order(2), poseStack, arm, worn, thin, light);
		}
	}

	/** Le gant porté à cette main, à dessiner ; {@code null} si elle est nue. */
	public static BodyHolder.@Nullable Worn glove(ItemStack stack, boolean left) {
		if (!(stack.getItem() instanceof GloveItem glove)) {
			return null;
		}
		Drawing circle = stack.get(FmabComponents.GLOVE_CIRCLE);
		return new BodyHolder.Worn("glove_" + glove.kind().name().toLowerCase(Locale.ROOT),
				left ? BodyPart.LEFT_ARM : BodyPart.RIGHT_ARM, circle == null || circle.isEmpty() ? null : circle,
				ink(glove.kind()));
	}

	/** Le fil du cercle, choisi pour trancher sur le gant. */
	static int ink(GloveItem.Kind kind) {
		return switch (kind) {
			case CLOTH -> 0xFF2B2B40;
			case LEATHER -> 0xFFE8D8B0;
			case SPARK -> 0xFFC0202A;
			case GAUNTLET -> 0xFF1E2126;
			case STATE -> 0xFF26386C;
		};
	}

	/** Les bras fins n'ont pas le même gabarit de peau : leurs calques ont leur variante. */
	static Identifier texture(BodyHolder.Worn worn, boolean thin) {
		String suffix = thin && worn.part() != BodyPart.BODY ? "_slim" : "";
		return Fmab.id("textures/entity/worn/" + worn.texture() + suffix + ".png");
	}

	/** Le cercle inscrit, posé sur le dos de la main dans le repère du bras. */
	private static void submitCircle(OrderedSubmitNodeCollector collector, PoseStack poseStack, ModelPart arm,
			BodyHolder.Worn worn, boolean thin, int light) {
		// La face extérieure du bras : −X pour le bras droit, +X pour le gauche, à 3 pixels du pivot
		// (2 pour les bras fins). C'est elle qu'on voit à droite de la main, à la première personne.
		float side = worn.part() == BodyPart.RIGHT_ARM ? -1 : 1;
		float x = side * ((thin ? 2f : 3f) / 16 + LIFT);
		Identifier texture = CircleTextures.get(worn.circle(), worn.ink());
		poseStack.pushPose();
		arm.translateAndRotate(poseStack);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture), (pose, buffer) -> {
			float y0 = CIRCLE_Y - CIRCLE_HALF, y1 = CIRCLE_Y + CIRCLE_HALF;
			// Les coordonnées de texture suivent celles de la face sur une peau, qui se lit sans miroir vue
			// de l'extérieur : u part de l'arrière du bras droit (+Z), de l'avant du bras gauche (−Z) ; le
			// bas de la page suit +Y. Les deux faces : le dos du quadrilatère ne doit pas disparaître selon
			// le sens où le bras est tourné.
			float z0 = side * -CIRCLE_HALF, z1 = -z0;
			vertex(buffer, pose, x, z0, y0, 0, 0, side, light);
			vertex(buffer, pose, x, z1, y0, 1, 0, side, light);
			vertex(buffer, pose, x, z1, y1, 1, 1, side, light);
			vertex(buffer, pose, x, z0, y1, 0, 1, side, light);
			vertex(buffer, pose, x, z0, y1, 0, 1, side, light);
			vertex(buffer, pose, x, z1, y1, 1, 1, side, light);
			vertex(buffer, pose, x, z1, y0, 1, 0, side, light);
			vertex(buffer, pose, x, z0, y0, 0, 0, side, light);
		});
		poseStack.popPose();
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float z, float y, float u, float v,
			float side, int light) {
		buffer.addVertex(pose, x, y, z)
				.setColor(-1)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, side, 0, 0);
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
