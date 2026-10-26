package com.ajustor.fmab.mixin;

import com.ajustor.fmab.gate.Shells;
import com.ajustor.fmab.gate.Wheelchairs;
import com.ajustor.fmab.stone.Paralysis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sans jambes et sans fauteuil, on rampe : la pose voulue est celle de la nage, à même le sol. Dans
 * l'armure d'une âme, on ne se vise plus : les coups et les clics vont à l'armure. Paralysé par une
 * Pierre vivante, on ne frappe plus.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "getDesiredPose", at = @At("HEAD"), cancellable = true)
	private void fmab$crawl(CallbackInfoReturnable<Pose> cir) {
		if (Wheelchairs.mustCrawl((Player) (Object) this)) {
			cir.setReturnValue(Pose.SWIMMING);
		}
	}

	/** Tenu par une Pierre vivante, on ne frappe plus. */
	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void fmab$paralysed(Entity target, CallbackInfo ci) {
		if (Paralysis.held((Player) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
	private void fmab$hiddenInShell(CallbackInfoReturnable<Boolean> cir) {
		if (Shells.inside((Player) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
