package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.item.AutomailItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
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
 * Un bras transmuté porte sa lame.
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
			models.put(kind, new Model(withBlades(), kind));
		}
	}

	/**
	 * La lame qu'un alchimiste transmute de son avant-bras, comme Ed : une arête d'acier le long du
	 * dessus de l'avant-bras, qui dépasse le poing et s'effile. Texturée en (56, 16) dans la peau de
	 * l'automail.
	 */
	private static void blade(PartDefinition arm, float x) {
		arm.addOrReplaceChild(BLADE, CubeListBuilder.create().texOffs(56, 16)
				.addBox(x, 4, 1.5f, 1, 12, 3)
				.addBox(x, 16, 2, 1, 3, 2), PartPose.ZERO);
	}

	private static final String BLADE = "fmab_blade";

	/** Le squelette du joueur, une lame (cachée tant qu'on ne l'a pas transmutée) à chaque bras. */
	private static ModelPart withBlades() {
		MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		blade(mesh.getRoot().getChild("right_arm"), -1.5f);
		blade(mesh.getRoot().getChild("left_arm"), 0.5f);
		return LayerDefinition.create(mesh, 64, 64).bakeRoot();
	}

	private static final Map<BodyPart, ModelPart> HAND_BLADES = new EnumMap<>(BodyPart.class);

	/**
	 * Pour la première personne : un os de bras sans volume, qui ne porte que la lame. On lui donne la
	 * pose du bras qu'on vient de dessiner.
	 */
	public static ModelPart handBlade(BodyPart arm) {
		return HAND_BLADES.computeIfAbsent(arm, a -> {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition bone = mesh.getRoot().addOrReplaceChild("arm", CubeListBuilder.create(), PartPose.ZERO);
			blade(bone, a == BodyPart.RIGHT_ARM ? -1.5f : 0.5f);
			return LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("arm");
		});
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
			rightArm.getChild(BLADE).visible = holder.fmab$blades().contains(BodyPart.RIGHT_ARM);
			leftArm.getChild(BLADE).visible = holder.fmab$blades().contains(BodyPart.LEFT_ARM);
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
