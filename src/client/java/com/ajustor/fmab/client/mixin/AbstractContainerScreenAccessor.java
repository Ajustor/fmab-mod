package com.ajustor.fmab.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** La position de la fenêtre d'un conteneur, pour y accrocher les onglets de l'inventaire. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int fmab$leftPos();

	@Accessor("topPos")
	int fmab$topPos();
}
