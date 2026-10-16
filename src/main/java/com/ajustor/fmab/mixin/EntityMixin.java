package com.ajustor.fmab.mixin;

import com.ajustor.fmab.stone.ProjectileTransmutation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Un projectile qui touche une Pierre vivante sans avoir été vu venir (tiré à bout portant) se défait
 * au contact. Le jeu le croit dévié : il n'applique ni ses dégâts ni son explosion.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "deflection", at = @At("HEAD"), cancellable = true)
	private void fmab$transmuteOnLivingStone(Projectile projectile, CallbackInfoReturnable<ProjectileDeflection> cir) {
		if (ProjectileTransmutation.intercept((Entity) (Object) this, projectile)) {
			cir.setReturnValue(ProjectileTransmutation.TRANSMUTED);
		}
	}
}
