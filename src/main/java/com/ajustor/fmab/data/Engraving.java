package com.ajustor.fmab.data;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Une gravure dans l'acier d'un automail. Pour l'instant, un cercle alchimique ; c'est un objet et non
 * un simple tracé pour que les gravures dessinées à la main (cosmétiques ou alchimiques) puissent s'y
 * ajouter en champs facultatifs, sans changer ce que les sauvegardes contiennent déjà.
 *
 * @param circle le cercle gravé
 */
public record Engraving(Drawing circle) {
	public static final Codec<Engraving> CODEC = RecordCodecBuilder.create(i -> i.group(
			FmabCodecs.DRAWING.fieldOf("circle").forGetter(Engraving::circle)
	).apply(i, Engraving::new));

	public static Engraving of(Drawing circle) {
		return new Engraving(circle);
	}
}
