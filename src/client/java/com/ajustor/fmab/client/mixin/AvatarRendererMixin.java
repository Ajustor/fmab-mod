package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.client.render.PoseHolder;
import com.ajustor.fmab.data.TransmutationPose;
import com.ajustor.fmab.registry.FmabAttachments;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recopie dans l'état de rendu le geste de transmutation que le serveur a donné au joueur. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;"
			+ "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void fmab$extractPose(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
		TransmutationPose pose = entity.getAttached(FmabAttachments.POSE);
		long now = entity.level().getGameTime();
		PoseHolder holder = (PoseHolder) state;
		if (pose == null || !pose.active(now)) {
			holder.fmab$setPose(TransmutationPose.Kind.NONE, 0);
		} else {
			holder.fmab$setPose(pose.kind(), pose.until() - now - partialTicks);
		}
	}
}
