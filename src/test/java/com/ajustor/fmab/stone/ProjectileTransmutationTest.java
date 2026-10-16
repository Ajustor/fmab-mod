package com.ajustor.fmab.stone;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Un projectile vise-t-il la Pierre vivante ? Sa trajectoire, prolongée de quelques ticks, la frôle. */
class ProjectileTransmutationTest {
	/** Un joueur debout à l'origine, élargi d'une marge d'un demi-bloc. */
	private static final AABB BODY = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3).inflate(0.5);
	private static final double TICKS = 3;

	@Test
	void anArrowFlyingStraightAtThePlayerIsOnCourse() {
		assertTrue(ProjectileTransmutation.onCourse(new Vec3(0, 1, -6), new Vec3(0, 0, 3), BODY, TICKS));
	}

	@Test
	void anArrowStillTooFarForTheLookaheadIsNot() {
		assertFalse(ProjectileTransmutation.onCourse(new Vec3(0, 1, -20), new Vec3(0, 0, 3), BODY, TICKS));
	}

	@Test
	void anArrowThatPassesBesideIsNot() {
		assertFalse(ProjectileTransmutation.onCourse(new Vec3(3, 1, -6), new Vec3(0, 0, 3), BODY, TICKS));
		assertFalse(ProjectileTransmutation.onCourse(new Vec3(0, 4, -6), new Vec3(0, 0, 3), BODY, TICKS));
	}

	@Test
	void anArrowThatGrazesWithinTheMarginIsOnCourse() {
		assertTrue(ProjectileTransmutation.onCourse(new Vec3(0.7, 1, -6), new Vec3(0, 0, 3), BODY, TICKS));
	}

	@Test
	void aDiagonalShotIsOnCourse() {
		assertTrue(ProjectileTransmutation.onCourse(new Vec3(-4, 3, -4), new Vec3(1.5, -0.6, 1.5), BODY, TICKS));
	}

	@Test
	void aProjectileFallingFromAboveIsOnCourse() {
		assertTrue(ProjectileTransmutation.onCourse(new Vec3(0, 6, 0), new Vec3(0, -2, 0), BODY, TICKS));
	}

	@Test
	void aProjectileMovingAwayIsNot() {
		assertFalse(ProjectileTransmutation.onCourse(new Vec3(0, 1, -2), new Vec3(0, 0, -3), BODY, TICKS));
	}

	@Test
	void aProjectileThatHasJustPassedThroughIsNot() {
		// Encore dans la marge, mais derrière le corps : il s'en va.
		assertFalse(ProjectileTransmutation.onCourse(new Vec3(0, 1, 0.6), new Vec3(0, 0, 3), BODY, TICKS));
	}

	@Test
	void aProjectileAlreadyInTheMarginAndComingIsOnCourse() {
		assertTrue(ProjectileTransmutation.onCourse(new Vec3(0, 1, -0.6), new Vec3(0, 0, 0.5), BODY, TICKS));
	}

	@Test
	void aProjectileAtRestIsNot() {
		// Une flèche plantée dans le sol, juste à côté.
		assertFalse(ProjectileTransmutation.onCourse(new Vec3(0, 0.1, -0.6), Vec3.ZERO, BODY, TICKS));
		assertFalse(ProjectileTransmutation.onCourse(new Vec3(0, 0.1, -0.6), new Vec3(0, 0, 1e-4), BODY, TICKS));
	}
}
