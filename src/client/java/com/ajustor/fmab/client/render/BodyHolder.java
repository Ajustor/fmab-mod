package com.ajustor.fmab.client.render;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.item.AutomailItem;
import com.ajustor.fmab.tattoo.BodyArt;
import com.ajustor.fmab.tattoo.BodySpot;
import net.minecraft.client.model.geom.PartPose;
import org.jspecify.annotations.Nullable;

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

	/** Les bras d'automail transmutés en lame. */
	Set<BodyPart> fmab$blades();

	void fmab$setBlades(Set<BodyPart> blades);

	/**
	 * La pose des membres du vrai modèle du joueur, relevée au moment où ses calques sont soumis : les
	 * automails la recopient os par os au lieu de rejouer l'animation.
	 */
	Map<BodyPart, PartPose> fmab$skeleton();

	void fmab$setSkeleton(Map<BodyPart, PartPose> skeleton);

	/**
	 * Une pièce portée sur le corps : son calque ({@code textures/entity/worn/}, aucun pour un tatouage),
	 * le membre qui la porte et, s'il y en a un, le vrai cercle qu'on y a inscrit, la couleur de son
	 * trait et l'endroit du membre où il se pose. Un tatouage ou une gravure porte plutôt son dessin
	 * ({@code art}, en ses couleurs). {@code engraved} : gravé dans l'automail qui remplace ce membre
	 * perdu, il se dessine donc sur l'automail et non sur la peau.
	 */
	record Worn(@Nullable String texture, BodyPart part, @Nullable Drawing circle, int ink, BodySpot spot,
			@Nullable BodyArt art, boolean engraved) {
		public Worn(String texture, BodyPart part) {
			this(texture, part, null, 0, BodySpot.HAND_BACK, null, false);
		}

		/** Porte-t-il un cercle ou un dessin à poser sur le membre ? */
		public boolean drawn() {
			return circle != null || art != null;
		}
	}

	/** Ses gants et ses tatouages. */
	java.util.List<Worn> fmab$worn();

	void fmab$setWorn(java.util.List<Worn> worn);

	/** Ce que font ses bras autour d'un fauteuil roulant. */
	WheelchairPose fmab$wheelchair();

	void fmab$setWheelchair(WheelchairPose pose);
}
