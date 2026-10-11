package com.ajustor.fmab.mixin;

import com.ajustor.fmab.entity.Voices;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Le pas propre de certaines créatures du mod : chaînes d'une armure vide, roc d'un golem (voir {@link Voices}). */
@Mixin(Entity.class)
public abstract class StepMixin {
	@Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
	private void fmab$step(BlockPos pos, BlockState state, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		Voices voice = Voices.of(self);
		if (voice != null && voice.step() != null) {
			self.playSound(voice.step(), 0.6f, voice.pitch());
			ci.cancel();
		}
	}
}
