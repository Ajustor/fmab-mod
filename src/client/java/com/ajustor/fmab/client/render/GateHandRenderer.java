package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.GateHandEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Un bras noir de la Porte : une longue coulée d'ombre qui s'effile, faite de segments qui ondulent
 * (voir {@link GateHandEntity#point}), et une main au bout. Les doigts, écartés tant qu'elle cherche,
 * se referment sur la proie.
 */
public class GateHandRenderer extends EntityRenderer<GateHandEntity, GateHandRenderer.State> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/gate_hand.png");
	/** Segments par bloc de bras : assez pour que la courbe paraisse souple. */
	private static final float SEGMENTS_PER_BLOCK = 2.5f;
	private static final int MAX_SEGMENTS = 24;
	/** Épaisseur du bras à sa base et près de la main, par rapport au cube de 3 pixels. */
	private static final float BASE_THICKNESS = 1.15f;
	private static final float TIP_THICKNESS = 0.55f;

	public static class State extends EntityRenderState {
		/** Les points du bras, de la base à la main, par rapport à la base. */
		Vec3[] points = new Vec3[0];
		/** Combien la main est refermée, de 0 (grande ouverte) à 1 (elle tient sa proie). */
		float grip;
		float age;
	}

	/** Un segment du bras : un cube de 3 pixels sur un bloc, qu'on oriente et qu'on étire. */
	static class Segment extends EntityModel<State> {
		Segment(ModelPart root) {
			super(root);
		}

		static LayerDefinition layer() {
			MeshDefinition mesh = new MeshDefinition();
			mesh.getRoot().addOrReplaceChild("arm", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5f, -1.5f, 0, 3, 3, 16),
					PartPose.ZERO);
			return LayerDefinition.create(mesh, 64, 64);
		}
	}

	/** La main : une paume, quatre doigts de deux phalanges, un pouce. */
	static class Hand extends EntityModel<State> {
		private final ModelPart[] fingers = new ModelPart[4];
		private final ModelPart[] tips = new ModelPart[4];
		private final ModelPart thumb;
		private final ModelPart thumbTip;

		Hand(ModelPart root) {
			super(root);
			ModelPart palm = root.getChild("palm");
			for (int i = 0; i < fingers.length; i++) {
				fingers[i] = palm.getChild("finger" + i);
				tips[i] = fingers[i].getChild("tip");
			}
			thumb = palm.getChild("thumb");
			thumbTip = thumb.getChild("tip");
		}

		/**
		 * La pose se règle ici : le rendu remet le modèle à zéro juste avant de l'animer, toute pose
		 * posée ailleurs serait perdue.
		 */
		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			float g = state.grip;
			for (int i = 0; i < fingers.length; i++) {
				// Ouverte, la main tâtonne : les doigts s'écartent et remuent chacun à son rythme.
				float wiggle = Mth.sin(state.age * 0.45f + i * 1.3f) * 0.18f * (1 - g);
				fingers[i].xRot = Mth.lerp(g, -0.2f + wiggle, 1.05f);
				fingers[i].yRot = Mth.lerp(g, (i - 1.5f) * 0.22f, (i - 1.5f) * 0.04f);
				tips[i].xRot = Mth.lerp(g, 0.15f + wiggle * 0.5f, 1.25f);
			}
			thumb.yRot = Mth.lerp(g, -0.85f, -0.25f);
			thumb.xRot = Mth.lerp(g, 0, 0.9f);
			thumbTip.xRot = Mth.lerp(g, 0.1f, 0.9f);
		}

		static LayerDefinition layer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition palm = mesh.getRoot().addOrReplaceChild("palm",
					CubeListBuilder.create().texOffs(0, 20).addBox(-2, -1, 0, 4, 2, 3), PartPose.ZERO);
			for (int i = 0; i < 4; i++) {
				// Le majeur et l'annulaire dépassent un peu : une main, pas un râteau.
				float z = (i == 1 || i == 2) ? 3 : 2.6f;
				PartDefinition finger = palm.addOrReplaceChild("finger" + i, CubeListBuilder.create().texOffs(20, 20)
						.addBox(-0.5f, -0.5f, 0, 1, 1, 3), PartPose.offset(-1.5f + i, -0.3f, z));
				finger.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(30, 20)
						.addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 2), PartPose.offset(0, 0, 2.8f));
			}
			PartDefinition thumb = palm.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(20, 26)
					.addBox(-0.5f, -0.5f, 0, 1, 1, 2), PartPose.offset(-2.1f, 0, 0.8f));
			thumb.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(30, 26)
					.addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 2), PartPose.offset(0, 0, 1.8f));
			return LayerDefinition.create(mesh, 64, 64);
		}
	}

	private final Segment segment;
	private final Hand hand;

	public GateHandRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.segment = new Segment(Segment.layer().bakeRoot());
		this.hand = new Hand(Hand.layer().bakeRoot());
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GateHandEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		Vec3 toward = entity.toward(partialTicks);
		float reach = entity.reach(partialTicks);
		state.age = entity.tickCount + partialTicks;
		// La main se referme dans le dernier quart de l'approche.
		float g = Mth.clamp((reach - 0.75f) / 0.25f, 0, 1);
		state.grip = g * g * (3 - 2 * g);
		int n = Mth.clamp((int) Math.ceil(toward.length() * reach * SEGMENTS_PER_BLOCK), 1, MAX_SEGMENTS);
		Vec3[] points = new Vec3[n + 1];
		for (int i = 0; i <= n; i++) {
			points[i] = GateHandEntity.point(toward, reach, (float) i / n, state.age, entity.getId());
		}
		state.points = points;
	}

	@Override
	protected boolean affectedByCulling(GateHandEntity entity) {
		return false;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		Vec3[] points = state.points;
		if (points.length < 2 || points[points.length - 1].lengthSqr() < 0.0025) {
			return;
		}
		int n = points.length - 1;
		for (int i = 0; i < n; i++) {
			Vec3 d = points[i + 1].subtract(points[i]);
			float length = (float) d.length();
			if (length < 1.0e-4f) {
				continue;
			}
			float thickness = Mth.lerp((float) i / n, BASE_THICKNESS, TIP_THICKNESS);
			poseStack.pushPose();
			poseStack.translate(points[i].x, points[i].y, points[i].z);
			orient(poseStack, d);
			// Un peu plus long que l'écart : les segments se chevauchent et le bras ne se fend pas aux coudes.
			poseStack.scale(thickness, thickness, length + 0.04f);
			collector.submitModel(segment, state, poseStack, TEXTURE, 0xF000F0, OverlayTexture.NO_OVERLAY,
					state.outlineColor, null);
			poseStack.popPose();
		}
		Vec3 tip = points[n];
		poseStack.pushPose();
		poseStack.translate(tip.x, tip.y, tip.z);
		orient(poseStack, points[n].subtract(points[n - 1]));
		poseStack.translate(0, 0, -0.05);
		collector.submitModel(hand, state, poseStack, TEXTURE, 0xF000F0, OverlayTexture.NO_OVERLAY, state.outlineColor,
				null);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	/** Tourne le repère pour que son axe +z suive la direction donnée. */
	private static void orient(PoseStack poseStack, Vec3 d) {
		float yaw = (float) Mth.atan2(d.x, d.z);
		float pitch = (float) Mth.atan2(d.y, Math.hypot(d.x, d.z));
		poseStack.mulPose(Axis.YP.rotation(yaw));
		poseStack.mulPose(Axis.XP.rotation(-pitch));
	}
}
