package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.client.render.BodyHolder;
import com.ajustor.fmab.client.render.TransmutationGestures;
import com.ajustor.fmab.gate.BodyPart;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Le modèle du joueur joue les gestes de transmutation, et ne dessine plus les membres que la Porte a
 * pris (leur manche ou leur jambe de pantalon disparaît avec eux) : l'automail, s'il y en a un, est
 * dessiné à leur place par {@link com.ajustor.fmab.client.render.AutomailLayer}.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin extends HumanoidModel<AvatarRenderState> {
	protected PlayerModelMixin(ModelPart root) {
		super(root);
	}

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void fmab$pose(AvatarRenderState state, CallbackInfo ci) {
		TransmutationGestures.apply(this, state);
		for (BodyPart part : ((BodyHolder) state).fmab$lostLimbs()) {
			switch (part) {
				case RIGHT_ARM -> rightArm.visible = false;
				case LEFT_ARM -> leftArm.visible = false;
				case RIGHT_LEG -> rightLeg.visible = false;
				case LEFT_LEG -> leftLeg.visible = false;
				default -> {
				}
			}
		}
	}
}
