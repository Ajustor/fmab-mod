package com.ajustor.fmab.mixin;

import com.ajustor.fmab.world.UniqueSites;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code /place structure} pose un lieu où l'on veut, même un lieu unique ailleurs qu'à sa place
 * (voir UniqueSite) : l'opérateur sait ce qu'il fait, et les gametests en ont besoin.
 */
@Mixin(PlaceCommand.class)
abstract class PlaceCommandMixin {
	@Inject(method = "placeStructure", at = @At("HEAD"))
	private static void fmab$forced(CommandSourceStack source, Holder.Reference<Structure> structure, BlockPos pos,
			CallbackInfoReturnable<Integer> cir) {
		UniqueSites.forced(true);
	}

	@Inject(method = "placeStructure", at = @At("RETURN"))
	private static void fmab$done(CommandSourceStack source, Holder.Reference<Structure> structure, BlockPos pos,
			CallbackInfoReturnable<Integer> cir) {
		UniqueSites.forced(false);
	}
}
