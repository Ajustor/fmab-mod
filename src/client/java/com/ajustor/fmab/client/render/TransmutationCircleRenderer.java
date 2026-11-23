package com.ajustor.fmab.client.render;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.client.GreatCirclePlanner;
import com.ajustor.fmab.transmutation.GreatCircle;
import net.minecraft.core.BlockPos;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;
import com.ajustor.fmab.client.compat.RenderCompat;

/**
 * Dessine le cercle comme une décalcomanie posée sur sa surface, de la taille choisie en le traçant
 * (de un à sept blocs de côté), dans la couleur de ce qui l'a tracé. Au sol et au plafond, le haut de la page regarde la direction où se
 * tenait l'alchimiste ; sur un mur, il regarde le ciel.
 *
 * <p>Quand le joueur trace un cercle géant autour de ce cercle (voir {@link GreatCirclePlanner}), il
 * en dessine aussi le plan : le tour prévu en pointillés, les relais déjà tracés, et les emplacements
 * qui restent, chacun sous une colonne de lumière qu'on voit de loin.
 */
public class TransmutationCircleRenderer
		implements BlockEntityRenderer<TransmutationCircleBlockEntity, TransmutationCircleRenderer.State> {
	private static final float LIFT = 0.01f;
	/**
	 * Demi-tour autour de l'axe (0, 1, −1) : la décalcomanie, définie à plat (normale +Y, haut de
	 * la page vers −Z), devient verticale, normale vers −Z, haut de la page vers +Y, sans être vue
	 * en miroir.
	 */
	private static final Quaternionf TO_WALL =
			new Quaternionf().rotationAxis((float) Math.PI, 0, (float) Math.sqrt(0.5), (float) -Math.sqrt(0.5));

	public static class State extends BlockEntityRenderState {
		Drawing drawing = Drawing.EMPTY;
		AttachFace face = AttachFace.FLOOR;
		Direction facing = Direction.NORTH;
		CircleMedium medium = CircleMedium.CHALK;
		float half = 1.5f;
		GreatCircle.@Nullable Plan plan;
		float age;
	}

	/** Le plan d'un cercle géant : le tour, en pâle ; les relais tracés, en bleu ; ceux à tracer, en or. */
	private static final float[] PLAN_RING = {0.55f, 0.75f, 1.0f};
	private static final float[] PLAN_DONE = {0.3f, 0.65f, 1.0f};
	private static final float[] PLAN_TODO = {1.0f, 0.78f, 0.25f};
	/** Assez haut pour qu'on la voie par-dessus les toits. */
	private static final float BEACON = 40;

	public TransmutationCircleRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TransmutationCircleBlockEntity blockEntity, State state, float partialTicks,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		BlockState block = blockEntity.getBlockState();
		state.drawing = blockEntity.drawing();
		state.face = block.getValue(TransmutationCircleBlock.FACE);
		state.facing = block.getValue(TransmutationCircleBlock.FACING);
		state.medium = block.getValue(TransmutationCircleBlock.MEDIUM);
		state.half = blockEntity.size().blocks() / 2f;
		state.plan = GreatCirclePlanner.centeredOn(blockEntity.getBlockPos());
		state.age = blockEntity.getLevel() == null ? 0 : blockEntity.getLevel().getGameTime() + partialTicks;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.drawing.isEmpty()) {
			return;
		}
		if (state.plan != null) {
			plan(state.plan, state.age, poseStack, collector);
		}
		Identifier texture = CircleTextures.get(state.drawing, state.medium.color());
		poseStack.pushPose();
		poseStack.translate(0.5f, 0.5f, 0.5f);
		// Après cette rotation, −Z local pointe vers state.facing.
		RenderCompat.rotate(poseStack, Axis.YP.rotationDegrees(180 - state.facing.toYRot()));
		switch (state.face) {
			case FLOOR -> poseStack.translate(0, -0.5f + LIFT, 0);
			case CEILING -> {
				poseStack.translate(0, 0.5f - LIFT, 0);
				RenderCompat.rotate(poseStack, Axis.ZP.rotationDegrees(180));
			}
			case WALL -> {
				// Le support est derrière, du côté +Z local.
				poseStack.translate(0, 0, 0.5f - LIFT);
				RenderCompat.rotate(poseStack, TO_WALL);
			}
		}
		int light = state.lightCoords;
		float half = state.half;
		collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture), (pose, buffer) -> {
			// Vu de face, −Z est le haut de la page et +X sa droite.
			vertex(buffer, pose, -half, -half, 0, 0, light);
			vertex(buffer, pose, -half, half, 0, 1, light);
			vertex(buffer, pose, half, half, 1, 1, light);
			vertex(buffer, pose, half, -half, 1, 0, light);
		});
		poseStack.popPose();
	}

	/** Le plan du cercle géant, à plat au niveau du cercle central. */
	private static void plan(GreatCircle.Plan plan, float age, PoseStack poseStack, SubmitNodeCollector collector) {
		BlockPos center = plan.center();
		Vec3 origin = new Vec3(0.5, LIFT * 4, 0.5);
		float pulse = 0.55f + 0.45f * (float) Math.sin(age * 0.25);
		collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
			GlowLines.ring(buffer, pose, origin, (float) plan.radius(), 96, 1, 0.12f, true, PLAN_RING, 0.5f);
			for (BlockPos relay : plan.relays()) {
				Vec3 at = origin.add(relay.getX() - center.getX(), relay.getY() - center.getY(), relay.getZ() - center.getZ());
				GlowLines.ring(buffer, pose, at, 2, 20, 1, PLAN_DONE, 0.8f);
			}
			for (GreatCircle.Spot spot : plan.spots()) {
				if (spot.filled()) {
					continue;
				}
				Vec3 at = origin.add(spot.pos().getX() - center.getX(), 0, spot.pos().getZ() - center.getZ());
				GlowLines.ring(buffer, pose, at, 1.6f, 20, 1, 0.2f, false, PLAN_TODO, pulse);
				GlowLines.pillar(buffer, pose, at, BEACON, 0.25f, PLAN_TODO, 0.35f * pulse);
			}
		});
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

	/** Le plan d'un cercle géant se voit de loin : ses relais peuvent être à l'autre bout de la ville. */
	@Override
	public int getViewDistance() {
		return GreatCircle.MAX_RADIUS + 32;
	}
}
