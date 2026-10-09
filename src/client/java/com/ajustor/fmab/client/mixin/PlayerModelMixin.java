package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.client.render.PoseHolder;
import com.ajustor.fmab.data.TransmutationPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Les gestes de l'alchimiste sur le modèle du joueur, par-dessus son animation ordinaire : penché, les
 * paumes plaquées sur le cercle ; les mains tenues qui tremblent sous l'énergie ; les mains jointes ;
 * le bras tendu vers une âme qu'on arrache. Le geste retombe en douceur à sa fin.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin extends HumanoidModel<AvatarRenderState> {
	/** Les dernières ticks d'un geste, il se relâche vers la pose ordinaire. */
	@Unique
	private static final float EASE = 4;

	protected PlayerModelMixin(ModelPart root) {
		super(root);
	}

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void fmab$pose(AvatarRenderState state, CallbackInfo ci) {
		PoseHolder holder = (PoseHolder) state;
		TransmutationPose.Kind pose = holder.fmab$pose();
		if (pose == TransmutationPose.Kind.NONE) {
			return;
		}
		float weight = Mth.clamp(holder.fmab$remaining() / EASE, 0, 1);
		float tremble = Mth.sin(state.ageInTicks * 2.1f) * 0.04f;
		if (pose == TransmutationPose.Kind.PALM) {
			lean(0.45f, weight);
			arms(-0.85f, 0.18f, 0, weight);
		} else if (pose == TransmutationPose.Kind.HOLD) {
			lean(0.45f, weight);
			arms(-0.9f + tremble, 0.18f, tremble, weight);
		} else if (pose == TransmutationPose.Kind.CLAP) {
			arms(-1.45f, -0.42f, 0, weight);
		} else if (pose == TransmutationPose.Kind.REACH) {
			rightArm.xRot = Mth.lerp(weight, rightArm.xRot, -1.5f + tremble);
			rightArm.yRot = Mth.lerp(weight, rightArm.yRot, -0.1f);
		}
	}

	/** Penché en avant, la tête qui regarde le cercle. */
	@Unique
	private void lean(float angle, float weight) {
		body.xRot = Mth.lerp(weight, body.xRot, angle);
		head.xRot = Mth.lerp(weight, head.xRot, head.xRot + angle * 0.6f);
	}

	/**
	 * Les deux bras vers l'avant, symétriques.
	 *
	 * @param inward rotation qui rapproche les mains (négatif : vers le centre)
	 */
	@Unique
	private void arms(float forward, float inward, float roll, float weight) {
		rightArm.xRot = Mth.lerp(weight, rightArm.xRot, forward);
		leftArm.xRot = Mth.lerp(weight, leftArm.xRot, forward);
		rightArm.yRot = Mth.lerp(weight, rightArm.yRot, -inward);
		leftArm.yRot = Mth.lerp(weight, leftArm.yRot, inward);
		rightArm.zRot = Mth.lerp(weight, rightArm.zRot, roll);
		leftArm.zRot = Mth.lerp(weight, leftArm.zRot, -roll);
	}
}
