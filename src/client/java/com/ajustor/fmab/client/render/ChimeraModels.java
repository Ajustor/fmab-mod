package com.ajustor.fmab.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * Les chimères de Tucker, sur des corps à elles plutôt que sur ceux de l'ours ou de l'araignée : la
 * grande chimère, lion devant, loup derrière, une queue de serpent qui ondule ; la chimère rampante,
 * un tronc cousu sur six membres dépareillés. Leurs peaux sont peintes par {@code tools/skins/creatures.py}.
 */
public final class ChimeraModels {
	private ChimeraModels() {
	}

	/** La phase de la marche, celle de l'animation vanilla des quadrupèdes. */
	private static final float STRIDE = 0.6662f;

	/** L'état de rendu de la grande chimère : en plus de la marche, à quel point elle se cabre. */
	public static class BeastState extends LivingEntityRenderState {
		public float rear;
	}

	/**
	 * La grande chimère : un torse de lion couché à l'horizontale, la crinière autour du cou, une tête
	 * au museau court, deux pattes de lion devant, deux de loup derrière, et la queue de serpent en
	 * quatre segments, tête comprise.
	 */
	public static class Beast extends EntityModel<BeastState> {
		private final ModelPart body;
		private final ModelPart head;
		private final ModelPart[] legs;
		private final ModelPart[] tail;

		public Beast(ModelPart root) {
			super(root);
			body = root.getChild("body");
			head = body.getChild("head");
			legs = new ModelPart[]{root.getChild("fore_right"), root.getChild("fore_left"), root.getChild("hind_right"),
					root.getChild("hind_left")};
			ModelPart t1 = body.getChild("tail"), t2 = t1.getChild("tail2"), t3 = t2.getChild("tail3"),
					snake = t3.getChild("snake");
			tail = new ModelPart[]{t1, t2, t3, snake};
		}

		public static ModelPart bake() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
					.texOffs(0, 0).addBox(-5, -5, -9, 10, 10, 18)
					.texOffs(56, 0).addBox(-6, -6, -11, 12, 12, 6), PartPose.offset(0, 9, 0));
			PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
					.texOffs(92, 0).addBox(-4, -4, -6, 8, 8, 6)
					.texOffs(92, 14).addBox(-2.5f, 0, -9, 5, 4, 3)
					.texOffs(108, 14).addBox(-4, -6, -3, 2, 2, 1)
					.texOffs(108, 14).addBox(2, -6, -3, 2, 2, 1, true), PartPose.offset(0, -2, -11));
			PartDefinition t1 = body.addOrReplaceChild("tail", CubeListBuilder.create()
					.texOffs(32, 28).addBox(-1.5f, -1.5f, 0, 3, 3, 6), PartPose.offsetAndRotation(0, -3, 8, 0.7f, 0, 0));
			PartDefinition t2 = t1.addOrReplaceChild("tail2", CubeListBuilder.create()
					.texOffs(50, 28).addBox(-1, -1, 0, 2, 2, 5), PartPose.offset(0, 0, 5.5f));
			PartDefinition t3 = t2.addOrReplaceChild("tail3", CubeListBuilder.create()
					.texOffs(64, 28).addBox(-1, -1, 0, 2, 2, 4), PartPose.offset(0, 0, 4.5f));
			t3.addOrReplaceChild("snake", CubeListBuilder.create()
					.texOffs(76, 28).addBox(-1.5f, -1, 0, 3, 2, 4), PartPose.offset(0, 0, 3.5f));
			for (int i = 0; i < 4; i++) {
				boolean fore = i < 2, right = i % 2 == 0;
				root.addOrReplaceChild((fore ? "fore_" : "hind_") + (right ? "right" : "left"), CubeListBuilder.create()
								.texOffs(fore ? 0 : 16, 28).addBox(-2, 0, -2, 4, 10, 4, !right),
						PartPose.offset(right ? -3.5f : 3.5f, 14, fore ? -6 : 7));
			}
			return LayerDefinition.create(mesh, 128, 64).bakeRoot();
		}

		@Override
		public void setupAnim(BeastState state) {
			super.setupAnim(state);
			float pos = state.walkAnimationPos * STRIDE, speed = Math.min(1, state.walkAnimationSpeed);
			head.yRot = state.yRot * Mth.DEG_TO_RAD;
			head.xRot = state.xRot * Mth.DEG_TO_RAD;
			// Les pattes en diagonale, comme un fauve qui trotte.
			legs[0].xRot = Mth.cos(pos) * 1.2f * speed;
			legs[3].xRot = Mth.cos(pos) * 1.2f * speed;
			legs[1].xRot = Mth.cos(pos + Mth.PI) * 1.2f * speed;
			legs[2].xRot = Mth.cos(pos + Mth.PI) * 1.2f * speed;
			// Cabrée : le torse se dresse sur les pattes arrière, les pattes avant battent l'air.
			float rear = state.rear;
			body.xRot = -rear * 0.9f;
			body.y = 9 - rear * 6;
			legs[0].xRot -= rear * 1.4f;
			legs[1].xRot -= rear * 1.2f;
			legs[0].y = legs[1].y = 14 - rear * 9;
			legs[0].z = legs[1].z = -6 + rear * 5;
			// La queue de serpent ondule, chaque segment en retard sur le précédent, la tête dressée.
			float age = state.ageInTicks;
			for (int i = 0; i < tail.length; i++) {
				tail[i].yRot = Mth.sin(age * 0.15f - i * 0.9f) * (0.25f + i * 0.08f);
			}
			tail[0].xRot = 0.7f + Mth.sin(age * 0.08f) * 0.1f;
			tail[1].xRot = 0.35f;
			tail[3].xRot = -0.6f + Mth.sin(age * 0.2f) * 0.15f;
		}
	}

	/**
	 * La chimère rampante : un tronc de chien, une tête de chien au cou tendu, et six membres cousus
	 * à ses flancs : deux pattes de chien devant, deux bras d'homme au milieu, deux pattes de chitine
	 * derrière, chacun avec son propre rythme.
	 */
	public static class Crawler extends EntityModel<LivingEntityRenderState> {
		private final ModelPart head;
		private final ModelPart body;
		private final ModelPart[] limbs;

		public Crawler(ModelPart root) {
			super(root);
			body = root.getChild("body");
			head = root.getChild("head");
			limbs = new ModelPart[6];
			for (int i = 0; i < 6; i++) {
				limbs[i] = root.getChild("limb" + i);
			}
		}

		public static ModelPart bake() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-4, -3, -6, 8, 6, 12),
					PartPose.offset(0, 17, 0));
			root.addOrReplaceChild("head", CubeListBuilder.create()
					.texOffs(0, 0).addBox(-4, -4, -8, 8, 8, 8)
					.texOffs(32, 0).addBox(-2, -2, 0, 4, 4, 4), PartPose.offset(0, 16, -7));
			// Pattes de chien (devant), bras d'homme (milieu), pattes de chitine (derrière).
			int[][] uv = {{40, 16}, {52, 16}, {0, 34}};
			for (int i = 0; i < 6; i++) {
				int pair = i / 2;
				boolean right = i % 2 == 0;
				float side = right ? -1 : 1;
				float z = -4 + pair * 4;
				CubeListBuilder cubes = CubeListBuilder.create().texOffs(uv[pair][0], uv[pair][1]).mirror(!right);
				if (pair == 0) {
					cubes.addBox(-1.5f, 0, -1.5f, 3, 8, 3);
				} else if (pair == 1) {
					cubes.addBox(-1.5f, 0, -1.5f, 3, 10, 3);
				} else {
					cubes.addBox(right ? -16 : 0, -1, -1, 16, 2, 2);
				}
				root.addOrReplaceChild("limb" + i, cubes, PartPose.offsetAndRotation(side * 4, pair == 2 ? 17 : 16, z,
						0, 0, pair == 1 ? side * -0.5f : pair == 2 ? side * 0.55f : side * -0.25f));
			}
			return LayerDefinition.create(mesh, 64, 64).bakeRoot();
		}

		@Override
		public void setupAnim(LivingEntityRenderState state) {
			super.setupAnim(state);
			float pos = state.walkAnimationPos * STRIDE, speed = Math.min(1, state.walkAnimationSpeed);
			head.yRot = state.yRot * Mth.DEG_TO_RAD;
			head.xRot = state.xRot * Mth.DEG_TO_RAD;
			// Chaque membre a sa phase : une démarche désordonnée, de bête mal assemblée.
			float[] phase = {0, Mth.PI, 2.1f, 2.1f + Mth.PI, 4.2f, 4.2f + Mth.PI};
			for (int i = 0; i < 6; i++) {
				ModelPart limb = limbs[i];
				float swing = Mth.cos(pos * (i >= 4 ? 1.3f : 1) + phase[i]) * speed;
				if (i >= 4) {
					// La chitine balaie le sol comme une patte d'araignée.
					limb.yRot = swing * 0.6f;
				} else {
					limb.xRot = swing * (i >= 2 ? 0.9f : 1.2f);
				}
			}
			body.zRot = Mth.sin(pos * 0.5f) * 0.06f * speed;
			body.y = 17 + Math.abs(Mth.sin(pos)) * speed;
		}
	}
}
