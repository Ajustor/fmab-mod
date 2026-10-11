package com.ajustor.fmab.mixin;

import com.ajustor.fmab.entity.Voices;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ce que disent les créatures du mod quand elles ne font rien (voir {@link Voices}). */
@Mixin(Mob.class)
public abstract class AmbientVoiceMixin {
	@Inject(method = "getAmbientSound", at = @At("HEAD"), cancellable = true)
	private void fmab$ambient(CallbackInfoReturnable<SoundEvent> cir) {
		Voices voice = Voices.of((Mob) (Object) this);
		if (voice != null) {
			cir.setReturnValue(voice.ambient());
		}
	}
}
