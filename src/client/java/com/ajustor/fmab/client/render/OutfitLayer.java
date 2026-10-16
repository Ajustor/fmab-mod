package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.item.Outfit;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Les volumes des tenues, par-dessus l'armure peinte : la capuche et les pans du manteau d'Edward,
 * les pans du manteau d'officier, les épaulières et le plumet de l'armure d'Alphonse, la visière de la
 * casquette. Une armure vanilla ne peint que la surface des membres ; ces pièces-là dépassent.
 *
 * <p>Le modèle n'a pas de membres à lui : chaque os est vide et ne porte que ses volumes. Il prend la
 * pose du corps comme le fait l'armure (même animation), plus les gestes de transmutation et le
 * fauteuil roulant pour un joueur.
 */
public class OutfitLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
	private final Map<Outfit, Model<S>> models = new EnumMap<>(Outfit.class);

	public OutfitLayer(RenderLayerParent<S, M> parent) {
		super(parent);
		for (Outfit outfit : Outfit.values()) {
			models.put(outfit, new Model<>(mesh().bakeRoot(), outfit));
		}
	}

	/** La texture des volumes d'une tenue, au gabarit de {@link #mesh()}. */
	public static Identifier texture(Outfit outfit) {
		return Fmab.id("textures/entity/outfit/" + outfit.name().toLowerCase(java.util.Locale.ROOT) + ".png");
	}

	/**
	 * Tous les volumes, sur des os vides. Leur place dans la texture (64x64) : la capuche en (0, 0), la
	 * visière en (24, 0), la pointe du heaume en (48, 0), les épaulières en (0, 8), le plumet en (48, 8),
	 * les pans courts en (0, 20) et les longs en (16, 20).
	 */
	static LayerDefinition mesh() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		CubeDeformation none = CubeDeformation.NONE;
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		// La visière déborde du front, juste sous le bandeau de la casquette.
		head.addOrReplaceChild("visor", CubeListBuilder.create().texOffs(24, 0)
				.addBox(-4, -5.5f, -7.5f, 8, 1, 3, new CubeDeformation(0, -0.3f, 0)), PartPose.ZERO);
		// La pointe au sommet du heaume, et le plumet qui en retombe dans le dos.
		head.addOrReplaceChild("spike", CubeListBuilder.create().texOffs(48, 0)
				.addBox(-1, -12, -1, 2, 3, 2, none), PartPose.ZERO);
		head.addOrReplaceChild("plume", CubeListBuilder.create().texOffs(48, 8)
				.addBox(-1, -10, 4.5f, 2, 7, 3, none), PartPose.rotation(0.25f, 0, 0));
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
		// La capuche rabattue sur le haut du dos, par-dessus le manteau.
		body.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-4, -0.5f, 3, 8, 4, 2, none), PartPose.ZERO);
		PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(),
				PartPose.offset(-5, 2, 0));
		PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(),
				PartPose.offset(5, 2, 0));
		rightArm.addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(0, 8)
				.addBox(-4.5f, -3.3f, -3.5f, 5, 3, 7, new CubeDeformation(0.2f)), PartPose.ZERO);
		leftArm.addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(0, 8).mirror()
				.addBox(-0.5f, -3.3f, -3.5f, 5, 3, 7, new CubeDeformation(0.2f)), PartPose.ZERO);
		PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
				PartPose.offset(-1.9f, 12, 0));
		PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
				PartPose.offset(1.9f, 12, 0));
		// Les pans tombent par-dessus jambières et bottes : un peu plus larges que les deux.
		CubeDeformation tail = new CubeDeformation(1.1f);
		rightLeg.addOrReplaceChild("tails", CubeListBuilder.create().texOffs(0, 20)
				.addBox(-2, 0, -2, 4, 9, 4, tail), PartPose.ZERO);
		leftLeg.addOrReplaceChild("tails", CubeListBuilder.create().texOffs(0, 20).mirror()
				.addBox(-2, 0, -2, 4, 9, 4, tail), PartPose.ZERO);
		rightLeg.addOrReplaceChild("long_tails", CubeListBuilder.create().texOffs(16, 20)
				.addBox(-2, 0, -2, 4, 11, 4, tail), PartPose.ZERO);
		leftLeg.addOrReplaceChild("long_tails", CubeListBuilder.create().texOffs(16, 20).mirror()
				.addBox(-2, 0, -2, 4, 11, 4, tail), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		if (state.isInvisible) {
			return;
		}
		// Une tenue ne se soumet qu'une fois, quel que soit le nombre de ses volumes.
		Set<Outfit> outfits = EnumSet.noneOf(Outfit.class);
		Outfit.extras(state.headEquipment.getItem(), state.chestEquipment.getItem()).forEach(e -> outfits.add(e.outfit()));
		for (Outfit outfit : outfits) {
			collector.order(1).submitModel(models.get(outfit), state, poseStack,
					RenderTypes.armorCutoutNoCull(texture(outfit)), light, LivingEntityRenderer.getOverlayCoords(state, 0.0F),
					state.outlineColor, null);
		}
	}

	/** Les volumes d'une tenue, posés comme l'armure, montrés selon ce qui est porté. */
	static class Model<S extends HumanoidRenderState> extends HumanoidModel<S> {
		private final Outfit outfit;

		Model(ModelPart root, Outfit outfit) {
			super(root);
			this.outfit = outfit;
		}

		@Override
		public void setupAnim(S state) {
			super.setupAnim(state);
			if (state instanceof AvatarRenderState avatar) {
				TransmutationGestures.apply(this, avatar);
				((BodyHolder) avatar).fmab$wheelchair().apply(this, avatar.ageInTicks);
			}
			Set<Outfit.Extra> worn = Outfit.extras(state.headEquipment.getItem(), state.chestEquipment.getItem());
			for (Outfit.Extra extra : Outfit.Extra.values()) {
				boolean shown = extra.outfit() == outfit && worn.contains(extra);
				switch (extra) {
					case VISOR -> head.getChild("visor").visible = shown;
					case HOOD -> body.getChild("hood").visible = shown;
					case TAILS -> {
						rightLeg.getChild("tails").visible = shown;
						leftLeg.getChild("tails").visible = shown;
					}
					case LONG_TAILS -> {
						rightLeg.getChild("long_tails").visible = shown;
						leftLeg.getChild("long_tails").visible = shown;
					}
					case PAULDRONS -> {
						rightArm.getChild("pauldron").visible = shown;
						leftArm.getChild("pauldron").visible = shown;
					}
					case PLUME -> {
						head.getChild("plume").visible = shown;
						head.getChild("spike").visible = shown;
					}
				}
			}
		}
	}
}
