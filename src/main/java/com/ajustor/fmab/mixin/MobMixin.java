package com.ajustor.fmab.mixin;

import com.ajustor.fmab.stone.Paralysis;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Une créature qu'une Pierre vivante tient ne pense plus : ni cible, ni chemin, ni attaque. */
@Mixin(Mob.class)
public abstract class MobMixin {
	@Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
	private void fmab$paralysed(CallbackInfo ci) {
		if (Paralysis.held((Mob) (Object) this)) {
			ci.cancel();
		}
	}
}
