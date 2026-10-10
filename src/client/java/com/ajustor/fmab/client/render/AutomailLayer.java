package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.item.AutomailItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
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
	private final Blades bladeModel;

	public AutomailLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
		super(parent);
		for (AutomailItem.Model kind : AutomailItem.Model.values()) {
			models.put(kind, new Model(context.bakeLayer(ModelLayers.PLAYER), kind));
		}
		bladeModel = new Blades(blades());
	}

	/** La texture de la lame transmutée : la même pour tous les automails, c'est de l'acier refait. */
	public static final Identifier BLADE_TEXTURE = Fmab.id("textures/entity/automail/blade.png");

	/**
	 * La lame qu'un alchimiste transmute de son avant-bras, comme Ed : un manchon renforcé sur
	 * l'avant-bras, d'où sort une lame large qui dépasse le poing et s'effile en pointe, son dos épaissi
	 * côté extérieur. Décrite pour le bras droit ; le gauche en est le miroir.
	 */
	private static void blade(PartDefinition arm, boolean right) {
		CubeListBuilder cubes = CubeListBuilder.create().mirror(!right);
		box(cubes, right, 0, 0, -3.5f, 3.5f, -2.5f, 5, 5, 5);
		box(cubes, right, 0, 10, -3, 8, 2, 4, 6, 1);
		box(cubes, right, 10, 10, -3, 14, 2, 3, 3, 1);
		box(cubes, right, 18, 10, -3, 17, 2, 2, 2, 1);
		box(cubes, right, 24, 10, -3, 19, 2, 1, 2, 1);
		box(cubes, right, 32, 0, -3.5f, 7.5f, 1.5f, 1, 12, 2);
		arm.addOrReplaceChild("blade", cubes, PartPose.ZERO);
	}

	/** Une boîte du bras droit, ou son reflet sur le bras gauche (autour de l'axe de chaque bras). */
	private static void box(CubeListBuilder cubes, boolean right, int u, int v, float x, float y, float z, int w, int h,
			int d) {
		cubes.texOffs(u, v).addBox(right ? x : -x - w, y, z, w, h, d);
	}

	/** Les deux bras, sans volume propre, chacun avec sa lame : on n'en montre que les bras transmutés. */
	private static ModelPart blades() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		blade(root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5, 2, 0)), true);
		blade(root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5, 2, 0)), false);
		return LayerDefinition.create(mesh, 64, 32).bakeRoot();
	}

	private static final Map<BodyPart, ModelPart> HAND_BLADES = new EnumMap<>(BodyPart.class);

	/**
	 * Pour la première personne : un os de bras sans volume, qui ne porte que la lame. On lui donne la
	 * pose du bras qu'on vient de dessiner.
	 */
	public static ModelPart handBlade(BodyPart arm) {
		return HAND_BLADES.computeIfAbsent(arm, a -> blades().getChild(a == BodyPart.RIGHT_ARM ? "right_arm" : "left_arm"));
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
		if (!holder.fmab$blades().isEmpty()) {
			collector.order(1).submitModel(bladeModel, state, poseStack, RenderTypes.entityCutout(BLADE_TEXTURE), light,
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

	/** Les lames des bras transmutés, qui prennent elles aussi la pose relevée sur le squelette. */
	static class Blades extends EntityModel<AvatarRenderState> {
		private final ModelPart right;
		private final ModelPart left;

		Blades(ModelPart root) {
			super(root);
			right = root.getChild("right_arm");
			left = root.getChild("left_arm");
		}

		@Override
		public void setupAnim(AvatarRenderState state) {
			BodyHolder holder = (BodyHolder) state;
			Model.load(right, holder.fmab$skeleton().get(BodyPart.RIGHT_ARM));
			Model.load(left, holder.fmab$skeleton().get(BodyPart.LEFT_ARM));
			right.visible = holder.fmab$blades().contains(BodyPart.RIGHT_ARM);
			left.visible = holder.fmab$blades().contains(BodyPart.LEFT_ARM);
		}
	}
}
