package com.ajustor.fmab.client.render;

import com.ajustor.fmab.entity.DischargeEntity;
import com.ajustor.fmab.transmutation.BoltShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Les éclairs d'une décharge de transmutation, dessinés comme l'éclair d'un orage : des traits
 * lumineux qui s'ajoutent à la lumière du monde, un halo coloré autour d'un cœur presque blanc. Leur
 * forme change toutes les deux ticks (voir {@link BoltShape}) : ils crépitent, puis s'éteignent.
 */
public class DischargeRenderer extends EntityRenderer<DischargeEntity, DischargeRenderer.State> {
	/** Le bleu de la réaction alchimique, et le blanc de son cœur. */
	private static final float[] BLUE = {0.25f, 0.6f, 1.0f};
	private static final float[] BLUE_CORE = {0.85f, 0.95f, 1.0f};
	/** Le rouge d'une réaction nourrie d'âmes, celle d'une Pierre philosophale. */
	private static final float[] RED = {0.95f, 0.1f, 0.15f};
	private static final float[] RED_CORE = {1.0f, 0.78f, 0.78f};
	/** Le halo, plus large que le cœur. */
	private static final float GLOW_WIDTH = 3.2f;
	/** Ticks entre deux formes : assez vite pour crépiter, assez lent pour qu'on voie l'éclair. */
	private static final int FLICKER = 2;

	public static class State extends EntityRenderState {
		BoltShape shape = new BoltShape(java.util.List.of());
		boolean red;
		/** De 1 (la décharge éclate) à 0 (elle s'éteint). */
		float fade;
	}

	public DischargeRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	/** La graine d'une forme : propre à chaque décharge, nouvelle toutes les deux ticks. */
	static long seed(int id, int age) {
		return id * 0x9E3779B97F4A7C15L + age / FLICKER;
	}

	@Override
	public void extractRenderState(DischargeEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.shape = BoltShape.of(seed(entity.getId(), entity.tickCount), entity.radius(), entity.bolts());
		state.red = entity.red();
		float age = (entity.tickCount + partialTicks) / Math.max(1, entity.lifetime());
		// L'éclair éclate d'un coup, puis pâlit sur sa fin.
		state.fade = Math.clamp(1.6f * (1 - age), 0f, 1f);
	}

	@Override
	protected boolean affectedByCulling(DischargeEntity entity) {
		// Ses éclairs débordent largement de l'entité : comme l'éclair d'un orage, on ne la cache pas.
		return false;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.fade <= 0 || state.shape.segments().isEmpty()) {
			return;
		}
		float[] glow = state.red ? RED : BLUE;
		float[] core = state.red ? RED_CORE : BLUE_CORE;
		float fade = state.fade;
		collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
			for (BoltShape.Segment s : state.shape.segments()) {
				float halo = s.branch() ? 0.3f : 0.4f;
				segment(buffer, pose, s.from(), s.to(), s.width() * GLOW_WIDTH, glow, halo * fade);
				segment(buffer, pose, s.from(), s.to(), s.width(), core, 0.95f * fade);
			}
		});
		super.submit(state, poseStack, collector, camera);
	}

	/** Un trait : deux rubans croisés, pour qu'il ait de l'épaisseur sous tous les angles. */
	private static void segment(VertexConsumer buffer, PoseStack.Pose pose, Vec3 from, Vec3 to, float width,
			float[] color, float alpha) {
		Vec3 dir = to.subtract(from);
		Vec3 side = dir.cross(new Vec3(0, 1, 0));
		if (side.lengthSqr() < 1.0E-6) {
			side = new Vec3(1, 0, 0);
		}
		side = side.normalize();
		Vec3 other = dir.cross(side).normalize();
		ribbon(buffer, pose, from, to, side.scale(width), color, alpha);
		ribbon(buffer, pose, from, to, other.scale(width), color, alpha);
	}

	/** Un ruban de {@code from} à {@code to}, large de deux fois {@code half}, visible des deux côtés. */
	private static void ribbon(VertexConsumer buffer, PoseStack.Pose pose, Vec3 from, Vec3 to, Vec3 half,
			float[] color, float alpha) {
		Vec3 a = from.subtract(half), b = from.add(half), c = to.add(half), d = to.subtract(half);
		quad(buffer, pose, a, b, c, d, color, alpha);
		quad(buffer, pose, d, c, b, a, color, alpha);
	}

	private static void quad(VertexConsumer buffer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
			float[] color, float alpha) {
		for (Vec3 v : new Vec3[]{a, b, c, d}) {
			buffer.addVertex(pose, (float) v.x, (float) v.y, (float) v.z).setColor(color[0], color[1], color[2], alpha);
		}
	}
}
