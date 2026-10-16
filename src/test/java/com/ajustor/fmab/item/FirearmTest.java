package com.ajustor.fmab.item;

import com.ajustor.fmab.TestBootstrap;
import com.ajustor.fmab.registry.FmabItems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les armes à feu ne surclassent plus l'alchimie : cadence, chute des dégâts, homonculus. */
class FirearmTest {
	/** Une épée de diamant : 7 dégâts, 1,6 coup par seconde. */
	private static final float DIAMOND_SWORD_DPS = 7 * 1.6f;

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void gunsHitLessPerSecondThanADiamondSword() {
		assertTrue(((FirearmItem) FmabItems.PISTOL).damagePerSecond() < DIAMOND_SWORD_DPS);
		assertTrue(((FirearmItem) FmabItems.RIFLE).damagePerSecond() < DIAMOND_SWORD_DPS);
	}

	@Test
	void aBulletWeakensWithDistance() {
		assertEquals(7, FirearmItem.damageAt(7, 5, 40));
		assertEquals(7, FirearmItem.damageAt(7, 12, 40));
		assertEquals(3.5f, FirearmItem.damageAt(7, 40, 40), 1e-4);
		assertTrue(FirearmItem.damageAt(7, 21, 40) < 7);
	}

	@Test
	void beyondLustsReachTheGunBarelyScratchesAHomunculus() {
		// Lust frappe à 16 blocs, Pride à 20 : à 21 blocs, une balle de pistolet ne lui fait presque rien.
		float hit = FirearmItem.damageAt(7, 21, 40) * FirearmItem.HOMUNCULUS_FACTOR;
		assertTrue(hit < 2, "une balle à 21 blocs : " + hit);
	}
}
