package com.ajustor.fmab.gametest;

/**
 * Les vérifications des scènes : une règle qui ne tient pas fait échouer le gametest, avec un message
 * qui dit ce qu'on attendait.
 */
final class GametestAsserts {
	private GametestAsserts() {
	}

	static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
