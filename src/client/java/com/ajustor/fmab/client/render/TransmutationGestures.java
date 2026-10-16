package com.ajustor.fmab.client.render;

import com.ajustor.fmab.data.TransmutationPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/**
 * Les gestes de l'alchimiste, par-dessus l'animation ordinaire, avec leur élan et leur retombée :
 * <ul>
 * <li>les mains jointes : les bras s'ouvrent, claquent l'une contre l'autre (le buste encaisse le
 * choc), restent jointes un instant en tremblant, puis poussent vers l'avant ;</li>
 * <li>les paumes sur le cercle : les bras se lèvent, plongent sur le cercle, le corps s'abaisse
 * avec le coup, puis les mains restent plaquées ;</li>
 * <li>les mains tenues : penché, les mains plaquées qui tremblent sous l'énergie qui coule ;</li>
 * <li>le bras tendu vers une âme qu'on arrache : il tire, par saccades.</li>
 * </ul>
 * Le geste retombe en douceur à sa fin. Joués sur le modèle du joueur seulement : ses automails en
 * recopient la pose (voir {@link AutomailLayer}).
 */
public final class TransmutationGestures {
	/** Les dernières ticks d'un geste, il se relâche vers la pose ordinaire. */
	private static final float EASE = 4;
	/** Le claquement des mains : bras ouverts jusqu'ici, mains jointes à {@link #CLAP_HIT}. */
	private static final float CLAP_OPEN = 3;
	private static final float CLAP_HIT = 5;
	/** Après le choc, les mains jointes poussent vers l'avant. */
	private static final float CLAP_PUSH = 9;
	/** Les paumes : bras levés jusqu'ici, plaqués sur le cercle à {@link #PALM_HIT}. */
	private static final float PALM_RAISE = 2.5f;
	private static final float PALM_HIT = 4.5f;

	private TransmutationGestures() {
	}

	public static void apply(HumanoidModel<?> model, AvatarRenderState state) {
		PoseHolder holder = (PoseHolder) state;
		TransmutationPose.Kind pose = holder.fmab$pose();
		if (pose == TransmutationPose.Kind.NONE) {
			return;
		}
		float weight = Mth.clamp(holder.fmab$remaining() / EASE, 0, 1);
		float t = holder.fmab$elapsed();
		float tremble = Mth.sin(state.ageInTicks * 2.1f) * 0.04f;
		switch (pose) {
			case CLAP -> clap(model, t, tremble, weight);
			case PALM -> palm(model, t, tremble, weight);
			case HOLD -> {
				float shake = Mth.sin(state.ageInTicks * 3.3f) * 0.03f;
				lean(model, 0.45f + shake, weight);
				arms(model, -0.9f + tremble, 0.18f, tremble, weight);
			}
			case REACH -> {
				// Il tire l'âme vers lui, par saccades : le bras recule et se tend de nouveau.
				float pull = Math.max(0, Mth.sin(t * 0.7f)) * 0.25f;
				model.rightArm.xRot = Mth.lerp(weight, model.rightArm.xRot, -1.5f + pull + tremble);
				model.rightArm.yRot = Mth.lerp(weight, model.rightArm.yRot, -0.1f);
				model.body.yRot = Mth.lerp(weight, model.body.yRot, -0.2f - pull * 0.4f);
				model.head.xRot = Mth.lerp(weight, model.head.xRot, model.head.xRot + 0.1f);
			}
			default -> {
			}
		}
	}

	/** Les mains jointes : l'ouverture, le claquement, le tremblement, la poussée. */
	private static void clap(HumanoidModel<?> model, float t, float tremble, float weight) {
		float forward, inward, push = 0;
		if (t < CLAP_OPEN) {
			// L'élan : les bras se lèvent en s'écartant.
			float k = smooth(t / CLAP_OPEN);
			forward = Mth.lerp(k, 0, -1.3f);
			inward = Mth.lerp(k, 0, -0.55f);
		} else if (t < CLAP_HIT) {
			// Le claquement : les mains se rejoignent d'un coup, et dépassent un peu.
			float k = (t - CLAP_OPEN) / (CLAP_HIT - CLAP_OPEN);
			forward = Mth.lerp(k * k, -1.3f, -1.45f);
			inward = Mth.lerp(k * k, -0.55f, 0.55f);
		} else if (t < CLAP_PUSH) {
			// Le choc encaissé, puis les mains jointes qui tremblent sous l'énergie.
			float k = (t - CLAP_HIT) / (CLAP_PUSH - CLAP_HIT);
			forward = -1.45f + tremble;
			inward = Mth.lerp(smooth(k), 0.55f, 0.45f);
			float jolt = Math.max(0, 1 - (t - CLAP_HIT) / 2);
			model.body.xRot += 0.12f * jolt * weight;
			model.head.xRot += 0.2f * jolt * weight;
		} else {
			// La poussée : les mains jointes partent vers l'avant et un peu vers le bas.
			float k = smooth(Mth.clamp((t - CLAP_PUSH) / 2, 0, 1));
			forward = Mth.lerp(k, -1.45f, -1.2f) + tremble;
			inward = 0.45f;
			push = k;
		}
		arms(model, forward, inward, 0, weight);
		model.body.xRot += 0.15f * push * weight;
	}

	/** Les paumes sur le cercle : les bras levés, le plongeon, l'impact, les mains plaquées. */
	private static void palm(HumanoidModel<?> model, float t, float tremble, float weight) {
		float bend, forward;
		if (t < PALM_RAISE) {
			float k = smooth(t / PALM_RAISE);
			bend = 0.15f * k;
			forward = Mth.lerp(k, 0, -2.1f);
		} else if (t < PALM_HIT) {
			float k = (t - PALM_RAISE) / (PALM_HIT - PALM_RAISE);
			bend = Mth.lerp(k * k, 0.15f, 0.55f);
			forward = Mth.lerp(k * k, -2.1f, -0.8f);
		} else {
			// L'impact : le buste plonge un peu trop, puis revient se poser.
			float settle = Math.max(0, 1 - (t - PALM_HIT) / 3);
			bend = 0.45f + 0.1f * settle;
			forward = -0.85f + tremble * 0.5f;
		}
		lean(model, bend, weight);
		arms(model, forward, 0.18f, 0, weight);
	}

	/** Une courbe douce de 0 à 1. */
	private static float smooth(float k) {
		k = Mth.clamp(k, 0, 1);
		return k * k * (3 - 2 * k);
	}

	/**
	 * Penché en avant, la tête qui regarde le cercle. Le torse pivote au cou : les hanches reculent et
	 * remontent, les jambes les suivent et le haut du corps descend pour garder les pieds au sol.
	 */
	private static void lean(HumanoidModel<?> model, float angle, float weight) {
		float before = model.body.xRot;
		float after = Mth.lerp(weight, before, angle);
		float back = 12 * (Mth.sin(after) - Mth.sin(before));
		float drop = 12 * (Mth.cos(before) - Mth.cos(after));
		model.body.xRot = after;
		model.rightLeg.z += back;
		model.leftLeg.z += back;
		model.body.y += drop;
		model.head.y += drop;
		model.rightArm.y += drop;
		model.leftArm.y += drop;
		model.head.xRot = Mth.lerp(weight, model.head.xRot, model.head.xRot + angle * 0.6f);
	}

	/**
	 * Les deux bras vers l'avant, symétriques.
	 *
	 * @param inward rotation qui rapproche les mains (positif : vers le centre)
	 */
	private static void arms(HumanoidModel<?> model, float forward, float inward, float roll, float weight) {
		model.rightArm.xRot = Mth.lerp(weight, model.rightArm.xRot, forward);
		model.leftArm.xRot = Mth.lerp(weight, model.leftArm.xRot, forward);
		model.rightArm.yRot = Mth.lerp(weight, model.rightArm.yRot, -inward);
		model.leftArm.yRot = Mth.lerp(weight, model.leftArm.yRot, inward);
		model.rightArm.zRot = Mth.lerp(weight, model.rightArm.zRot, roll);
		model.leftArm.zRot = Mth.lerp(weight, model.leftArm.zRot, -roll);
	}
}
