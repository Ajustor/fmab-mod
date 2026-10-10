package com.ajustor.fmab.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;

/**
 * Les bras autour d'un fauteuil roulant : celui qui fait tourner ses roues, les mains sur les jantes,
 * d'avant en arrière ; celui qui pousse, les bras tendus vers les poignées.
 */
public enum WheelchairPose {
	NONE,
	/** Assis, il fait avancer le fauteuil à la force des bras. */
	ROLL,
	/** Debout derrière, il tient les poignées. */
	PUSH;

	public void apply(HumanoidModel<?> model, float ageInTicks) {
		if (this == ROLL) {
			// Une poussée, puis la main revient en arrière sur la jante.
			float swing = Mth.sin(ageInTicks * 0.45f) * 0.55f;
			model.rightArm.xRot = 0.15f + swing;
			model.leftArm.xRot = 0.15f + swing;
			model.rightArm.yRot = 0;
			model.leftArm.yRot = 0;
			model.rightArm.zRot = 0.3f;
			model.leftArm.zRot = -0.3f;
		} else if (this == PUSH) {
			model.rightArm.xRot = -1.05f;
			model.leftArm.xRot = -1.05f;
			model.rightArm.yRot = -0.12f;
			model.leftArm.yRot = 0.12f;
			model.rightArm.zRot = 0;
			model.leftArm.zRot = 0;
		}
	}
}
