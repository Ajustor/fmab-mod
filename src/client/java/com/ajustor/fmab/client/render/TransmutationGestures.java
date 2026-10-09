package com.ajustor.fmab.client.render;

import com.ajustor.fmab.data.TransmutationPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/**
 * Les gestes de l'alchimiste, par-dessus l'animation ordinaire : penché, les paumes plaquées sur le
 * cercle ; les mains tenues qui tremblent sous l'énergie ; les mains jointes ; le bras tendu vers une
 * âme qu'on arrache. Le geste retombe en douceur à sa fin. Le joueur et ses automails les jouent
 * ensemble.
 */
public final class TransmutationGestures {
	/** Les dernières ticks d'un geste, il se relâche vers la pose ordinaire. */
	private static final float EASE = 4;

	private TransmutationGestures() {
	}

	public static void apply(HumanoidModel<?> model, AvatarRenderState state) {
		PoseHolder holder = (PoseHolder) state;
		TransmutationPose.Kind pose = holder.fmab$pose();
		if (pose == TransmutationPose.Kind.NONE) {
			return;
		}
		float weight = Mth.clamp(holder.fmab$remaining() / EASE, 0, 1);
		float tremble = Mth.sin(state.ageInTicks * 2.1f) * 0.04f;
		if (pose == TransmutationPose.Kind.PALM) {
			lean(model, 0.45f, weight);
			arms(model, -0.85f, 0.18f, 0, weight);
		} else if (pose == TransmutationPose.Kind.HOLD) {
			lean(model, 0.45f, weight);
			arms(model, -0.9f + tremble, 0.18f, tremble, weight);
		} else if (pose == TransmutationPose.Kind.CLAP) {
			arms(model, -1.45f, 0.45f, 0, weight);
		} else if (pose == TransmutationPose.Kind.REACH) {
			model.rightArm.xRot = Mth.lerp(weight, model.rightArm.xRot, -1.5f + tremble);
			model.rightArm.yRot = Mth.lerp(weight, model.rightArm.yRot, -0.1f);
		}
	}

	/**
	 * Penché en avant, la tête qui regarde le cercle. Le torse pivote au cou : les hanches reculent et
	 * remontent, les jambes les suivent et le haut du corps descend pour garder les pieds au sol.
	 */
	private static void lean(HumanoidModel<?> model, float angle, float weight) {
		float before = model.body.xRot;
		float after = Mth.lerp(weight, before, angle);
		float back = 12 * (Mth.sin(after) - Mth.sin(before));
		float drop = 12 * (Mth.cos(before) - Mth.cos(after));
		model.body.xRot = after;
		model.rightLeg.z += back;
		model.leftLeg.z += back;
		model.body.y += drop;
		model.head.y += drop;
		model.rightArm.y += drop;
		model.leftArm.y += drop;
		model.head.xRot = Mth.lerp(weight, model.head.xRot, model.head.xRot + angle * 0.6f);
	}

	/**
	 * Les deux bras vers l'avant, symétriques.
	 *
	 * @param inward rotation qui rapproche les mains (positif : vers le centre)
	 */
	private static void arms(HumanoidModel<?> model, float forward, float inward, float roll, float weight) {
		model.rightArm.xRot = Mth.lerp(weight, model.rightArm.xRot, forward);
		model.leftArm.xRot = Mth.lerp(weight, model.leftArm.xRot, forward);
		model.rightArm.yRot = Mth.lerp(weight, model.rightArm.yRot, -inward);
		model.leftArm.yRot = Mth.lerp(weight, model.leftArm.yRot, inward);
		model.rightArm.zRot = Mth.lerp(weight, model.rightArm.zRot, roll);
		model.leftArm.zRot = Mth.lerp(weight, model.leftArm.zRot, -roll);
	}
}
