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

	/**
	 * Olivier : la poitrine, sa longue chevelure blonde qui tombe dans le dos, le col de fourrure du
	 * manteau de Briggs et le sabre à la hanche gauche.
	 */
	public static ModelPart olivier() {
		MeshDefinition mesh = mesh(true);
		PartDefinition body = mesh.getRoot().getChild("body");
		bust(body, 24, 0, 6, 2, 2.5f, -0.15f);
		body.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(64, 0).addBox(-4, -1, 2.3f, 8, 11, 1),
				PartPose.ZERO);
		// Le col de fourrure couvre aussi le haut des épaules.
		body.addOrReplaceChild("collar", CubeListBuilder.create().texOffs(82, 0).addBox(-7, -1.5f, -3, 14, 2, 6),
				PartPose.ZERO);
		// Le sabre pend à la hanche gauche, derrière le bras.
		body.addOrReplaceChild("sabre", CubeListBuilder.create().texOffs(122, 0).addBox(-0.5f, 0, -0.5f, 1, 10, 1),
				PartPose.offset(3.2f, 9, 2.6f));
		return bakeWide(mesh);
	}

	/** Scar : une carrure de lutteur, les épaules larges et les deltoïdes saillants. */
	public static ModelPart scar() {
		MeshDefinition mesh = mesh(false);
		PartDefinition root = mesh.getRoot();
		root.getChild("right_arm").addOrReplaceChild("shoulder",
				CubeListBuilder.create().texOffs(64, 0).addBox(-3.5f, -2.5f, -2.5f, 5, 3, 5), PartPose.ZERO);
		root.getChild("left_arm").addOrReplaceChild("shoulder",
				CubeListBuilder.create().texOffs(64, 8).addBox(-1.5f, -2.5f, -2.5f, 5, 3, 5), PartPose.ZERO);
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

	/** Une silhouette de joueur, sans volume de plus : la peau dit tout. */
	public static ModelPart plain(boolean slim) {
		return bake(mesh(slim));
	}

	/** Une femme : la poitrine, sous la surcouche de la veste. */
	public static ModelPart woman(boolean slim) {
		MeshDefinition mesh = mesh(slim);
		bust(mesh.getRoot().getChild("body"), 24, 0, 6, 2, 2.5f, -0.15f);
		return bake(mesh);
	}

	/**
	 * Izumi : la poitrine, ses dreadlocks nouées en une grosse queue qui tombe dans le dos et deux mèches
	 * qui retombent devant les épaules (peau élargie, volumes à droite).
	 */
	public static ModelPart izumi() {
		MeshDefinition mesh = mesh(true);
		PartDefinition root = mesh.getRoot();
		bust(root.getChild("body"), 24, 0, 6, 2, 2.5f, -0.15f);
		PartDefinition head = root.getChild("head");
		head.addOrReplaceChild("dreads", CubeListBuilder.create().texOffs(64, 0).addBox(-1.5f, -5, 4, 3, 13, 2),
				PartPose.ZERO);
		head.addOrReplaceChild("locks", CubeListBuilder.create().texOffs(78, 0).addBox(-4.6f, -3, -2.5f, 1, 6, 1)
				.texOffs(78, 0).mirror().addBox(3.6f, -3, -2.5f, 1, 6, 1), PartPose.ZERO);
		return bakeWide(mesh);
	}

	/** Winry : la poitrine, et la queue de cheval nouée haut qui tombe dans la nuque. */
	public static ModelPart winry() {
		MeshDefinition mesh = mesh(true);
		PartDefinition root = mesh.getRoot();
		bust(root.getChild("body"), 24, 0, 6, 2, 2.5f, -0.15f);
		ponytail(root.getChild("head"), 11, -6, 0.22f);
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

	/**
	 * Alphonse, d'après l'anime : une armure massive à la tête petite. Le heaume à la mentonnière en
	 * grille, sa crête, sa pointe et la longue houppe ; un plastron énorme ; des épaulières rondes
	 * hérissées d'une pointe, des brassards épais ; des jambes écartées et très épaisses (cuissards,
	 * grèves, solerets), une pointe à chaque genou, une sacoche sur la cuisse droite ; le long tablier
	 * de toile qui tombe jusqu'aux genoux (peau élargie, volumes à droite).
	 */
	public static ModelPart alphonse() {
		MeshDefinition mesh = mesh(false);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.getChild("head");
		head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(100, 32).addBox(-2.5f, -4, -5.5f, 5, 4, 2),
				PartPose.ZERO);
		head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(100, 38).addBox(-1, -9, -4.5f, 2, 1, 6),
				PartPose.ZERO);
		head.addOrReplaceChild("horn", CubeListBuilder.create().texOffs(114, 32).addBox(-1, -11.5f, -3.5f, 2, 3, 2)
				.texOffs(116, 38).addBox(-0.5f, -14, -3, 1, 3, 1), PartPose.ZERO);
		head.addOrReplaceChild("plume", CubeListBuilder.create().texOffs(122, 30).addBox(-1, -9, 3.5f, 2, 14, 1),
				PartPose.ZERO);
		PartDefinition body = root.getChild("body");
		body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(64, 0).addBox(-5.5f, -1, -3.5f, 11, 8, 7),
				PartPose.ZERO);
		body.addOrReplaceChild("apron", CubeListBuilder.create().texOffs(100, 20).addBox(-3, 9, -3.6f, 6, 11, 1),
				PartPose.ZERO);
		body.addOrReplaceChild("back_flap", CubeListBuilder.create().texOffs(114, 20).addBox(-2.5f, 9, 2.6f, 5, 7, 1),
				PartPose.ZERO);
		// Les épaulières tiennent aux épaules, pas aux bras : elles ne tournent pas quand le bras balance.
		body.addOrReplaceChild("right_pauldron", CubeListBuilder.create().texOffs(64, 15)
				.addBox(-11, -2.5f, -4.5f, 9, 6, 9).texOffs(116, 38).addBox(-7, -5.5f, -0.5f, 1, 3, 1), PartPose.ZERO);
		body.addOrReplaceChild("left_pauldron", CubeListBuilder.create().texOffs(64, 30)
				.addBox(2, -2.5f, -4.5f, 9, 6, 9).texOffs(116, 38).addBox(6, -5.5f, -0.5f, 1, 3, 1), PartPose.ZERO);
		root.getChild("right_arm").addOrReplaceChild("bracer", CubeListBuilder.create().texOffs(100, 0)
				.addBox(-4, 4, -3, 6, 5, 6), PartPose.ZERO);
		PartDefinition left = root.getChild("left_arm");
		left.addOrReplaceChild("bracer", CubeListBuilder.create().texOffs(100, 0).mirror().addBox(-2, 4, -3, 6, 5, 6),
				PartPose.ZERO);
		// Les jambes, écartées par la carrure : cuissard, grève et soleret autour de chacune.
		for (boolean isLeft : new boolean[]{false, true}) {
			PartDefinition leg = root.addOrReplaceChild(isLeft ? "left_leg" : "right_leg", CubeListBuilder.create()
					.texOffs(isLeft ? 16 : 0, isLeft ? 48 : 16).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(isLeft ? 3.1f : -3.1f, 12, 0));
			leg.addOrReplaceChild("thigh", CubeListBuilder.create().texOffs(64, 45).mirror(isLeft)
					.addBox(-3, 0, -3, 6, 6, 6), PartPose.ZERO);
			leg.addOrReplaceChild("greave", CubeListBuilder.create().texOffs(88, 45).mirror(isLeft)
					.addBox(-3, 6.5f, -3, 6, 4, 6), PartPose.ZERO);
			leg.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(100, 11).mirror(isLeft)
					.addBox(-3, 10, -4, 6, 2, 7), PartPose.ZERO);
			leg.addOrReplaceChild("knee", CubeListBuilder.create().texOffs(120, 38).addBox(-0.5f, 5, -4.2f, 1, 2, 1),
					PartPose.ZERO);
		}
		root.getChild("right_leg").addOrReplaceChild("pouch", CubeListBuilder.create().texOffs(112, 45)
				.addBox(-4.6f, 1, -1, 2, 3, 2), PartPose.ZERO);
		return bakeWide(mesh);
	}

	/**
	 * Armstrong : la boucle blonde dressée sur le crâne nu, la moustache en guidon qui déborde des
	 * joues, et des épaules de colosse (peau élargie, volumes à droite).
	 */
	public static ModelPart armstrong() {
		MeshDefinition mesh = mesh(false);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.getChild("head");
		head.addOrReplaceChild("curl", CubeListBuilder.create().texOffs(64, 0).addBox(-1, -10.5f, -4, 2, 3, 2),
				PartPose.rotation(-0.2f, 0, 0));
		// La moustache : une barre fine sous le nez, et deux crocs qui retombent au bout.
		head.addOrReplaceChild("stache", CubeListBuilder.create().texOffs(72, 0).addBox(-5, -2.7f, -4.6f, 10, 1, 1)
				.texOffs(72, 4).addBox(-5, -1.7f, -4.6f, 1, 2, 1)
				.texOffs(72, 4).mirror().addBox(4, -1.7f, -4.6f, 1, 2, 1), PartPose.ZERO);
		root.getChild("right_arm").addOrReplaceChild("shoulder",
				CubeListBuilder.create().texOffs(64, 8).addBox(-3.5f, -2.5f, -2.5f, 5, 3, 5), PartPose.ZERO);
		root.getChild("left_arm").addOrReplaceChild("shoulder",
				CubeListBuilder.create().texOffs(64, 16).addBox(-1.5f, -2.5f, -2.5f, 5, 3, 5), PartPose.ZERO);
		return bakeWide(mesh);
	}

	/** Pinako : le chignon dressé sur le crâne, la longue pipe au coin de la bouche et son fourneau. */
	public static ModelPart pinako() {
		MeshDefinition mesh = mesh(true);
		PartDefinition head = mesh.getRoot().getChild("head");
		head.addOrReplaceChild("bun", CubeListBuilder.create().texOffs(56, 20).addBox(-1, -10, -1, 2, 2, 2),
				PartPose.ZERO);
		head.addOrReplaceChild("pipe", CubeListBuilder.create().texOffs(24, 0).addBox(1.5f, -1.6f, -7, 1, 1, 3),
				PartPose.ZERO);
		head.addOrReplaceChild("bowl", CubeListBuilder.create().texOffs(56, 16).addBox(1.5f, -3.1f, -7.5f, 1, 2, 1),
				PartPose.ZERO);
		return bake(mesh);
	}

	/** Hawkeye : la poitrine, le chignon relevé à l'arrière de la tête, l'étui du pistolet sur la hanche droite. */
	public static ModelPart hawkeye() {
		MeshDefinition mesh = mesh(true);
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.getChild("body");
		bust(body, 24, 0, 6, 2, 2.5f, -0.15f);
		root.getChild("head").addOrReplaceChild("bun", CubeListBuilder.create().texOffs(56, 16)
				.addBox(-1.5f, -6.5f, 4, 3, 3, 2), PartPose.ZERO);
		body.addOrReplaceChild("holster", CubeListBuilder.create().texOffs(56, 24).addBox(-4.8f, 9, -1, 1, 3, 2),
				PartPose.ZERO);
		return bake(mesh);
	}

	/**
	 * Kimblee : le feutre blanc à bord, posé sur le crâne, et la longue queue de cheval noire nouée bas
	 * (peau élargie, volumes à droite).
	 */
	public static ModelPart kimblee() {
		MeshDefinition mesh = mesh(false);
		PartDefinition head = mesh.getRoot().getChild("head");
		head.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(64, 0).addBox(-5.5f, -8.6f, -5.5f, 11, 1, 11),
				PartPose.ZERO);
		head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(64, 12).addBox(-4, -11, -4, 8, 3, 8),
				PartPose.ZERO);
		head.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(96, 12).addBox(-1, 0, 0, 2, 8, 2),
				PartPose.offsetAndRotation(0, -2, 4, 0.12f, 0, 0));
		return bakeWide(mesh);
	}

	/** Ling : la petite queue de cheval nouée sur la nuque. */
	public static ModelPart ling() {
		MeshDefinition mesh = mesh(false);
		ponytail(mesh.getRoot().getChild("head"), 5, -3, 0.2f);
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
