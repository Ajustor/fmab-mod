package com.ajustor.fmab.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** La politique d'éviction des textures de cercles : la plus ancienne part, l'épinglée reste. */
class PinnedLruTest {
	@Test
	void theLeastRecentlyServedLeavesFirst() {
		PinnedLru<String, String> cache = new PinnedLru<>(2);
		cache.put("a", "A");
		cache.put("b", "B");
		cache.get("a");
		assertEquals(List.of("B"), cache.put("c", "C"));
		assertNotNull(cache.get("a"));
		assertNull(cache.get("b"));
	}

	@Test
	void aPinnedEntryIsNeverEvicted() {
		PinnedLru<String, String> cache = new PinnedLru<>(2);
		cache.put("glyph", "G");
		cache.pin("glyph");
		for (int i = 0; i < 80; i++) {
			cache.put("circle" + i, "C" + i);
		}
		assertNotNull(cache.get("glyph"));
		assertEquals(2, cache.size());
	}

	@Test
	void pinsCountAndUnpinningLetsItGo() {
		PinnedLru<String, String> cache = new PinnedLru<>(1);
		cache.put("a", "A");
		cache.pin("a");
		cache.pin("a");
		// a est épinglé, b vient d'arriver : personne ne part, la capacité est dépassée.
		assertEquals(List.of(), cache.put("b", "B"));
		// Une épingle de moins, a reste épinglé : c'est b, de trop, qui part.
		assertEquals(List.of("B"), cache.unpin("a"));
		assertEquals(List.of(), cache.unpin("a"));
		// Plus d'épingle : a part dès qu'il faut de la place.
		assertEquals(List.of("A"), cache.put("c", "C"));
	}

	@Test
	void theEntryJustStoredIsNeverEvicted() {
		PinnedLru<String, String> cache = new PinnedLru<>(1);
		cache.put("a", "A");
		cache.pin("a");
		assertEquals(List.of(), cache.put("b", "B"));
		assertNotNull(cache.get("b"));
		cache.pin("b");
		assertEquals(List.of(), cache.put("c", "C"));
		assertEquals(3, cache.size());
	}
}
