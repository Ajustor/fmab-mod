package com.ajustor.fmab.client.render;

import com.ajustor.fmab.data.TransmutationPose;
import com.ajustor.fmab.registry.FmabAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

import java.util.function.Consumer;

/**
 * Les gestes de transmutation vus de ses propres yeux : pendant le geste, les deux mains paraissent
 * à la place de l'objet tenu, avec le même élan qu'à la troisième personne (voir
 * {@link TransmutationGestures}). Les mains claquent l'une contre l'autre devant soi, plongent vers le
 * sol pour se plaquer sur le cercle, ou la main droite se tend pour arracher une âme. Chaque bras est
 * dessiné comme la main ordinaire, automail et moignon compris, puis déplacé.
 */
public final class FirstPersonGestures {
	/** Les dernières ticks d'un geste, les mains reviennent à leur place. */
	private static final float EASE = 4;
	/** Le temps que la main gauche, d'ordinaire hors de vue, monte rejoindre la droite. */
	private static final float RISE = 2;
	/** Combien la main gauche se tient sous l'écran quand elle ne sert pas. */
	private static final float HIDDEN = 0.8f;

	private static final float CLAP_OPEN = 3;
	private static final float CLAP_HIT = 5;
	private static final float CLAP_PUSH = 9;
	private static final float PALM_RAISE = 2.5f;
	private static final float PALM_HIT = 4.5f;

	private FirstPersonGestures() {
	}

	/**
	 * Le déplacement d'une main par rapport à sa place ordinaire, dans le repère de la caméra.
	 *
	 * @param in    vers le milieu de l'écran
	 * @param up    vers le haut
	 * @param ahead vers l'avant
	 * @param yaw   rotation qui tourne la main vers le milieu, en degrés
	 * @param pitch rotation qui relève (positif) ou abaisse le bout des doigts, en degrés
	 */
	private record Offset(float in, float up, float ahead, float yaw, float pitch) {
		static final Offset REST = new Offset(0, 0, 0, 0, 0);

		Offset lerp(float k, Offset to) {
			return new Offset(Mth.lerp(k, in, to.in), Mth.lerp(k, up, to.up), Mth.lerp(k, ahead, to.ahead),
					Mth.lerp(k, yaw, to.yaw), Mth.lerp(k, pitch, to.pitch));
		}
	}

	/**
	 * Dessine les mains du geste en cours, s'il y en a un.
	 *
	 * @param arm dessine un bras à sa place ordinaire, dans la pose courante
	 * @return vrai si le geste a pris la place des mains ordinaires
	 */
	public static boolean submit(LocalPlayer player, float partialTicks, PoseStack poseStack, Consumer<HumanoidArm> arm) {
		TransmutationPose pose = player.getAttached(FmabAttachments.POSE);
		long now = player.level().getGameTime();
		if (pose == null || !pose.active(now) || player.isInvisible()) {
			return false;
		}
		float t = pose.start() == 0 ? Float.MAX_VALUE : now - pose.start() + partialTicks;
		float weight = Mth.clamp((pose.until() - now - partialTicks) / EASE, 0, 1);
		float age = player.tickCount + partialTicks;
		float tremble = Mth.sin(age * 2.1f) * 0.04f;
		Offset gesture = switch (pose.kind()) {
			case CLAP -> clap(t, tremble);
			case PALM -> palm(t, tremble);
			case HOLD -> new Offset(0.32f, -0.02f + Mth.sin(age * 3.3f) * 0.02f, 0.2f, 18, -6 + tremble * 40);
			case REACH -> reach(t, tremble);
			default -> Offset.REST;
		};
		boolean both = pose.kind() != TransmutationPose.Kind.REACH;
		for (HumanoidArm side : HumanoidArm.values()) {
			boolean main = side == player.getMainArm();
			if (!main && !both) {
				continue;
			}
			Offset at = Offset.REST.lerp(weight, gesture);
			float hidden = main ? 0 : HIDDEN * (1 - smooth(Math.min(t, 1000) / RISE) * weight);
			draw(poseStack, side, at, hidden, arm);
		}
		return true;
	}

	private static void draw(PoseStack poseStack, HumanoidArm side, Offset at, float hidden, Consumer<HumanoidArm> arm) {
		float s = side == HumanoidArm.RIGHT ? 1 : -1;
		poseStack.pushPose();
		poseStack.translate(-s * at.in(), at.up() - hidden, -at.ahead());
		poseStack.mulPose(Axis.YP.rotationDegrees(s * at.yaw()));
		poseStack.mulPose(Axis.XP.rotationDegrees(at.pitch()));
		arm.accept(side);
		poseStack.popPose();
	}

	/** Les mains jointes : elles s'écartent, se rejoignent d'un coup au milieu, tremblent, poussent. */
	private static Offset clap(float t, float tremble) {
		if (t < CLAP_OPEN) {
			float k = smooth(t / CLAP_OPEN);
			return new Offset(-0.12f * k, 0.12f * k, 0, -12 * k, 8 * k);
		}
		if (t < CLAP_HIT) {
			float k = (t - CLAP_OPEN) / (CLAP_HIT - CLAP_OPEN);
			k *= k;
			return new Offset(Mth.lerp(k, -0.12f, 0.26f), 0.12f, 0, Mth.lerp(k, -12, 14), 8);
		}
		if (t < CLAP_PUSH) {
			// Le choc fait reculer les mains d'un rien, puis elles tremblent sous l'énergie.
			float jolt = Math.max(0, 1 - (t - CLAP_HIT) / 2);
			return new Offset(0.22f + 0.04f * jolt, 0.12f - 0.05f * jolt + tremble, -0.06f * jolt, 14, 8);
		}
		float k = smooth((t - CLAP_PUSH) / 2);
		return new Offset(0.22f, Mth.lerp(k, 0.12f, 0.02f) + tremble, 0.22f * k, 14, Mth.lerp(k, 8, -6));
	}

	/** Les paumes : les mains se lèvent, plongent vers le sol devant soi, et s'y plaquent. */
	private static Offset palm(float t, float tremble) {
		if (t < PALM_RAISE) {
			float k = smooth(t / PALM_RAISE);
			return new Offset(0.25f * k, 0.12f * k, 0.05f * k, 12 * k, 10 * k);
		}
		if (t < PALM_HIT) {
			float k = (t - PALM_RAISE) / (PALM_HIT - PALM_RAISE);
			k *= k;
			return new Offset(Mth.lerp(k, 0.25f, 0.32f), Mth.lerp(k, 0.12f, -0.04f), Mth.lerp(k, 0.05f, 0.2f),
					Mth.lerp(k, 12, 18), Mth.lerp(k, 10, -8));
		}
		// L'impact : les mains rebondissent un peu, puis restent plaquées.
		float settle = Math.max(0, 1 - (t - PALM_HIT) / 3);
		return new Offset(0.32f, -0.02f - 0.04f * settle + tremble * 0.3f, 0.2f, 18, -6);
	}

	/** Le bras tendu vers une âme : il se lève vers le milieu, puis tire par saccades. */
	private static Offset reach(float t, float tremble) {
		float k = smooth(Math.min(t, 1000) / 3);
		float pull = Math.max(0, Mth.sin(t * 0.7f)) * k;
		return new Offset(0.22f * k, 0.14f * k + tremble * 0.5f, 0.25f * k - 0.15f * pull, 10 * k, 6 * k);
	}

	/** Une courbe douce de 0 à 1. */
	private static float smooth(float k) {
		k = Mth.clamp(k, 0, 1);
		return k * k * (3 - 2 * k);
	}
}
