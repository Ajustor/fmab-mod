package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.stone.Eclipse;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * L'éclipse du Jour promis, dans le ciel : le soleil s'éteint presque derrière la lune nouvelle et
 * les étoiles apparaissent en plein midi. Le ciel, le brouillard et la lumière : voir ClientLevelMixin.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void fmab$eclipse(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state,
			CallbackInfo ci) {
		if (state.skybox != DimensionType.Skybox.OVERWORLD || !Eclipse.now(level)) {
			return;
		}
		state.moonAngle = state.sunAngle;
		state.moonPhase = MoonPhase.NEW_MOON;
		// Le soleil n'est plus qu'une lueur derrière la lune (le ciel, lui, vient de ClientLevelMixin).
		state.rainBrightness = Math.min(state.rainBrightness, 0.06f);
		state.starBrightness = Math.max(state.starBrightness, 0.6f);
		state.sunriseAndSunsetColor = ARGB.color(140, 160, 30, 20);
	}
}
