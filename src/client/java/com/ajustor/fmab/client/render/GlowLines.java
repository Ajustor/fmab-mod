package com.ajustor.fmab.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

/**
 * Des traits de lumière, à dessiner dans {@code RenderTypes.lightning()} : ils s'ajoutent à la
 * lumière du monde, comme un éclair. Un trait au sol, un cercle au sol, une colonne qui monte : le
 * cercle géant qui s'allume, et le plan de ses relais quand on le trace.
 */
final class GlowLines {
	/** Largeur d'un trait, en blocs. */
	static final float LINE = 0.35f;
	private static final float[] WHITE = {1, 1, 1};

	private GlowLines() {
	}

	/** Un trait au sol, de {@code from} vers {@code to}, tracé jusqu'à la fraction {@code reach}. */
	static void line(VertexConsumer buffer, PoseStack.Pose pose, Vec3 from, Vec3 to, float reach, float[] color,
			float alpha) {
		line(buffer, pose, from, to, reach, LINE, color, alpha);
	}

	/** Un trait au sol de cette largeur. */
	static void line(VertexConsumer buffer, PoseStack.Pose pose, Vec3 from, Vec3 to, float reach, float width,
			float[] color, float alpha) {
		Vec3 end = from.add(to.subtract(from).scale(reach));
		Vec3 dir = end.subtract(from);
		if (dir.lengthSqr() < 1.0E-4) {
			return;
		}
		Vec3 side = new Vec3(-dir.z, 0, dir.x).normalize();
		flat(buffer, pose, from, end, side.scale(width * 2.5), color, alpha * 0.35f);
		flat(buffer, pose, from, end, side.scale(width), WHITE, alpha * 0.25f);
		flat(buffer, pose, from, end, side.scale(width), color, alpha);
	}

	/** Un cercle au sol, dont seule la fraction {@code reach} est tracée. */
	static void ring(VertexConsumer buffer, PoseStack.Pose pose, Vec3 center, float radius, int segments, float reach,
			float[] color, float alpha) {
		ring(buffer, pose, center, radius, segments, reach, LINE, false, color, alpha);
	}

	/**
	 * Un cercle au sol de cette largeur ; en pointillés, un segment sur deux.
	 */
	static void ring(VertexConsumer buffer, PoseStack.Pose pose, Vec3 center, float radius, int segments, float reach,
			float width, boolean dashed, float[] color, float alpha) {
		int drawn = Math.round(segments * reach);
		for (int i = 0; i < drawn; i++) {
			if (dashed && i % 2 == 1) {
				continue;
			}
			double a = 2 * Math.PI * i / segments, b = 2 * Math.PI * (i + 1) / segments;
			Vec3 p = center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
			Vec3 q = center.add(Math.cos(b) * radius, 0, Math.sin(b) * radius);
			line(buffer, pose, p, q, 1, width, color, alpha);
		}
	}

	/** Une colonne de lumière qui monte d'un point. */
	static void pillar(VertexConsumer buffer, PoseStack.Pose pose, Vec3 base, float height, float[] color,
			float alpha) {
		pillar(buffer, pose, base, height, 0.6f, color, alpha);
	}

	/** Une colonne de lumière de cette demi-largeur. */
	static void pillar(VertexConsumer buffer, PoseStack.Pose pose, Vec3 base, float height, float half, float[] color,
			float alpha) {
		Vec3 top = base.add(0, height, 0);
		for (Vec3 side : new Vec3[]{new Vec3(half, 0, 0), new Vec3(0, 0, half)}) {
			flat(buffer, pose, base, top, side.scale(2), color, alpha * 0.3f);
			flat(buffer, pose, base, top, side, color, alpha);
		}
	}

	/** Un ruban de {@code from} à {@code to}, large de deux fois {@code half}, visible des deux côtés. */
	static void flat(VertexConsumer buffer, PoseStack.Pose pose, Vec3 from, Vec3 to, Vec3 half, float[] color,
			float alpha) {
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
