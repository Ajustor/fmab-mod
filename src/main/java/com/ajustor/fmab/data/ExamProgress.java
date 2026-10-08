package com.ajustor.fmab.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Où en est le candidat à l'examen d'Alchimiste d'État.
 *
 * @param itemsGiven il a remis les objets demandés
 * @param passed     il a vaincu le golem : il est Alchimiste d'État
 */
public record ExamProgress(boolean itemsGiven, boolean passed) {
	public static final ExamProgress NONE = new ExamProgress(false, false);

	public static final Codec<ExamProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("items_given", false).forGetter(ExamProgress::itemsGiven),
			Codec.BOOL.optionalFieldOf("passed", false).forGetter(ExamProgress::passed)
	).apply(i, ExamProgress::new));
}
