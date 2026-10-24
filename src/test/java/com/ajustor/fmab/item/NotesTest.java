package com.ajustor.fmab.item;

import com.ajustor.fmab.data.Tome;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Consigner son savoir : on n'écrit que ce qu'on comprend, et les tomes d'avant se lisent toujours. */
class NotesTest {
	private static final Set<String> EXISTING = Set.of("fmab:terre", "fmab:eau", "fmab:feu", "fmab:direction");

	@Test
	void onlyKnownGlyphsAreWrittenOnceInTheAskedOrder() {
		List<String> written = Notes.chosen(List.of("fmab:eau", "fmab:feu", "fmab:eau", "fmab:inconnu", "fmab:terre"),
				Set.of("fmab:terre", "fmab:eau", "fmab:inconnu"), EXISTING);
		assertEquals(List.of("fmab:eau", "fmab:terre"), written, "ni le Feu qu'on ne comprend pas, ni un glyphe qui n'existe pas");
	}

	@Test
	void aBookOfNotesHoldsAtMostSixteenGlyphs() {
		List<String> many = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			many.add("fmab:g" + i);
		}
		assertEquals(Notes.MAX_GLYPHS, Notes.chosen(many, Set.copyOf(many), Set.copyOf(many)).size());
	}

	@Test
	void notesCarryTheirAuthorAndOldTomesStillRead() {
		Tome notes = Tome.notes("Ed", List.of("fmab:terre"));
		Tome back = Tome.CODEC.parse(JsonOps.INSTANCE, Tome.CODEC.encodeStart(JsonOps.INSTANCE, notes).getOrThrow()).getOrThrow();
		assertEquals(notes, back);
		assertEquals(Optional.of("Ed"), back.author());
		Tome old = Tome.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
				"{\"title\":\"tome.fmab.rudiments\",\"glyphs\":[\"fmab:terre\"]}")).getOrThrow();
		assertEquals(Optional.empty(), old.author(), "un tome sans auteur, comme tous ceux d'avant");
	}
}
