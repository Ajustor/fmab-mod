package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.WheelchairEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Le fauteuil roulant d'Amestris : assise et dossier de bois, cadre de fer, deux grandes roues qui
 * tournent au rythme du sol, deux roulettes devant, des poignées derrière pour qui pousse.
 */
public class WheelchairRenderer extends MobRenderer<WheelchairEntity, WheelchairRenderer.State, WheelchairRenderer.Model> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/wheelchair.png");

	public static class State extends LivingEntityRenderState {
		float wheel;
	}

	public static class Model extends EntityModel<State> {
		private final ModelPart rightWheel;
		private final ModelPart leftWheel;

		Model(ModelPart root) {
			super(root);
			rightWheel = root.getChild("right_wheel");
			leftWheel = root.getChild("left_wheel");
		}

		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			rightWheel.xRot = state.wheel;
			leftWheel.xRot = state.wheel;
		}

		/** Le sol est à y = 24, l'avant vers -z, comme pour tout modèle d'entité. */
		static LayerDefinition layer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			root.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(0, 0).addBox(-5, 15, -5, 10, 2, 10),
					PartPose.ZERO);
			root.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 12).addBox(-5, 5, 4, 10, 10, 1),
					PartPose.ZERO);
			root.addOrReplaceChild("armrests", CubeListBuilder.create().texOffs(24, 12).addBox(-6, 10, -3, 1, 1, 8)
					.texOffs(24, 12).addBox(5, 10, -3, 1, 1, 8), PartPose.ZERO);
			root.addOrReplaceChild("handles", CubeListBuilder.create().texOffs(44, 12).addBox(-5, 5, 5, 1, 1, 3)
					.texOffs(44, 12).addBox(4, 5, 5, 1, 1, 3), PartPose.ZERO);
			root.addOrReplaceChild("casters", CubeListBuilder.create().texOffs(44, 18).addBox(-4.5f, 21, -6, 1, 3, 3)
					.texOffs(44, 18).addBox(3.5f, 21, -6, 1, 3, 3), PartPose.ZERO);
			root.addOrReplaceChild("footrest", CubeListBuilder.create().texOffs(24, 24).addBox(-4, 20, -8, 8, 1, 3),
					PartPose.ZERO);
			root.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(48, 24).addBox(-5, 15, -6, 1, 6, 1)
					.texOffs(48, 24).addBox(4, 15, -6, 1, 6, 1), PartPose.ZERO);
			wheel(root, "right_wheel", -6.5f);
			wheel(root, "left_wheel", 6.5f);
			return LayerDefinition.create(mesh, 64, 64);
		}

		/** Une roue de cinq pixels de rayon : un disque, doublé d'un second tourné d'un huitième de tour. */
		private static void wheel(PartDefinition root, String name, float x) {
			PartDefinition wheel = root.addOrReplaceChild(name,
					CubeListBuilder.create().texOffs(0, 24).addBox(-0.5f, -5, -5, 1, 10, 10), PartPose.offset(x, 19, 1));
			wheel.addOrReplaceChild("rim", CubeListBuilder.create().texOffs(0, 24).addBox(-0.5f, -5, -5, 1, 10, 10),
					PartPose.rotation(Mth.PI / 4, 0, 0));
		}
	}

	public WheelchairRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(Model.layer().bakeRoot()), 0.45f);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(WheelchairEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.wheel = entity.wheel(partialTicks);
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURE;
	}
}
