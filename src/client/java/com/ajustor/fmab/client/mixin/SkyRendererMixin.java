package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.client.render.EclipseSky;
import com.ajustor.fmab.stone.Eclipse;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.3 {
/*import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
*///?} else {
import com.mojang.blaze3d.pipeline.RenderPipeline;
//?}

/**
 * L'éclipse du Jour promis, dans le ciel : le jour de l'éclipse, la lune s'approche du soleil toute la
 * matinée, le mord, le couvre (les étoiles apparaissent en plein midi), puis s'en éloigne. Voir
 * {@link EclipseSky} pour la façon dont elle le cache, et ClientLevelMixin pour le ciel, le brouillard
 * et la lumière.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
	//? if >=26.3 {
	/*@Shadow
	private void renderSun(RenderPass pass, float alpha, PoseStack poseStack) {
	}

	@Shadow
	private void renderMoon(RenderPass pass, MoonPhase phase, float alpha, PoseStack poseStack) {
	}
	*///?} else {
	@Shadow
	private void renderSun(float alpha, PoseStack poseStack) {
	}

	@Shadow
	private void renderMoon(MoonPhase phase, float alpha, PoseStack poseStack) {
	}
	//?}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void fmab$eclipse(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state,
			CallbackInfo ci) {
		if (state.skybox != DimensionType.Skybox.OVERWORLD) {
			EclipseSky.update(Float.NaN, 0);
			return;
		}
		long clock = level.getOverworldClockTime();
		float separation = Eclipse.separation(clock);
		EclipseSky.update(separation, Eclipse.coverage(clock));
		if (Float.isNaN(separation)) {
			return;
		}
		// La lune quitte sa place, à l'opposé du soleil, pour le suivre sur sa route.
		state.moonAngle = state.sunAngle + separation;
		state.moonPhase = MoonPhase.FULL_MOON;
		float strength = Eclipse.strength(level);
		if (strength <= 0) {
			return;
		}
		state.starBrightness = Math.max(state.starBrightness, 0.6f * strength);
		int glow = ARGB.color(Math.round(140 * strength), 160, 30, 20);
		//? if >=26.3 {
		/*state.sunriseAndSunsetColor = ARGB.vector4fFromARGB32(glow);
		*///?} else {
		state.sunriseAndSunsetColor = glow;
		//?}
	}

	// --- la couronne : le soleil redessiné plus grand, derrière la lune -----------------------------
	//? if >=26.3 {
	/*@Inject(method = "renderSunMoonAndStars", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSun(Lcom/mojang/renderpearl/api/commands/RenderPass;FLcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void fmab$corona(RenderPass pass, PoseStack poseStack, float sunAngle, float moonAngle, float starAngle,
			MoonPhase phase, float alpha, float starBrightness, CallbackInfo ci) {
		if (EclipseSky.corona() > 0) {
			EclipseSky.drawing(EclipseSky.Drawing.CORONA);
			renderSun(pass, alpha * EclipseSky.corona(), poseStack);
			EclipseSky.drawing(EclipseSky.Drawing.NONE);
		}
	}

	@Inject(method = "renderSunMoonAndStars", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/renderer/SkyRenderer;renderMoon(Lcom/mojang/renderpearl/api/commands/RenderPass;Lnet/minecraft/world/level/MoonPhase;FLcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void fmab$silhouette(RenderPass pass, PoseStack poseStack, float sunAngle, float moonAngle, float starAngle,
			MoonPhase phase, float alpha, float starBrightness, CallbackInfo ci) {
		if (EclipseSky.mask() > 0) {
			EclipseSky.drawing(EclipseSky.Drawing.MASK);
			renderMoon(pass, phase, alpha, poseStack);
			EclipseSky.drawing(EclipseSky.Drawing.NONE);
		}
	}

	@ModifyArg(method = "renderSun", index = 2, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/SkyRenderer;applyCelestialBodyTransform(Lcom/mojang/blaze3d/vertex/PoseStack;FF)Lorg/joml/Matrix4f;"))
	private float fmab$coronaSize(float size) {
		return EclipseSky.drawing() == EclipseSky.Drawing.CORONA ? size * EclipseSky.CORONA_SCALE : size;
	}

	@ModifyArg(method = "renderSun", index = 1, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"))
	private Vector4f fmab$coronaColor(Vector4f color) {
		return EclipseSky.drawing() == EclipseSky.Drawing.CORONA ? EclipseSky.coronaColor(color) : color;
	}

	@ModifyArg(method = "renderMoon", index = 2, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/SkyRenderer;applyCelestialBodyTransform(Lcom/mojang/blaze3d/vertex/PoseStack;FF)Lorg/joml/Matrix4f;"))
	private float fmab$moonSize(float size) {
		return EclipseSky.moonSize(size);
	}

	@ModifyArg(method = "renderMoon", index = 1, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"))
	private Vector4f fmab$moonColor(Vector4f color) {
		return EclipseSky.active() ? EclipseSky.moonColor(color) : color;
	}

	@ModifyArg(method = "drawCelestialBody", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;getCompiledPipeline(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;)Lcom/mojang/renderpearl/api/pipeline/CompiledRenderPipeline;"))
	private RenderPipeline fmab$moonPipeline(RenderPipeline pipeline) {
		return EclipseSky.drawing() == EclipseSky.Drawing.MASK ? EclipseSky.MASK : pipeline;
	}
	*///?} else {
	@Inject(method = "renderSunMoonAndStars", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSun(FLcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void fmab$corona(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase phase,
			float alpha, float starBrightness, CallbackInfo ci) {
		if (EclipseSky.corona() > 0) {
			EclipseSky.drawing(EclipseSky.Drawing.CORONA);
			renderSun(alpha * EclipseSky.corona(), poseStack);
			EclipseSky.drawing(EclipseSky.Drawing.NONE);
		}
	}

	@Inject(method = "renderSunMoonAndStars", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/renderer/SkyRenderer;renderMoon(Lnet/minecraft/world/level/MoonPhase;FLcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void fmab$silhouette(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase phase,
			float alpha, float starBrightness, CallbackInfo ci) {
		if (EclipseSky.mask() > 0) {
			EclipseSky.drawing(EclipseSky.Drawing.MASK);
			renderMoon(phase, alpha, poseStack);
			EclipseSky.drawing(EclipseSky.Drawing.NONE);
		}
	}

	@ModifyArg(method = "renderSun", index = 0, at = @At(value = "INVOKE",
			target = "Lorg/joml/Matrix4fStack;scale(FFF)Lorg/joml/Matrix4f;"))
	private float fmab$coronaWidth(float size) {
		return EclipseSky.drawing() == EclipseSky.Drawing.CORONA ? size * EclipseSky.CORONA_SCALE : size;
	}

	@ModifyArg(method = "renderSun", index = 2, at = @At(value = "INVOKE",
			target = "Lorg/joml/Matrix4fStack;scale(FFF)Lorg/joml/Matrix4f;"))
	private float fmab$coronaDepth(float size) {
		return EclipseSky.drawing() == EclipseSky.Drawing.CORONA ? size * EclipseSky.CORONA_SCALE : size;
	}

	@ModifyArg(method = "renderSun", index = 1, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
	private Vector4f fmab$coronaColor(Vector4f color) {
		return EclipseSky.drawing() == EclipseSky.Drawing.CORONA ? EclipseSky.coronaColor(color) : color;
	}

	@ModifyArg(method = "renderMoon", index = 0, at = @At(value = "INVOKE",
			target = "Lorg/joml/Matrix4fStack;scale(FFF)Lorg/joml/Matrix4f;"))
	private float fmab$moonWidth(float size) {
		return EclipseSky.moonSize(size);
	}

	@ModifyArg(method = "renderMoon", index = 2, at = @At(value = "INVOKE",
			target = "Lorg/joml/Matrix4fStack;scale(FFF)Lorg/joml/Matrix4f;"))
	private float fmab$moonDepth(float size) {
		return EclipseSky.moonSize(size);
	}

	@ModifyArg(method = "renderMoon", index = 1, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
	private Vector4f fmab$moonColor(Vector4f color) {
		return EclipseSky.active() ? EclipseSky.moonColor(color) : color;
	}

	@ModifyArg(method = "renderMoon", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"))
	private RenderPipeline fmab$moonPipeline(RenderPipeline pipeline) {
		return EclipseSky.drawing() == EclipseSky.Drawing.MASK ? EclipseSky.MASK : pipeline;
	}
	//?}
}
