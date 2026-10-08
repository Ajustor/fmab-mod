package com.ajustor.fmab.client.render;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.block.ChalkCircleBlock;
import com.ajustor.fmab.block.ChalkCircleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Dessine le cercle de craie comme une décalcomanie posée sur le sol, trois blocs de côté, le haut
 * de la page tourné vers la direction où regardait celui qui l'a tracé.
 */
public class ChalkCircleRenderer implements BlockEntityRenderer<ChalkCircleBlockEntity, ChalkCircleRenderer.State> {
	/** Blanc cassé de la craie. */
	private static final int CHALK = 0xF0EEEAE0;
	private static final float HALF = 1.5f;
	private static final float LIFT = 0.01f;

	public static class State extends BlockEntityRenderState {
		Drawing drawing = Drawing.EMPTY;
		Direction facing = Direction.NORTH;
	}

	public ChalkCircleRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ChalkCircleBlockEntity blockEntity, State state, float partialTicks,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.drawing = blockEntity.drawing();
		state.facing = blockEntity.getBlockState().getValue(ChalkCircleBlock.FACING);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.drawing.isEmpty()) {
			return;
		}
		Identifier texture = CircleTextures.get(state.drawing, CHALK);
		poseStack.pushPose();
		poseStack.translate(0.5f, LIFT, 0.5f);
		poseStack.mulPose(Axis.YP.rotationDegrees(180 - state.facing.toYRot()));
		int light = state.lightCoords;
		collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture), (pose, buffer) -> {
			// Vu de dessus, le nord de la pose est le haut de la page.
			vertex(buffer, pose, -HALF, -HALF, 0, 0, light);
			vertex(buffer, pose, -HALF, HALF, 0, 1, light);
			vertex(buffer, pose, HALF, HALF, 1, 1, light);
			vertex(buffer, pose, HALF, -HALF, 1, 0, light);
		});
		poseStack.popPose();
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float z, float u, float v, int light) {
		buffer.addVertex(pose, x, 0, z)
				.setColor(-1)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, 0, 1, 0);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
