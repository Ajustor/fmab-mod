package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.client.render.AutomailLayer;
import com.ajustor.fmab.client.render.BodyHolder;
import com.ajustor.fmab.client.render.PoseHolder;
import com.ajustor.fmab.client.render.WheelchairPose;
import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.TransmutationPose;
import com.ajustor.fmab.entity.WheelchairEntity;
import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.Wheelchairs;
import com.ajustor.fmab.item.AutomailItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Avatar;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Recopie dans l'état de rendu ce que le serveur a donné au joueur : son geste de transmutation, les
 * membres que la Porte lui a pris et les automails posés dessus. À la première personne, le bras perdu
 * disparaît, ou montre son automail, et sa lame s'il l'a transmuté.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;"
			+ "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void fmab$extractPose(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
		TransmutationPose pose = entity.getAttached(FmabAttachments.POSE);
		long now = entity.level().getGameTime();
		PoseHolder holder = (PoseHolder) state;
		if (pose == null || !pose.active(now)) {
			holder.fmab$setPose(TransmutationPose.Kind.NONE, 0);
		} else {
			holder.fmab$setPose(pose.kind(), pose.until() - now - partialTicks);
		}
		fmab$extractBody(entity, (BodyHolder) state);
		((BodyHolder) state).fmab$setWheelchair(fmab$wheelchairPose(entity));
	}

	/** Assis dans un fauteuil qu'il fait avancer, ou debout derrière celui qu'il pousse. */
	@Unique
	private static WheelchairPose fmab$wheelchairPose(Avatar entity) {
		if (entity.getVehicle() instanceof WheelchairEntity chair) {
			return chair.propelling() ? WheelchairPose.ROLL : WheelchairPose.NONE;
		}
		if (entity instanceof Player player && Wheelchairs.pushing(player) != null) {
			return WheelchairPose.PUSH;
		}
		return WheelchairPose.NONE;
	}

	@Unique
	private static void fmab$extractBody(Avatar entity, BodyHolder holder) {
		GateState gate = entity instanceof Player ? entity.getAttached(FmabAttachments.GATE) : null;
		holder.fmab$setBlades(Set.of());
		if (gate == null || gate.lost().isEmpty()) {
			holder.fmab$setBody(Set.of(), Map.of());
			return;
		}
		Automail automail = entity.getAttached(FmabAttachments.AUTOMAIL);
		Set<BodyPart> lost = EnumSet.noneOf(BodyPart.class);
		Map<BodyPart, AutomailItem.Model> automails = new EnumMap<>(BodyPart.class);
		Set<BodyPart> blades = EnumSet.noneOf(BodyPart.class);
		for (BodyPart part : gate.lost()) {
			if (!part.limb()) {
				continue;
			}
			lost.add(part);
			if (automail != null && automail.get(part).getItem() instanceof AutomailItem item && item.fits(part)) {
				automails.put(part, item.model());
				if (Automails.bladed(automail.get(part))) {
					blades.add(part);
				}
			}
		}
		holder.fmab$setBody(lost, automails);
		holder.fmab$setBlades(blades);
	}

	@Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
	private void fmab$hideRightHand(PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin,
			boolean sleeve, CallbackInfo ci) {
		if (fmab$bareStump(BodyPart.RIGHT_ARM)) {
			ci.cancel();
		}
	}

	@Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
	private void fmab$hideLeftHand(PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin,
			boolean sleeve, CallbackInfo ci) {
		if (fmab$bareStump(BodyPart.LEFT_ARM)) {
			ci.cancel();
		}
	}

	@ModifyVariable(method = "renderRightHand", at = @At("HEAD"), argsOnly = true)
	private Identifier fmab$rightAutomail(Identifier skin) {
		AutomailItem.Model model = fmab$handAutomail(BodyPart.RIGHT_ARM);
		return model == null ? skin : AutomailLayer.texture(model);
	}

	@ModifyVariable(method = "renderLeftHand", at = @At("HEAD"), argsOnly = true)
	private Identifier fmab$leftAutomail(Identifier skin) {
		AutomailItem.Model model = fmab$handAutomail(BodyPart.LEFT_ARM);
		return model == null ? skin : AutomailLayer.texture(model);
	}

	/** Un automail n'a pas de manche. */
	@ModifyVariable(method = "renderRightHand", at = @At("HEAD"), argsOnly = true)
	private boolean fmab$rightSleeve(boolean sleeve) {
		return sleeve && fmab$handAutomail(BodyPart.RIGHT_ARM) == null;
	}

	@ModifyVariable(method = "renderLeftHand", at = @At("HEAD"), argsOnly = true)
	private boolean fmab$leftSleeve(boolean sleeve) {
		return sleeve && fmab$handAutomail(BodyPart.LEFT_ARM) == null;
	}

	/** À la première personne, la lame transmutée sort de l'avant-bras d'automail. */
	@Inject(method = "renderRightHand", at = @At("TAIL"))
	private void fmab$rightBlade(PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin,
			boolean sleeve, CallbackInfo ci) {
		fmab$blade(BodyPart.RIGHT_ARM, poseStack, collector, light);
	}

	@Inject(method = "renderLeftHand", at = @At("TAIL"))
	private void fmab$leftBlade(PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin,
			boolean sleeve, CallbackInfo ci) {
		fmab$blade(BodyPart.LEFT_ARM, poseStack, collector, light);
	}

	@Unique
	private void fmab$blade(BodyPart arm, PoseStack poseStack, SubmitNodeCollector collector, int light) {
		AutomailItem.Model model = fmab$handAutomail(arm);
		Player player = Minecraft.getInstance().player;
		Automail automail = player == null ? null : player.getAttached(FmabAttachments.AUTOMAIL);
		if (model == null || automail == null || !Automails.bladed(automail.get(arm))) {
			return;
		}
		PlayerModel skeleton = (PlayerModel) ((LivingEntityRenderer<?, ?, ?>) (Object) this).getModel();
		ModelPart bone = arm == BodyPart.RIGHT_ARM ? skeleton.rightArm : skeleton.leftArm;
		ModelPart blade = AutomailLayer.handBlade(arm);
		blade.loadPose(bone.storePose());
		collector.submitModelPart(blade, poseStack, RenderTypes.entityCutout(AutomailLayer.BLADE_TEXTURE), light,
				OverlayTexture.NO_OVERLAY, null);
	}

	/** Le bras que le joueur voit à la première personne est-il perdu, sans automail ? */
	@Unique
	private static boolean fmab$bareStump(BodyPart arm) {
		Player player = Minecraft.getInstance().player;
		GateState gate = player == null ? null : player.getAttached(FmabAttachments.GATE);
		return gate != null && gate.lost(arm) && fmab$handAutomail(arm) == null;
	}

	/** Le modèle d'automail posé sur ce bras du joueur, s'il l'a perdu et remplacé. */
	@Unique
	private static AutomailItem.Model fmab$handAutomail(BodyPart arm) {
		Player player = Minecraft.getInstance().player;
		GateState gate = player == null ? null : player.getAttached(FmabAttachments.GATE);
		Automail automail = player == null ? null : player.getAttached(FmabAttachments.AUTOMAIL);
		if (gate == null || !gate.lost(arm) || automail == null) {
			return null;
		}
		return automail.get(arm).getItem() instanceof AutomailItem item && item.fits(arm) ? item.model() : null;
	}
}
