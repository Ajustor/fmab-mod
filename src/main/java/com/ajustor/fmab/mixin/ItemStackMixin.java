package com.ajustor.fmab.mixin;

import com.ajustor.fmab.transmutation.Recomposition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Un objet qu'on retire d'une case de résultat, on l'a fait soi-même : on en connaît la structure. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "onCraftedBy", at = @At("HEAD"))
	private void fmab$understand(Player player, int amount, CallbackInfo ci) {
		if (player instanceof ServerPlayer crafter) {
			Recomposition.crafted(crafter, (ItemStack) (Object) this);
		}
	}
}
