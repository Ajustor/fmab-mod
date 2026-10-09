package com.ajustor.fmab.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Des silhouettes propres pour les boss, sur le squelette humanoïde : des volumes ajoutés au corps
 * ou à la tête, texturés dans la bande libre de leur peau (y 32 à 48, que le modèle humanoïde
 * n'utilise pas). Ils suivent les mouvements de la partie qui les porte.
 */
public final class BossModels {
	private BossModels() {
	}

	private static MeshDefinition base() {
		return HumanoidModel.createMesh(CubeDeformation.NONE, 0);
	}

	private static ModelPart bake(MeshDefinition mesh) {
		return LayerDefinition.create(mesh, 64, 64).bakeRoot();
	}

	/** Gluttony : un ventre rond qui déborde devant lui. */
	public static ModelPart gluttony() {
		MeshDefinition mesh = base();
		mesh.getRoot().getChild("body").addOrReplaceChild("belly",
				CubeListBuilder.create().texOffs(0, 32).addBox(-5, 3, -5, 10, 8, 4), PartPose.ZERO);
		return bake(mesh);
	}

	/** Sloth : des épaules et un dos massifs, une carrure de colosse. */
	public static ModelPart sloth() {
		MeshDefinition mesh = base();
		mesh.getRoot().getChild("body").addOrReplaceChild("shoulders",
				CubeListBuilder.create().texOffs(0, 32).addBox(-6, -1, -3, 12, 5, 6), PartPose.ZERO);
		return bake(mesh);
	}

	/** Père : une longue barbe et une chevelure qui tombe dans le dos. */
	public static ModelPart father() {
		MeshDefinition mesh = base();
		PartDefinition head = mesh.getRoot().getChild("head");
		head.addOrReplaceChild("beard",
				CubeListBuilder.create().texOffs(0, 32).addBox(-3.5f, -2, -4.6f, 7, 7, 1), PartPose.ZERO);
		head.addOrReplaceChild("mane",
				CubeListBuilder.create().texOffs(16, 32).addBox(-4.5f, -8.2f, 2.6f, 9, 11, 2), PartPose.ZERO);
		return bake(mesh);
	}
}
