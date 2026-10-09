package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.stone.Eclipse;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * L'éclipse assombrit vraiment le monde : le ciel et le brouillard virent au crépuscule rouge, et la
 * lumière du ciel baisse de plus de moitié. Ce sont des couches d'attributs d'environnement, comme
 * celles de l'éclair d'orage (côté client seulement : rien ne change pour les apparitions).
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	private static final int DUSK = ARGB.color(70, 18, 20);

	@Inject(method = "addEnvironmentAttributeLayers", at = @At("RETURN"))
	private void fmab$eclipse(EnvironmentAttributeSystem.Builder builder,
			CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> cir) {
		ClientLevel self = (ClientLevel) (Object) this;
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR,
				(color, tick) -> ARGB.srgbLerp(0.85f * Eclipse.strength(self), color, DUSK));
		builder.addTimeBasedLayer(EnvironmentAttributes.FOG_COLOR,
				(color, tick) -> ARGB.srgbLerp(0.75f * Eclipse.strength(self), color, DUSK));
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_FACTOR,
				(factor, tick) -> factor * (1 - 0.6f * Eclipse.strength(self)));
	}
}
