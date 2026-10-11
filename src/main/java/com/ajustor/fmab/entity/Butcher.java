package com.ajustor.fmab.entity;

import net.minecraft.util.Mth;

/**
 * Les règles du combat contre Barry le Boucher ({@link BarryEntity}), sans rien du monde pour les
 * tester : son coup de hachoir en arc, annoncé ; sa ruée, une fois blessé ; son sceau, qui tient au
 * premier effondrement.
 */
public final class Butcher {
	/** Il faut l'abattre deux fois : au premier effondrement, le sceau tient bon. */
	public static final int SEAL_COLLAPSES = 2;
	/** Après le premier effondrement, il se relève vite, fou de rage, en ticks. */
	public static final int FIRST_REFORM = 20 * 5;

	/** Portée du coup de hachoir, en blocs. */
	public static final double SWEEP_REACH = 4.5;
	/** Demi-ouverture de l'arc, en degrés, de part et d'autre de là où il regarde. */
	public static final double SWEEP_HALF_ANGLE = 70;
	/** Le hachoir levé avant de frapper : le temps de reculer ou de passer derrière lui. */
	public static final int SWEEP_WINDUP = 16;
	public static final int SWEEP_COOLDOWN = 70;
	public static final float SWEEP_DAMAGE = 7;
	public static final double SWEEP_KNOCKBACK = 1.4;

	/** Tête baissée avant la ruée. */
	public static final int RUSH_WINDUP = 20;
	public static final int RUSH_TICKS = 12;
	public static final double RUSH_SPEED = 0.9;
	public static final float RUSH_DAMAGE = 8;
	public static final double RUSH_KNOCKBACK = 2;
	/** Lancé dans un mur, il reste sonné tout ce temps : de quoi frapper sans riposte. */
	public static final int STUN = 40;

	private Butcher() {
	}

	/** Le sceau peut-il s'effacer, à ce nombre d'effondrements (celui en cours compris) ? */
	public static boolean sealExposed(int collapses) {
		return collapses >= SEAL_COLLAPSES;
	}

	/** Le temps qu'il met à se reformer après ce nombre d'effondrements. */
	public static int reformTicks(int collapses, int usual) {
		return sealExposed(collapses) ? usual : FIRST_REFORM;
	}

	/**
	 * Une créature à ce décalage (dx, dz) est-elle dans l'arc du hachoir, Barry tourné vers {@code yaw}
	 * (en degrés, à la manière de Minecraft : 0 regarde vers +z) ?
	 */
	public static boolean inSweep(double dx, double dz, float yaw) {
		if (dx * dx + dz * dz > SWEEP_REACH * SWEEP_REACH) {
			return false;
		}
		float toward = (float) Math.toDegrees(Math.atan2(-dx, dz));
		return Math.abs(Mth.wrapDegrees(toward - yaw)) <= SWEEP_HALF_ANGLE;
	}

	/** Fonce-t-il ? Une fois par relève, blessé de moitié, sa cible ni trop près ni trop loin. */
	public static boolean rushes(float health, float max, boolean rushed, double distance) {
		return !rushed && health <= max / 2 && distance > 5 && distance < 20;
	}
}
