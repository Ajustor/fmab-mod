package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.gate.Shells;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Celui qui est dans l'armure d'une âme ne se voit plus : on ne voit que l'armure. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private <E extends Entity> void fmab$hiddenInShell(E entity, Frustum frustum, double camX, double camY, double camZ,
			//? if >=26.3 {
			/*float partialTicks,
			*///?}
			CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof Player player && Shells.inside(player)) {
			cir.setReturnValue(false);
		}
	}
}
