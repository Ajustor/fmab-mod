package com.ajustor.fmab.mixin;

import com.ajustor.fmab.gate.Shells;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Une âme et celui qui est dans son armure occupent la même place : ils ne se repoussent pas. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private void fmab$shellNotPushed(CallbackInfoReturnable<Boolean> cir) {
		if (Shells.paired((LivingEntity) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
