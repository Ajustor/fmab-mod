package com.ajustor.fmab.mixin;

import com.ajustor.fmab.entity.Voices;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Les monstres ont leur propre cri « hostile » : celui des créatures du mod passe devant (voir {@link Voices}). */
@Mixin(Monster.class)
public abstract class MonsterVoiceMixin {
	@Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
	private void fmab$hurt(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
		Voices voice = Voices.of((Monster) (Object) this);
		if (voice != null) {
			cir.setReturnValue(voice.hurt());
		}
	}

	@Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
	private void fmab$death(CallbackInfoReturnable<SoundEvent> cir) {
		Voices voice = Voices.of((Monster) (Object) this);
		if (voice != null) {
			cir.setReturnValue(voice.death());
		}
	}
}
