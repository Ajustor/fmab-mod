package com.ajustor.fmab.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;

/**
 * Des silhouettes pour les personnages au squelette de joueur (avec sa surcouche) : une poitrine,
 * une queue de cheval, des chignons, une visière, un col de fourrure... Les petits volumes se
 * texturent dans les coins que la peau de joueur laisse libres, (24, 0) sur 16x8 et (56, 16) sur
 * 8x32 ; les plus grands demandent une peau élargie à 128x64, dont la moitié droite leur revient.
 */
public final class FigureModels {
	private FigureModels() {
	}

	private static MeshDefinition mesh(boolean slim) {
		return PlayerModel.createMesh(CubeDeformation.NONE, slim);
	}

	private static ModelPart bake(MeshDefinition mesh) {
		return LayerDefinition.create(mesh, 64, 64).bakeRoot();
	}

	/** Pour une peau élargie à 128x64 : la disposition du joueur à gauche, les volumes à droite. */
	private static ModelPart bakeWide(MeshDefinition mesh) {
		return LayerDefinition.create(mesh, 128, 64).bakeRoot();
	}

	/**
	 * Une poitrine sous les épaules : un volume un peu enfoncé dans le buste et incliné, pour que le
	 * bas avance plus que le haut sans laisser de jour contre le corps.
	 *
	 * @param depth l'épaisseur, dont une part (un demi-pixel pour 2, un pixel pour 3) dans le buste
	 * @param tilt  l'inclinaison, en radians (négative : le bas avance)
	 */
	static void bust(PartDefinition body, int u, int v, int width, int depth, float y, float tilt) {
		float embed = depth / 2f - 0.5f;
		body.addOrReplaceChild("bust", CubeListBuilder.create().texOffs(u, v)
						.addBox(-width / 2f, 0, -(depth - embed), width, 3, depth),
				PartPose.offsetAndRotation(0, y, -2, tilt, 0, 0));
	}

	/** Olivier : la poitrine, et sa longue chevelure blonde qui tombe dans le dos. */
	public static ModelPart olivier() {
		MeshDefinition mesh = mesh(true);
		PartDefinition body = mesh.getRoot().getChild("body");
		bust(body, 24, 0, 6, 2, 2.5f, -0.15f);
		body.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(64, 0).addBox(-4, -1, 2.3f, 8, 11, 1),
				PartPose.ZERO);
		return bakeWide(mesh);
	}

	/** Scar : un torse de lutteur, les pectoraux saillants sous le maillot. */
	public static ModelPart scar() {
		MeshDefinition mesh = mesh(false);
		mesh.getRoot().getChild("body").addOrReplaceChild("chest",
				CubeListBuilder.create().texOffs(64, 0).addBox(-4, 1, -3, 8, 3, 1), PartPose.ZERO);
		return bakeWide(mesh);
	}

	/** Cornello : le soleil de Léto en médaillon sur la poitrine, la robe de prêtre qui s'évase. */
	public static ModelPart cornello() {
		MeshDefinition mesh = mesh(false);
		PartDefinition body = mesh.getRoot().getChild("body");
		body.addOrReplaceChild("medallion",
				CubeListBuilder.create().texOffs(64, 0).addBox(-2, 1.5f, -3, 4, 4, 1), PartPose.ZERO);
		body.addOrReplaceChild("robe",
				CubeListBuilder.create().texOffs(64, 8).addBox(-5, 10, -3, 10, 6, 6), PartPose.ZERO);
		return bakeWide(mesh);
	}

	/** Marcoh : la sacoche de médecin qu'il porte en bandoulière, sur la hanche, dans le dos. */
	public static ModelPart marcoh() {
		MeshDefinition mesh = mesh(false);
		mesh.getRoot().getChild("body").addOrReplaceChild("satchel",
				CubeListBuilder.create().texOffs(64, 0).addBox(-4.5f, 7.5f, 2, 4, 3, 2), PartPose.ZERO);
		return bakeWide(mesh);
	}

	/** Un soldat de Briggs : le col de fourrure de la parka, les lunettes de neige, le sac au dos. */
	public static ModelPart briggs() {
		MeshDefinition mesh = mesh(false);
		PartDefinition root = mesh.getRoot();
		root.getChild("body").addOrReplaceChild("collar",
				CubeListBuilder.create().texOffs(64, 0).addBox(-5, -1, -3, 10, 2, 6), PartPose.ZERO);
		root.getChild("body").addOrReplaceChild("pack",
				CubeListBuilder.create().texOffs(64, 12).addBox(-3, 1.5f, 2, 6, 7, 3), PartPose.ZERO);
		root.getChild("head").addOrReplaceChild("goggles",
				CubeListBuilder.create().texOffs(64, 8).addBox(-3.5f, -6.8f, -5.3f, 7, 2, 1), PartPose.ZERO);
		return bakeWide(mesh);
	}

	/** Un soldat de Drachma : la chapka bouffante et ses rabats, les pans de la capote. */
	public static ModelPart drachma() {
		MeshDefinition mesh = mesh(false);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.getChild("head");
		head.addOrReplaceChild("chapka",
				CubeListBuilder.create().texOffs(64, 0).addBox(-5, -10, -5, 10, 2, 10), PartPose.ZERO);
		head.addOrReplaceChild("flaps", CubeListBuilder.create().texOffs(64, 12)
				.addBox(-5.5f, -5, -2.5f, 1, 5, 5)
				.addBox(4.5f, -5, -2.5f, 1, 5, 5, true), PartPose.ZERO);
		root.getChild("body").addOrReplaceChild("skirt",
				CubeListBuilder.create().texOffs(64, 24).addBox(-4.5f, 10, -2.5f, 9, 5, 5), PartPose.ZERO);
		return bakeWide(mesh);
	}

	/** Une femme : la poitrine, sous la surcouche de la veste. */
	public static ModelPart woman(boolean slim) {
		MeshDefinition mesh = mesh(slim);
		bust(mesh.getRoot().getChild("body"), 24, 0, 6, 2, 2.5f, -0.15f);
		return bake(mesh);
	}

	/** Winry : la poitrine, et la queue de cheval nouée haut qui tombe dans la nuque. */
	public static ModelPart winry() {
		MeshDefinition mesh = mesh(true);
		PartDefinition root = mesh.getRoot();
		bust(root.getChild("body"), 24, 0, 6, 2, 2.5f, -0.15f);
		ponytail(root.getChild("head"), 8, -6, 0.22f);
		return bake(mesh);
	}

	/** Hohenheim : les cheveux noués bas, sur la nuque. */
	public static ModelPart hohenheim() {
		MeshDefinition mesh = mesh(false);
		ponytail(mesh.getRoot().getChild("head"), 7, -3, 0.15f);
		return bake(mesh);
	}

	/** Une queue de cheval de 2 sur 2, attachée derrière la tête à la hauteur donnée. */
	private static void ponytail(PartDefinition head, int length, float y, float tilt) {
		head.addOrReplaceChild("ponytail", CubeListBuilder.create().texOffs(56, 16).addBox(-1, 0, 0, 2, length, 2),
				PartPose.offsetAndRotation(0, y, 4, tilt, 0, 0));
	}

	/** May Chang : les deux chignons sur le haut du crâne, et les nattes qui en retombent. */
	public static ModelPart mayChang() {
		MeshDefinition mesh = mesh(true);
		PartDefinition head = mesh.getRoot().getChild("head");
		head.addOrReplaceChild("buns", CubeListBuilder.create().texOffs(24, 0)
				.addBox(-5.5f, -9, -1, 3, 3, 3)
				.addBox(2.5f, -9, -1, 3, 3, 3, true), PartPose.ZERO);
		head.addOrReplaceChild("braids", CubeListBuilder.create().texOffs(56, 16)
				.addBox(-5.2f, -6, 0.5f, 1, 7, 1)
				.addBox(4.2f, -6, 0.5f, 1, 7, 1, true), PartPose.ZERO);
		return bake(mesh);
	}

	/** Un soldat d'Amestris : la visière de sa casquette, un peu baissée sur les yeux. */
	public static ModelPart soldier() {
		MeshDefinition mesh = mesh(false);
		mesh.getRoot().getChild("head").addOrReplaceChild("visor",
				CubeListBuilder.create().texOffs(24, 0).addBox(-3, 0, -2, 6, 1, 2),
				PartPose.offsetAndRotation(0, -5.2f, -4.4f, 0.3f, 0, 0));
		return bake(mesh);
	}
}
