package com.ajustor.fmab.client.render;

import com.ajustor.fmab.data.TransmutationPose;

/**
 * Ajouté à l'état de rendu du joueur : le geste de transmutation à jouer et depuis combien de temps
 * il dure (pour l'animer).
 */
public interface PoseHolder {
	TransmutationPose.Kind fmab$pose();

	/** Ticks restants avant la fin du geste, pour qu'il retombe en douceur. */
	float fmab$remaining();

	/** Ticks écoulés depuis le début du geste, pour son élan (grand si on ne sait pas quand il a commencé). */
	float fmab$elapsed();

	void fmab$setPose(TransmutationPose.Kind pose, float remaining, float elapsed);
}
