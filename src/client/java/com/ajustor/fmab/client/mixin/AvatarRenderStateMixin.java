package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.client.render.PoseHolder;
import com.ajustor.fmab.data.TransmutationPose;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** L'état de rendu du joueur retient son geste de transmutation. */
@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements PoseHolder {
	@Unique
	private TransmutationPose.Kind fmab$pose = TransmutationPose.Kind.NONE;
	@Unique
	private float fmab$remaining;

	@Override
	public TransmutationPose.Kind fmab$pose() {
		return fmab$pose;
	}

	@Override
	public float fmab$remaining() {
		return fmab$remaining;
	}

	@Override
	public void fmab$setPose(TransmutationPose.Kind pose, float remaining) {
		fmab$pose = pose;
		fmab$remaining = remaining;
	}
}
