package com.ajustor.fmab.mixin;

import com.ajustor.fmab.stone.Paralysis;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Une créature qu'une Pierre vivante tient ne pense plus : ni cible, ni chemin, ni attaque. Son élan
 * s'éteint aussi : sans cela, elle garderait la vitesse et la direction que son contrôle de mouvement
 * lui avait données au tick d'avant, et continuerait de marcher.
 */
@Mixin(Mob.class)
public abstract class MobMixin {
	@Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
	private void fmab$paralysed(CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		if (Paralysis.held(self)) {
			self.setSpeed(0);
			self.setXxa(0);
			self.setYya(0);
			self.setZza(0);
			self.setJumping(false);
			ci.cancel();
		}
	}
}
