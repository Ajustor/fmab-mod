package com.ajustor.fmab;

import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.Restoration;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les réglages du serveur : leur fichier, et le prix d'une Porte rendue. */
class FmabConfigTest {
	@Test
	void aGateCostsMoreThanAnythingElse() {
		assertEquals(100, FmabConfig.DEFAULT.gateRestorationCost(), "une Pierre pleine");
		assertTrue(FmabConfig.DEFAULT.gateRestorationCost() > Restoration.cost(BodyPart.BODY),
				"plus cher qu'un corps entier");
	}

	@Test
	void missingKeysKeepTheirDefaults() {
		assertEquals(FmabConfig.DEFAULT, FmabConfig.fromJson(JsonParser.parseString("{}").getAsJsonObject()));
		assertEquals(FmabConfig.DEFAULT.withGateRestorationCost(80),
				FmabConfig.fromJson(JsonParser.parseString("{\"gate_restoration_cost\": 80}").getAsJsonObject()));
	}

	@Test
	void fatherAwaitsFiveSacrificesByDefault() {
		assertEquals(5, FmabConfig.DEFAULT.promisedDaySacrifices(), "cinq Portes ouvertes");
		assertEquals(FmabConfig.DEFAULT.withPromisedDaySacrifices(2),
				FmabConfig.fromJson(JsonParser.parseString("{\"promised_day_sacrifices\": 2}").getAsJsonObject()));
		assertEquals(0, FmabConfig.DEFAULT.withPromisedDaySacrifices(-3).promisedDaySacrifices(), "jamais négatif");
	}

	@Test
	void theCostStaysWithinWhatAStoneHolds() {
		assertEquals(Restoration.STONE_SOULS, FmabConfig.DEFAULT.withGateRestorationCost(500).gateRestorationCost(),
				"une Pierre n'en tient pas plus");
		assertEquals(1, FmabConfig.DEFAULT.withGateRestorationCost(0).gateRestorationCost(), "jamais gratuit");
	}
}
