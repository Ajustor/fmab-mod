package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.client.render.FirstPersonGestures;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * À la première personne, pendant un geste de transmutation, les deux mains prennent la place de
 * l'objet tenu (voir {@link FirstPersonGestures}).
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@Invoker("renderPlayerArm")
	abstract void fmab$renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int light,
			float inverseArmHeight, float attackValue, HumanoidArm arm);

	@Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void fmab$gestures(float partialTicks, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player,
			int light, CallbackInfo ci) {
		if (FirstPersonGestures.submit(player, partialTicks, poseStack,
				side -> fmab$renderPlayerArm(poseStack, collector, light, 0, 0, side))) {
			ci.cancel();
		}
	}
}
