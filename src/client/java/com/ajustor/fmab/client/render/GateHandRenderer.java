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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Un bras noir de la Porte : un long bras d'ombre, une main aux doigts écartés au bout. Il s'oriente
 * vers sa proie et s'allonge jusqu'à elle, en ondulant ; les doigts se referment quand il la tient.
 */
public class GateHandRenderer extends EntityRenderer<GateHandEntity, GateHandRenderer.State> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/gate_hand.png");
	/** Longueur du bras sans proie, en blocs. */
	private static final float RISE = 4;

	public static class State extends EntityRenderState {
		Vec3 toward = new Vec3(0, RISE, 0);
		float reach;
		float age;
		/** Longueur visible du bras, en blocs. */
		float length;
	}

	/** Le bras (un cube d'un bloc qu'on étire) et la main, en pixels. */
	static class Model extends EntityModel<State> {
		final ModelPart arm;
		final ModelPart hand;
		final ModelPart[] fingers = new ModelPart[4];

		Model(ModelPart root) {
			super(root);
			arm = root.getChild("arm");
			hand = root.getChild("hand");
			for (int i = 0; i < fingers.length; i++) {
				fingers[i] = hand.getChild("finger" + i);
			}
		}

		/**
		 * La pose se règle ici : le rendu remet le modèle à zéro juste avant de l'animer, toute pose
		 * posée ailleurs serait perdue.
		 */
		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			arm.zScale = state.length;
			hand.z = state.length * 16;
			boolean holding = state.reach >= 1;
			for (int i = 0; i < fingers.length; i++) {
				// Écartés en approchant, refermés une fois la proie saisie.
				fingers[i].xRot = holding ? 1.1f : -0.25f + Mth.sin(state.age * 0.5f + i) * 0.15f;
				fingers[i].yRot = holding ? 0 : (i - 1.5f) * 0.25f;
			}
		}

		static LayerDefinition layer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			root.addOrReplaceChild("arm", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5f, -1.5f, 0, 3, 3, 16),
					PartPose.ZERO);
			PartDefinition hand = root.addOrReplaceChild("hand",
					CubeListBuilder.create().texOffs(0, 20).addBox(-2.5f, -1, 0, 5, 2, 4), PartPose.ZERO);
			for (int i = 0; i < 4; i++) {
				hand.addOrReplaceChild("finger" + i, CubeListBuilder.create().texOffs(20, 20).addBox(-0.5f, -0.5f, 0, 1,
						1, 5), PartPose.offset(-1.8f + i * 1.2f, 0, 3.5f));
			}
			return LayerDefinition.create(mesh, 64, 64);
		}
	}

	private final Model model;

	public GateHandRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new Model(Model.layer().bakeRoot());
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GateHandEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		Entity target = entity.target();
		Vec3 from = entity.getPosition(partialTicks);
		state.toward = target == null ? new Vec3(0, RISE, 0)
				: target.getPosition(partialTicks).add(0, target.getBbHeight() * 0.6, 0).subtract(from);
		state.reach = entity.reach(partialTicks);
		state.age = entity.tickCount + partialTicks;
		state.length = (float) (state.toward.length() * state.reach);
	}

	@Override
	protected boolean affectedByCulling(GateHandEntity entity) {
		return false;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.length < 0.05f) {
			return;
		}
		float yaw = (float) Mth.atan2(state.toward.x, state.toward.z);
		float pitch = (float) Mth.atan2(state.toward.y, Math.hypot(state.toward.x, state.toward.z));
		// Une ondulation lente : le bras n'est pas raide.
		float sway = Mth.sin(state.age * 0.3f) * 0.08f;
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotation(yaw + sway));
		poseStack.mulPose(Axis.XP.rotation(-pitch + sway * 0.5f));
		collector.submitModel(model, state, poseStack, TEXTURE, 0xF000F0, OverlayTexture.NO_OVERLAY, state.outlineColor,
				null);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
