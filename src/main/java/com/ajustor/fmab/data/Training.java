package com.ajustor.fmab.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Où en est le joueur avec Izumi.
 *
 * @param met      il l'a rencontrée : ses épreuves comptent à partir de là
 * @param achieved épreuves réussies
 * @param rewarded épreuves dont il a rendu compte (et reçu la récompense)
 */
public record Training(boolean met, Set<String> achieved, Set<String> rewarded) {
	public static final Training NONE = new Training(false, Set.of(), Set.of());

	private static final Codec<Set<String>> STRING_SET = Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf);

	public static final Codec<Training> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("met", false).forGetter(Training::met),
			STRING_SET.optionalFieldOf("achieved", Set.of()).forGetter(Training::achieved),
			STRING_SET.optionalFieldOf("rewarded", Set.of()).forGetter(Training::rewarded)
	).apply(i, Training::new));

	public Training {
		achieved = Set.copyOf(achieved);
		rewarded = Set.copyOf(rewarded);
	}

	public Training meet() {
		return new Training(true, achieved, rewarded);
	}

	public Training achieve(String trial) {
		Set<String> more = new HashSet<>(achieved);
		more.add(trial);
		return new Training(met, more, rewarded);
	}

	public Training reward(String trial) {
		Set<String> more = new HashSet<>(rewarded);
		more.add(trial);
		return new Training(met, achieved, more);
	}
}
