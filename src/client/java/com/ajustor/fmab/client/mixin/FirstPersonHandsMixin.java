package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.client.render.FirstPersonGestures;
import com.ajustor.fmab.gate.Shells;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.3 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
*///?} else {
import net.minecraft.client.renderer.ItemInHandRenderer;
//?}

/**
 * Les mains à la première personne : pendant un geste de transmutation, les deux mains prennent la
 * place de l'objet tenu (voir {@link FirstPersonGestures}) ; dans l'armure d'une âme, ses mains à soi
 * restent dedans (on ne voit que par la fente du heaume). Le rendu des mains a changé de classe et de
 * forme en 26.3 : il se dessine depuis un état extrait d'avance, plus depuis le joueur.
 */
//? if >=26.3 {
/*@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsMixin {
	@Invoker("renderPlayerArm")
	abstract void fmab$renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int light,
			float inverseArmHeight, float attackValue, HumanoidArm arm, PlayerRenderState state);

	@Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void fmab$hands(float partialTicks, PoseStack poseStack, SubmitNodeCollector collector, PlayerRenderState state,
			FirstPersonHandsAndItemsRenderState hands, CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		int light = state.avatarRenderState.lightCoords;
		if (Shells.inside(player) || FirstPersonGestures.submit(player, partialTicks, poseStack,
				side -> fmab$renderPlayerArm(poseStack, collector, light, 0, 0, side, state))) {
			ci.cancel();
		}
	}
}
*///?} else {
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonHandsMixin {
	@Invoker("renderPlayerArm")
	abstract void fmab$renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int light,
			float inverseArmHeight, float attackValue, HumanoidArm arm);

	@Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void fmab$hands(float partialTicks, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player,
			int light, CallbackInfo ci) {
		if (Shells.inside(player) || FirstPersonGestures.submit(player, partialTicks, poseStack,
				side -> fmab$renderPlayerArm(poseStack, collector, light, 0, 0, side))) {
			ci.cancel();
		}
	}
}
//?}
