package com.ajustor.fmab.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Où en est le candidat à l'examen d'Alchimiste d'État.
 *
 * @param itemsGiven il a remis les objets demandés
 * @param passed     il a vaincu le golem : il est Alchimiste d'État
 * @param twoStages  il a réussi un cercle à deux étages, la preuve qu'il tient le rang d'Alchimiste
 */
public record ExamProgress(boolean itemsGiven, boolean passed, boolean twoStages) {
	public static final ExamProgress NONE = new ExamProgress(false, false, false);

	public static final Codec<ExamProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("items_given", false).forGetter(ExamProgress::itemsGiven),
			Codec.BOOL.optionalFieldOf("passed", false).forGetter(ExamProgress::passed),
			Codec.BOOL.optionalFieldOf("two_stages", false).forGetter(ExamProgress::twoStages)
	).apply(i, ExamProgress::new));

	public ExamProgress withItemsGiven() {
		return new ExamProgress(true, passed, twoStages);
	}

	public ExamProgress withPassed() {
		return new ExamProgress(itemsGiven, true, twoStages);
	}

	public ExamProgress withTwoStages() {
		return new ExamProgress(itemsGiven, passed, true);
	}
}
