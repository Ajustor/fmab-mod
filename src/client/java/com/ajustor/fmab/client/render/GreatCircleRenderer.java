package com.ajustor.fmab.client.render;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.entity.GreatCircleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Un cercle géant qui s'allume : le tracé du cercle central, projeté à l'échelle de la ville, grandit
 * depuis le centre jusqu'aux relais ; un anneau passe par eux, et une colonne de lumière monte de
 * chacun. Il tient, puis s'éteint.
 */
public class GreatCircleRenderer extends EntityRenderer<GreatCircleEntity, GreatCircleRenderer.State> {
	private static final float[] BLUE = {0.3f, 0.65f, 1.0f};
	private static final float[] RED = {1.0f, 0.12f, 0.15f};
	private static final int FULL_BRIGHT = 0xF000F0;
	/** Juste au-dessus du sol, pour ne pas s'y enfoncer. */
	static final float LIFT = 0.06f;
	/** Les ticks où la lumière court du centre aux relais. */
	private static final float SPREAD = 24;

	public static class State extends EntityRenderState {
		List<Vec3> relays = List.of();
		float radius;
		boolean red;
		/** De 0 à 1 : jusqu'où la lumière a couru. */
		float spread;
		float fade;
		float age;
		Drawing drawing = Drawing.EMPTY;
	}

	public GreatCircleRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GreatCircleEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		List<Vec3> relays = new ArrayList<>();
		for (BlockPos p : entity.relays()) {
			relays.add(new Vec3(p.getX(), p.getY() + LIFT, p.getZ()));
		}
		state.relays = relays;
		state.radius = entity.radius();
		state.red = entity.red();
		state.age = entity.tickCount + partialTicks;
		state.spread = Math.clamp(state.age / SPREAD, 0f, 1f);
		state.fade = Math.clamp((GreatCircleEntity.LIFETIME - state.age) / 20f, 0f, 1f);
		state.drawing = entity.level().getBlockEntity(entity.blockPosition()) instanceof TransmutationCircleBlockEntity c
				? c.drawing() : Drawing.EMPTY;
	}

	@Override
	protected boolean affectedByCulling(GreatCircleEntity entity) {
		return false;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.fade <= 0 || state.relays.isEmpty()) {
			return;
		}
		float[] color = state.red ? RED : BLUE;
		// Un battement : la lumière respire tant que le cercle tient.
		float alpha = state.fade * (0.75f + 0.25f * (float) Math.sin(state.age * 0.4));
		if (!state.drawing.isEmpty()) {
			decal(state, poseStack, collector, color, alpha);
		}
		collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
			GlowLines.ring(buffer, pose, new Vec3(0, LIFT, 0), state.radius, 128, state.spread, color, alpha);
			for (Vec3 relay : state.relays) {
				GlowLines.pillar(buffer, pose, relay, 6 + 18 * state.spread, color, alpha);
			}
		});
		super.submit(state, poseStack, collector, camera);
	}

	/** Le tracé du cercle central, agrandi jusqu'aux relais, luisant à plat sur le sol. */
	private static void decal(State state, PoseStack poseStack, SubmitNodeCollector collector, float[] color, float alpha) {
		int argb = 0xFF000000 | (int) (color[0] * 255) << 16 | (int) (color[1] * 255) << 8 | (int) (color[2] * 255);
		Identifier texture = CircleTextures.get(state.drawing, argb);
		float half = state.radius * 1.08f * state.spread;
		int a = (int) (alpha * 255) << 24 | 0xFFFFFF;
		collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture), (pose, buffer) -> {
			decalVertex(buffer, pose, -half, -half, 0, 0, a);
			decalVertex(buffer, pose, -half, half, 0, 1, a);
			decalVertex(buffer, pose, half, half, 1, 1, a);
			decalVertex(buffer, pose, half, -half, 1, 0, a);
		});
	}

	private static void decalVertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float z, float u, float v,
			int color) {
		buffer.addVertex(pose, x, LIFT, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(FULL_BRIGHT).setNormal(pose, 0, 1, 0);
	}
}
