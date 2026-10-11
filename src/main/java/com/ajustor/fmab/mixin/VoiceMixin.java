package com.ajustor.fmab.mixin;

import com.ajustor.fmab.entity.Voices;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Les cris des créatures du mod, touchées ou mourantes, et la hauteur de leur voix (voir {@link Voices}). */
@Mixin(LivingEntity.class)
public abstract class VoiceMixin {
	@Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
	private void fmab$hurt(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
		Voices voice = Voices.of((LivingEntity) (Object) this);
		if (voice != null) {
			cir.setReturnValue(voice.hurt());
		}
	}

	@Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
	private void fmab$death(CallbackInfoReturnable<SoundEvent> cir) {
		Voices voice = Voices.of((LivingEntity) (Object) this);
		if (voice != null) {
			cir.setReturnValue(voice.death());
		}
	}

	@Inject(method = "getVoicePitch", at = @At("RETURN"), cancellable = true)
	private void fmab$pitch(CallbackInfoReturnable<Float> cir) {
		Voices voice = Voices.of((LivingEntity) (Object) this);
		if (voice != null && voice.pitch() != 1) {
			cir.setReturnValue(cir.getReturnValueF() * voice.pitch());
		}
	}
}
