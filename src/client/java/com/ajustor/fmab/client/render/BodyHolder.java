package com.ajustor.fmab.client.render;

import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.item.AutomailItem;

import java.util.Map;
import java.util.Set;

/**
 * Ajouté à l'état de rendu du joueur : les membres que la Porte a pris, et l'automail posé sur
 * chacun d'eux.
 */
public interface BodyHolder {
	/** Les membres perdus (bras et jambes), qu'on ne dessine plus avec la peau. */
	Set<BodyPart> fmab$lostLimbs();

	/** Le modèle d'automail posé sur chaque membre perdu qui en a un. */
	Map<BodyPart, AutomailItem.Model> fmab$automails();

	void fmab$setBody(Set<BodyPart> lostLimbs, Map<BodyPart, AutomailItem.Model> automails);
}
