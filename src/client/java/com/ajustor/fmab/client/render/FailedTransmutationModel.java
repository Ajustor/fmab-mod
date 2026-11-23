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
 * Ce qui sort d'une transmutation humaine : une masse de chair à vif portée par six bras humains,
 * maigres, qui s'écartent et retombent jusqu'au sol comme des pattes d'araignée. La tête pend à
 * l'envers sous l'avant, l'œil unique tourné vers l'arrière, la mâchoire ouverte ; une bosse et une
 * crête de vertèbres percent le dos, des entrailles traînent derrière. La chair respire mal. Sa peau
 * est peinte par {@code tools/skins/creatures.py} (« failed_transmutation »).
 */
public class FailedTransmutationModel extends EntityModel<LivingEntityRenderState> {
	private static final float STRIDE = 0.6662f;
	private static final int LIMBS = 6;

	private final ModelPart mass;
	private final ModelPart head;
	private final ModelPart[] limbs = new ModelPart[LIMBS];

	public FailedTransmutationModel(ModelPart root) {
		super(root);
		mass = root.getChild("mass");
		head = mass.getChild("head");
		for (int i = 0; i < LIMBS; i++) {
			limbs[i] = root.getChild("limb" + i);
		}
	}

	public static ModelPart bake() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition mass = root.addOrReplaceChild("mass", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5, -6, -6, 10, 6, 12), PartPose.offsetAndRotation(0, 14, 0, 0, 0, 0.1f));
		mass.addOrReplaceChild("hump", CubeListBuilder.create().texOffs(44, 0).addBox(-3, -4, -3, 6, 4, 6),
				PartPose.offsetAndRotation(-1.5f, -5, -1, -0.2f, 0.4f, -0.2f));
		for (int i = 0; i < 5; i++) {
			mass.addOrReplaceChild("vertebra" + i, CubeListBuilder.create().texOffs(40, 20).addBox(-1, -2, -1, 2, 2, 2),
					PartPose.offsetAndRotation(1, -6, -6 + i * 3, -0.4f, 0, 0.2f * i));
		}
		// La tête pend sous l'avant de la masse, à l'envers, l'œil vers l'arrière.
		PartDefinition head = mass.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(68, 0).addBox(-3, -5, -6, 6, 5, 6), PartPose.offsetAndRotation(0.5f, 0, -3, Mth.PI - 0.5f, 0.3f, 0.2f));
		head.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(104, 20).addBox(-1, -1, -1, 2, 2, 1),
				PartPose.offset(1, -3, -6));
		head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(110, 20).addBox(-2, 0, -3, 4, 2, 3),
				PartPose.offsetAndRotation(0, 0, -2.5f, 0.6f, 0, 0.15f));
		// Des entrailles qui traînent derrière, en anneaux.
		PartDefinition gut = mass.addOrReplaceChild("gut0", CubeListBuilder.create().texOffs(20, 20)
				.addBox(-1, -1, 0, 2, 2, 6), PartPose.offsetAndRotation(-2, 0, 4, 0.9f, 0, 0));
		gut.addOrReplaceChild("gut1", CubeListBuilder.create().texOffs(20, 20).addBox(-1, -1, 0, 2, 2, 6),
				PartPose.offsetAndRotation(0, 0, 5.5f, 0.15f, 0.6f, 0));
		for (int i = 0; i < LIMBS; i++) {
			float side = i % 2 == 0 ? -1 : 1;
			float z = -4.5f + (i / 2) * 4.5f;
			// Le bras s'écarte presque à l'horizontale, l'avant-bras retombe jusqu'au sol, la main à plat.
			PartDefinition arm = root.addOrReplaceChild("limb" + i, CubeListBuilder.create().texOffs(92, 0)
					.addBox(-1, 0, -1, 2, 8, 2), PartPose.offsetAndRotation(side * 4.5f, 12.5f, z, 0,
					side * 0.25f * (i / 2 - 1), side * -1.35f));
			PartDefinition forearm = arm.addOrReplaceChild("forearm", CubeListBuilder.create().texOffs(100, 0)
					.addBox(-1, 0, -1, 2, 9, 2), PartPose.offsetAndRotation(0, 8, 0, 0, 0, side * 1.45f));
			forearm.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(108, 0)
					.addBox(-1.5f, 0, -1.5f, 3, 1, 3), PartPose.offset(0, 9, 0));
		}
		return LayerDefinition.create(mesh, 128, 64).bakeRoot();
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks, pos = state.walkAnimationPos * STRIDE * 1.4f;
		float speed = Math.min(1, state.walkAnimationSpeed);
		// Le souffle : la chair gonfle et retombe, lentement, mal.
		mass.yScale = 1 + 0.07f * Mth.sin(age * 0.12f) + 0.03f * Mth.sin(age * 0.43f);
		mass.y += Math.abs(Mth.sin(pos)) * speed;
		for (int i = 0; i < LIMBS; i++) {
			// Chaque bras à son rythme : une démarche d'araignée mal assemblée.
			float phase = i * 2.1f + (i % 2) * Mth.PI;
			limbs[i].yRot += Mth.cos(pos + phase) * 0.5f * speed;
			limbs[i].zRot += Math.max(0, Mth.sin(pos + phase)) * 0.4f * speed * (i % 2 == 0 ? 1 : -1);
			// Au repos, les doigts tapotent le sol.
			limbs[i].zRot += Mth.sin(age * 0.3f + i) * 0.03f;
		}
		head.yRot += Mth.sin(age * 0.06f) * 0.3f;
	}
}
