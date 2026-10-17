package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.client.render.AutomailLayer;
import com.ajustor.fmab.client.render.BodyCache;
import com.ajustor.fmab.client.render.BodyHolder;
import com.ajustor.fmab.client.render.PoseHolder;
import com.ajustor.fmab.client.render.WheelchairPose;
import com.ajustor.fmab.data.TransmutationPose;
import com.ajustor.fmab.entity.WheelchairEntity;
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
		TransmutationPose pose = FmabAttachments.POSE.get(entity);
		long now = entity.level().getGameTime();
		PoseHolder holder = (PoseHolder) state;
		if (pose == null || !pose.active(now)) {
			holder.fmab$setPose(TransmutationPose.Kind.NONE, 0, 0);
		} else {
			float elapsed = pose.start() == 0 ? Float.MAX_VALUE : now - pose.start() + partialTicks;
			holder.fmab$setPose(pose.kind(), pose.until() - now - partialTicks, elapsed);
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

	/** Le corps du joueur, tiré du cache : rien n'est recalculé tant que la Porte et les automails restent les mêmes. */
	@Unique
	private static void fmab$extractBody(Avatar entity, BodyHolder holder) {
		BodyCache.Body body = BodyCache.of(entity);
		holder.fmab$setBody(body.lost(), body.automails());
		holder.fmab$setBlades(body.blades());
		holder.fmab$setWorn(fmab$worn(entity));
	}

	/** Ses gants (une sorte par main) et ses tatouages, à dessiner sur son corps. */
	@Unique
	private static java.util.List<BodyHolder.Worn> fmab$worn(Avatar entity) {
		java.util.List<BodyHolder.Worn> out = new java.util.ArrayList<>();
		com.ajustor.fmab.data.Gloves gloves = FmabAttachments.GLOVES.get(entity);
		if (gloves != null) {
			for (boolean left : new boolean[]{false, true}) {
				if (gloves.get(left).getItem() instanceof com.ajustor.fmab.item.GloveItem glove) {
					out.add(new BodyHolder.Worn("glove_" + glove.kind().name().toLowerCase(java.util.Locale.ROOT),
							left ? BodyPart.LEFT_ARM : BodyPart.RIGHT_ARM));
				}
			}
		}
		com.ajustor.fmab.data.Tattoos tattoos = FmabAttachments.TATTOOS.get(entity);
		if (tattoos != null) {
			for (com.ajustor.fmab.tattoo.TattooSlot slot : com.ajustor.fmab.tattoo.TattooSlot.values()) {
				if (tattoos.get(slot).isPresent()) {
					BodyPart part = switch (slot) {
						case BACK, CHEST -> BodyPart.BODY;
						default -> slot.isLeft() ? BodyPart.LEFT_ARM : BodyPart.RIGHT_ARM;
					};
					out.add(new BodyHolder.Worn("tattoo_" + slot.id(), part));
				}
			}
		}
		return out;
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
		Player player = Minecraft.getInstance().player;
		if (player == null || !BodyCache.of(player).blades().contains(arm)) {
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
		if (player == null) {
			return false;
		}
		BodyCache.Body body = BodyCache.of(player);
		return body.lost().contains(arm) && !body.automails().containsKey(arm);
	}

	/** Le modèle d'automail posé sur ce bras du joueur, s'il l'a perdu et remplacé. */
	@Unique
	private static AutomailItem.Model fmab$handAutomail(BodyPart arm) {
		Player player = Minecraft.getInstance().player;
		return player == null ? null : BodyCache.of(player).automails().get(arm);
	}
}
