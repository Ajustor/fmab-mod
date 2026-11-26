package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.client.compat.RenderCompat;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.item.GloveItem;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.tattoo.BodySpot;
import com.ajustor.fmab.tattoo.TattooSlot;
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
 * Ce qu'un joueur porte sur lui, vu de tous : ses gants, et ses cercles tatoués. Un gant est un calque au
 * gabarit d'une peau de joueur ({@code textures/entity/worn/}, voir tools/skins), transparent sauf là où
 * il se pose, dessiné sur un modèle qui ne montre que le membre qui le porte. Comme les automails, ce
 * modèle reprend la pose des bras et du buste du vrai modèle du joueur.
 *
 * <p>Un gant se pose aussi sur la couche des manches, pour qu'une peau aux manches longues ne le cache
 * pas, et le vrai cercle qu'on y a inscrit se brode au dos de la main. Un tatouage n'a pas de calque :
 * c'est le vrai cercle tatoué, à fleur de peau sous la couche des vêtements, qui le cache là où elle est
 * opaque ({@link BodySpot}). À la première personne, la main ordinaire reçoit les mêmes
 * ({@link #submitHand}).
 */
public class HandsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	/** L'encre des tatouages : un noir violacé, qui se lit sur toutes les carnations. */
	static final int TATTOO_INK = 0xFF1B1626;
	/** Le trait d'une gravure : l'acier creusé, plus sombre que la pièce. */
	static final int ENGRAVING_INK = 0xFF23272C;

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
			// Un bras perdu ne porte plus que ce qui est gravé dans son automail.
			if (holder.fmab$lostLimbs().contains(worn.part()) && !worn.engraved()) {
				continue;
			}
			if (worn.texture() != null) {
				RenderCompat.submitModel(collector.order(1), new Shown(model, worn.part()), state, poseStack,
						RenderTypes.entityCutout(texture(worn, thin)), light,
						LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
			}
			ModelPart bone = switch (worn.part()) {
				case RIGHT_ARM -> skeleton.rightArm;
				case LEFT_ARM -> skeleton.leftArm;
				case BODY -> skeleton.body;
				default -> null;
			};
			if (bone != null && worn.circle() != null) {
				// L'automail a le gabarit large, quelle que soit la peau.
				submitCircle(collector.order(2), poseStack, bone, worn, thin && !worn.engraved(), light);
			}
		}
	}

	/**
	 * La main vue de ses propres yeux : le calque du gant sur le bras que Minecraft vient de soumettre,
	 * puis le cercle qu'il porte, brodé ou tatoué.
	 */
	public static void submitHand(SubmitNodeCollector collector, PoseStack poseStack, ModelPart arm, BodyHolder.Worn worn,
			boolean thin, int light) {
		if (worn.texture() != null) {
			collector.order(1).submitModelPart(arm, poseStack, RenderTypes.entityCutout(texture(worn, thin)), light,
					OverlayTexture.NO_OVERLAY, null);
		}
		if (worn.circle() != null) {
			submitCircle(collector.order(2), poseStack, arm, worn, thin, light);
		}
	}

	/**
	 * Le cercle tatoué à cet emplacement, ou gravé dans l'automail qui remplace le bras, à dessiner sur
	 * le membre qui le porte.
	 */
	public static BodyHolder.Worn tattoo(TattooSlot slot, Drawing drawing, boolean engraved) {
		BodyPart part = slot.arm().orElse(BodyPart.BODY);
		return new BodyHolder.Worn(null, part, drawing, engraved ? ENGRAVING_INK : TATTOO_INK, BodySpot.of(slot),
				engraved);
	}

	/** Le gant porté à cette main, à dessiner ; {@code null} si elle est nue. */
	public static BodyHolder.@Nullable Worn glove(ItemStack stack, boolean left) {
		if (!(stack.getItem() instanceof GloveItem glove)) {
			return null;
		}
		Drawing circle = stack.get(FmabComponents.GLOVE_CIRCLE);
		return new BodyHolder.Worn("glove_" + glove.kind().name().toLowerCase(Locale.ROOT),
				left ? BodyPart.LEFT_ARM : BodyPart.RIGHT_ARM, circle == null || circle.isEmpty() ? null : circle,
				ink(glove.kind()), BodySpot.HAND_BACK, false);
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

	/**
	 * Le vrai cercle, dans le repère du membre qui le porte, au carré que lui donne son emplacement
	 * ({@link BodySpot}) : brodé au dos de la main d'un gant, ou tatoué à fleur de peau.
	 */
	private static void submitCircle(OrderedSubmitNodeCollector collector, PoseStack poseStack, ModelPart bone,
			BodyHolder.Worn worn, boolean thin, int light) {
		// Une gravure est à la surface de l'acier, qu'aucun vêtement ne couvre.
		boolean left = worn.part() == BodyPart.LEFT_ARM;
		BodySpot.Placement at = worn.engraved() ? worn.spot().place(left, thin, BodySpot.GLOVE_LIFT)
				: worn.spot().place(left, thin);
		Identifier texture = CircleTextures.get(worn.circle(), worn.ink());
		poseStack.pushPose();
		bone.translateAndRotate(poseStack);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture), (pose, buffer) -> {
			// Les coins du carré : u suit l'axe horizontal de la page vu de l'extérieur, v le bas (+Y).
			float h = at.half();
			float x0 = at.x() - at.ux() * h, z0 = at.z() - at.uz() * h;
			float x1 = at.x() + at.ux() * h, z1 = at.z() + at.uz() * h;
			float y0 = at.y() - h, y1 = at.y() + h;
			// Les deux faces : le dos du quadrilatère ne doit pas disparaître selon le sens où le membre
			// est tourné.
			vertex(buffer, pose, x0, y0, z0, 0, 0, at, light);
			vertex(buffer, pose, x1, y0, z1, 1, 0, at, light);
			vertex(buffer, pose, x1, y1, z1, 1, 1, at, light);
			vertex(buffer, pose, x0, y1, z0, 0, 1, at, light);
			vertex(buffer, pose, x0, y1, z0, 0, 1, at, light);
			vertex(buffer, pose, x1, y1, z1, 1, 1, at, light);
			vertex(buffer, pose, x1, y0, z1, 1, 0, at, light);
			vertex(buffer, pose, x0, y0, z0, 0, 0, at, light);
		});
		poseStack.popPose();
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
			BodySpot.Placement at, int light) {
		buffer.addVertex(pose, x, y, z)
				.setColor(-1)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, at.nx(), 0, at.nz());
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
