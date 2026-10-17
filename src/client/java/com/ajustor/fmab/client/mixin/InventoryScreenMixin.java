package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.client.screen.InventoryTabs;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Les onglets du mod sur l'inventaire du joueur : posés à l'ouverture, replacés à chaque image (le
 * livre de recettes décale l'inventaire).
 */
@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin {
	@Unique
	private InventoryTabs.Tab[] fmab$tabs;

	@Inject(method = "init", at = @At("TAIL"))
	private void fmab$addTabs(CallbackInfo ci) {
		fmab$tabs = InventoryTabs.add((InventoryScreen) (Object) this, true);
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void fmab$placeTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (fmab$tabs != null) {
			InventoryTabs.place((InventoryScreen) (Object) this, fmab$tabs);
		}
	}
}
