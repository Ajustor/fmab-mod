package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.client.Cinematics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Les cinématiques se dessinent à la fin de l'interface du jeu : par-dessus la barre d'action et le
 * chat, même interface masquée (F1), mais sous les menus qu'on ouvre (pause, inventaire).
 */
@Mixin(Hud.class)
public abstract class HudMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
			at = @At("TAIL"))
	private void fmab$cinematic(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
		if (Cinematics.active()) {
			graphics.nextStratum();
			Cinematics.render(graphics, delta);
		}
	}
}
