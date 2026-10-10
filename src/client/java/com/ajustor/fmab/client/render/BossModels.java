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

	/**
	 * Le squelette humanoïde, avec les membres gauches de la disposition « joueur » (leur propre
	 * dessin dans la peau) plutôt qu'en miroir des membres droits.
	 */
	private static MeshDefinition base() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1, -2, -2, 4, 12, 4),
				PartPose.offset(5, 2, 0));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2, 0, -2, 4, 12, 4),
				PartPose.offset(1.9f, 12, 0));
		return mesh;
	}

	/** Le même squelette aux bras fins (3 pixels), comme le modèle de joueur « slim ». */
	private static MeshDefinition slimBase() {
		MeshDefinition mesh = base();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2, -2, -2, 3, 12, 4),
				PartPose.offset(-5, 2.5f, 0));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1, -2, -2, 3, 12, 4),
				PartPose.offset(5, 2.5f, 0));
		return mesh;
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

	/**
	 * Lust : une longue chevelure noire qui tombe jusqu'aux reins, et une silhouette de femme fatale :
	 * la poitrine haute et marquée sous le décolleté de sa robe, la taille fine, les hanches rondes.
	 */
	public static ModelPart lust() {
		MeshDefinition mesh = slimBase();
		mesh.getRoot().getChild("head").addOrReplaceChild("hair",
				CubeListBuilder.create().texOffs(0, 32).addBox(-4.5f, -8.3f, 2.4f, 9, 14, 2), PartPose.ZERO);
		PartDefinition body = mesh.getRoot().getChild("body");
		FigureModels.bust(body, 24, 32, 7, 3, 3, -0.35f);
		// Les hanches, plus larges que la taille : la robe s'évase en haut des cuisses.
		body.addOrReplaceChild("hips",
				CubeListBuilder.create().texOffs(24, 38).addBox(-4.5f, 9.5f, -2.5f, 9, 3, 5), PartPose.ZERO);
		return bake(mesh);
	}

	/** Envy : la touffe hérissée en palmier, au sommet du crâne. */
	public static ModelPart envy() {
		MeshDefinition mesh = base();
		mesh.getRoot().getChild("head").addOrReplaceChild("spikes",
				CubeListBuilder.create().texOffs(0, 32).addBox(-4.5f, -10.5f, -4.5f, 9, 3, 9), PartPose.ZERO);
		return bake(mesh);
	}

	/** Greed : le col de fourrure de son manteau. */
	public static ModelPart greed() {
		MeshDefinition mesh = base();
		mesh.getRoot().getChild("body").addOrReplaceChild("fur",
				CubeListBuilder.create().texOffs(0, 32).addBox(-5, -1, -3, 10, 3, 6), PartPose.ZERO);
		return bake(mesh);
	}

	/** Wrath : les pans de la vareuse du Généralissime, qui tombent sur les cuisses. */
	public static ModelPart wrath() {
		MeshDefinition mesh = base();
		mesh.getRoot().getChild("body").addOrReplaceChild("coat",
				CubeListBuilder.create().texOffs(0, 32).addBox(-4.5f, 10, -2.5f, 9, 5, 5), PartPose.ZERO);
		return bake(mesh);
	}

	/**
	 * Pride : derrière l'enfant, ses ombres, des lames noires qui se dressent dans son dos. Elles ne
	 * se montrent qu'une fois qu'il s'est révélé.
	 */
	public static ModelPart pride() {
		MeshDefinition mesh = base();
		PartDefinition body = mesh.getRoot().getChild("body");
		body.addOrReplaceChild("shadows", CubeListBuilder.create()
				.texOffs(0, 32).addBox(-6, -10, 2.5f, 2, 14, 1)
				.texOffs(6, 32).addBox(-1, -14, 3, 2, 16, 1)
				.texOffs(12, 32).addBox(4, -9, 2.5f, 2, 13, 1), PartPose.ZERO);
		return bake(mesh);
	}

	/** Barry le Boucher : un cimier sur son heaume. */
	public static ModelPart barry() {
		MeshDefinition mesh = base();
		mesh.getRoot().getChild("head").addOrReplaceChild("crest",
				CubeListBuilder.create().texOffs(0, 32).addBox(-1, -11, -4.5f, 2, 3, 9), PartPose.ZERO);
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
