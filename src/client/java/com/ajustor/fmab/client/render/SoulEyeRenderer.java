package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.client.compat.RenderCompat;
import com.ajustor.fmab.entity.SoulEyeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * L'œil dans le sol ({@link SoulEyeEntity}) : une amande à plat sous les pieds de sa proie, qui s'ouvre
 * comme une paupière, lumineuse dans le noir.
 */
public class SoulEyeRenderer extends EntityRenderer<SoulEyeEntity, SoulEyeRenderer.State> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/soul_eye.png");
	/** L'amande du modèle : 64 pixels de long, soit quatre blocs avant mise à l'échelle. */
	private static final float MODEL_BLOCKS = 4;

	public static class State extends EntityRenderState {
		/** Où est la proie, par rapport à l'œil. */
		Vec3 offset = Vec3.ZERO;
		float open;
		float size;
		float yaw;
	}

	/**
	 * Une plaque sans épaisseur, d'une seule face (le rendu ne cache pas l'envers : elle se voit des deux
	 * côtés). L'œil est peint aux deux places de la texture où une face du dessus ou du dessous le cherche.
	 */
	static class Plate extends EntityModel<State> {
		Plate(ModelPart root) {
			super(root);
		}

		static LayerDefinition layer() {
			MeshDefinition mesh = new MeshDefinition();
			mesh.getRoot().addOrReplaceChild("eye", CubeListBuilder.create().texOffs(0, 0).addBox(-32, 0, -16, 64, 0, 32,
					Set.of(Direction.DOWN)),
					PartPose.ZERO);
			return LayerDefinition.create(mesh, 160, 32);
		}
	}

	private final Plate plate;

	public SoulEyeRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.plate = new Plate(Plate.layer().bakeRoot());
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SoulEyeEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.open = SoulEyeEntity.openness(entity.tickCount + partialTicks, entity.shut());
		Entity target = entity.target();
		// Assez grand pour qu'on voie la proie debout dessus, comme dans un œil grand ouvert.
		state.size = target == null ? 1.5f : Math.max(1.5f, target.getBbWidth() * 2.6f);
		state.offset = target == null || !target.isAlive() ? Vec3.ZERO
				: target.getPosition(partialTicks).subtract(entity.getPosition(partialTicks));
		// Chacun regarde dans sa direction.
		state.yaw = entity.getId() * 2.39996f;
	}

	@Override
	protected boolean affectedByCulling(SoulEyeEntity entity) {
		return false;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.open <= 0.02f) {
			return;
		}
		float scale = state.size / MODEL_BLOCKS;
		poseStack.pushPose();
		// Juste au-dessus du sol : pas de scintillement avec le bloc.
		poseStack.translate(state.offset.x, state.offset.y + 0.02, state.offset.z);
		RenderCompat.rotate(poseStack, Axis.YP.rotation(state.yaw));
		poseStack.scale(scale, 1, scale * Mth.clamp(state.open, 0.05f, 1));
		RenderCompat.submitModel(collector, plate, state, poseStack, TEXTURE, 0xF000F0, OverlayTexture.NO_OVERLAY,
				state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
