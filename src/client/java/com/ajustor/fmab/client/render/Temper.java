package com.ajustor.fmab.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;

/**
 * Le tempérament d'un personnage, joué par-dessus l'animation humanoïde ordinaire : sa respiration au
 * repos, le rebond et le roulis de sa démarche, son port de tête, la pose de ses bras quand il attend
 * (croisés, dans le dos, sur les hanches...) et sa garde quand il se bat. Ce qui ne bouge que pour un
 * personnage (les doigts-lames de Lust, le ventre de Gluttony) reste dans son propre modèle.
 *
 * @param tempo  vitesse du souffle (1 : un souffle toutes les 4 secondes environ)
 * @param breath ampleur du souffle, en pixels
 * @param bob    rebond du pas, en pixels
 * @param sway   roulis des épaules à chaque pas, en radians
 * @param slump  tête et épaules qui tombent (positif) ou se redressent (négatif), en radians
 * @param idle   la pose des bras au repos
 * @param guard  la garde, quand il se bat
 */
public record Temper(float tempo, float breath, float bob, float sway, float slump, Idle idle, Guard guard) {
	/** Les bras au repos. */
	public enum Idle {
		NONE, CROSSED, HIPS, BEHIND, CLASPED, POCKETS, REACH
	}

	/** Les bras en garde, quand il s'apprête à frapper. */
	public enum Guard {
		NONE, FISTS, SWORD, DUAL, PALM
	}

	/** Un soldat : droit, le pas réglé, l'épée levée au combat. */
	public static final Temper SOLDIER = new Temper(1, 0.25f, 0.6f, 0.03f, -0.05f, Idle.BEHIND, Guard.SWORD);
	/** Un officier : plus raide encore, les mains dans le dos. */
	public static final Temper OFFICER = new Temper(0.8f, 0.2f, 0.5f, 0.02f, -0.08f, Idle.BEHIND, Guard.SWORD);
	/** Les soldats immortels : voûtés, le pas lourd, les bras qui pendent vers l'avant. */
	public static final Temper IMMORTAL = new Temper(0.6f, 0.4f, 1.1f, 0.1f, 0.3f, Idle.REACH, Guard.FISTS);
	/** Izumi : les bras croisés, l'air de qui attend une bêtise. */
	public static final Temper IZUMI = new Temper(1, 0.3f, 0.6f, 0.04f, -0.04f, Idle.CROSSED, Guard.FISTS);
	/** Winry : les poings sur les hanches. */
	public static final Temper WINRY = new Temper(1.2f, 0.3f, 0.7f, 0.05f, 0, Idle.HIPS, Guard.NONE);
	/** May Chang : vive, sautillante. */
	public static final Temper MAY = new Temper(1.5f, 0.3f, 1.2f, 0.05f, -0.05f, Idle.CLASPED, Guard.FISTS);
	/** Olivier : droite comme un sabre, une main sur la garde. */
	public static final Temper OLIVIER = new Temper(0.8f, 0.2f, 0.5f, 0.02f, -0.1f, Idle.NONE, Guard.SWORD);
	/** Hohenheim : calme, les mains dans le dos. */
	public static final Temper HOHENHEIM = new Temper(0.7f, 0.3f, 0.5f, 0.03f, 0.05f, Idle.BEHIND, Guard.NONE);
	/** Marcoh : voûté, inquiet, les mains jointes. */
	public static final Temper MARCOH = new Temper(1.3f, 0.3f, 0.5f, 0.03f, 0.25f, Idle.CLASPED, Guard.NONE);
	/** Cornello : les mains jointes du prêcheur. */
	public static final Temper CORNELLO = new Temper(0.9f, 0.3f, 0.5f, 0.04f, -0.05f, Idle.CLASPED, Guard.NONE);
	/** Scar : tendu, la main droite qui décompose tendue en avant. */
	public static final Temper SCAR = new Temper(1, 0.3f, 0.7f, 0.04f, 0.08f, Idle.NONE, Guard.PALM);
	/** Lust : une démarche chaloupée, une main sur la hanche. */
	public static final Temper LUST = new Temper(0.8f, 0.3f, 0.5f, 0.09f, -0.06f, Idle.HIPS, Guard.NONE);
	/** Gluttony : il se dandine, les bras tendus vers ce qu'il va manger. */
	public static final Temper GLUTTONY = new Temper(1.2f, 0.45f, 1.0f, 0.16f, 0.1f, Idle.REACH, Guard.NONE);
	/** Sloth : las, tête basse, chaque pas pèse. */
	public static final Temper SLOTH = new Temper(0.4f, 0.6f, 1.4f, 0.08f, 0.35f, Idle.NONE, Guard.FISTS);
	/** Greed : nonchalant, les mains dans les poches. */
	public static final Temper GREED = new Temper(0.9f, 0.3f, 0.6f, 0.07f, -0.03f, Idle.POCKETS, Guard.FISTS);
	/** Wrath : le Généralissime, raide, ses deux sabres croisés au combat. */
	public static final Temper WRATH = new Temper(0.8f, 0.2f, 0.4f, 0.02f, -0.08f, Idle.BEHIND, Guard.DUAL);
	/** Envy : il sautille, fanfaron, les mains sur les hanches. */
	public static final Temper ENVY = new Temper(1.4f, 0.3f, 1.2f, 0.06f, -0.04f, Idle.HIPS, Guard.FISTS);
	/** Pride : l'enfant sage, les mains jointes devant lui. */
	public static final Temper PRIDE = new Temper(1.2f, 0.25f, 0.8f, 0.03f, 0.05f, Idle.CLASPED, Guard.NONE);
	/** Père : immobile, souverain. */
	public static final Temper FATHER = new Temper(0.5f, 0.3f, 0.4f, 0.02f, 0, Idle.NONE, Guard.NONE);
	/** Une armure vide : pas de souffle, un pas qui cliquette. */
	public static final Temper ARMOR = new Temper(0, 0, 0.8f, 0.05f, 0, Idle.NONE, Guard.SWORD);

	/** La phase de la marche, celle de l'animation vanilla des jambes. */
	private static final float STRIDE = 0.6662f;

	/**
	 * Joue le tempérament sur le modèle, déjà posé par l'animation humanoïde.
	 *
	 * @param aggressive le personnage se bat : sa garde remplace sa pose de repos
	 */
	public void apply(HumanoidModel<?> model, HumanoidRenderState state, boolean aggressive) {
		float age = state.ageInTicks;
		float speed = Mth.clamp(state.walkAnimationSpeed, 0, 1);
		float rest = 1 - Mth.clamp(speed * 2.5f, 0, 1);
		float stride = state.walkAnimationPos * STRIDE;
		// Le coup porté l'emporte sur tout : la pose revient à mesure qu'il s'achève.
		float free = state.attackTime > 0 ? 0 : 1;
		boolean swimming = state.isVisuallySwimming || state.isFallFlying || state.isPassenger;
		if (swimming) {
			return;
		}

		// Le port de tête : affaissé ou redressé, et un léger regard qui flâne au repos.
		model.head.xRot += slump;
		model.head.yRot += Mth.sin(age * 0.021f) * 0.12f * rest;
		model.body.xRot += slump * 0.25f;

		// Le souffle au repos, et le rebond du pas : tout le haut du corps monte et descend.
		float rise = Mth.sin(age * 0.07f * tempo) * breath * rest;
		float step = -Math.abs(Mth.sin(stride)) * bob * speed;
		lift(model, rise + step);
		// Le roulis des épaules, que la tête compense à moitié.
		float roll = Mth.cos(stride) * sway * speed;
		model.body.zRot += roll;
		model.head.zRot -= roll * 0.5f;
		model.rightArm.zRot += roll;
		model.leftArm.zRot += roll;

		if (aggressive && guard != Guard.NONE) {
			guard(model, guard, free, age);
		} else if (idle != Idle.NONE) {
			idle(model, idle, rest * free, age);
		}
	}

	/** Monte (négatif) ou descend le buste, la tête et les bras. */
	private static void lift(HumanoidModel<?> model, float dy) {
		model.body.y += dy;
		model.head.y += dy;
		model.rightArm.y += dy;
		model.leftArm.y += dy;
	}

	private static void idle(HumanoidModel<?> model, Idle idle, float w, float age) {
		if (w <= 0) {
			return;
		}
		float drift = Mth.sin(age * 0.05f) * 0.03f;
		switch (idle) {
			case CROSSED -> {
				arm(model.rightArm, w, -0.95f + drift, -0.55f, 0.1f);
				arm(model.leftArm, w, -0.8f + drift, 0.55f, -0.1f);
			}
			case HIPS -> {
				arm(model.rightArm, w, 0.15f, 0.25f, 0.55f + drift);
				arm(model.leftArm, w, 0.15f, -0.25f, -0.55f - drift);
			}
			case BEHIND -> {
				arm(model.rightArm, w, 0.4f, 0, -0.12f);
				arm(model.leftArm, w, 0.4f, 0, 0.12f);
			}
			case CLASPED -> {
				arm(model.rightArm, w, -0.55f + drift, -0.45f, 0);
				arm(model.leftArm, w, -0.55f + drift, 0.45f, 0);
			}
			case POCKETS -> {
				arm(model.rightArm, w, 0.12f, 0.1f, -0.06f);
				arm(model.leftArm, w, 0.12f, -0.1f, 0.06f);
			}
			case REACH -> {
				arm(model.rightArm, w, -0.75f + drift * 2, -0.1f, 0.05f);
				arm(model.leftArm, w, -0.7f - drift * 2, 0.1f, -0.05f);
			}
			default -> {
			}
		}
	}

	private static void guard(HumanoidModel<?> model, Guard guard, float w, float age) {
		float sway = Mth.sin(age * 0.12f) * 0.05f;
		switch (guard) {
			case FISTS -> {
				arm(model.rightArm, w, -1.15f + sway, -0.35f, 0);
				arm(model.leftArm, w, -0.95f - sway, 0.4f, 0);
			}
			case SWORD -> {
				arm(model.rightArm, w, -1.05f + sway, -0.15f, 0);
				arm(model.leftArm, w, -0.35f, 0.3f, -0.1f);
			}
			case DUAL -> {
				arm(model.rightArm, w, -1.1f + sway, -0.3f, 0);
				arm(model.leftArm, w, -1.1f - sway, 0.3f, 0);
			}
			case PALM -> {
				arm(model.rightArm, w, -1.45f + sway, -0.12f, 0);
				arm(model.leftArm, w, -0.3f, 0.15f, -0.1f);
			}
			default -> {
			}
		}
	}

	/** Amène un bras vers une pose, selon un poids (0 : il garde la sienne). */
	private static void arm(ModelPart arm, float w, float x, float y, float z) {
		arm.xRot = Mth.lerp(w, arm.xRot, x);
		arm.yRot = Mth.lerp(w, arm.yRot, y);
		arm.zRot = Mth.lerp(w, arm.zRot, z);
	}
}
