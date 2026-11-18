package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.entity.DischargeEntity;
import com.ajustor.fmab.registry.FmabSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Les éclairs bleus de la transmutation : de vrais éclairs, lumineux et ramifiés, qui jaillissent du
 * cercle et crépitent quelques instants, plus nombreux et plus longs pour un grand cercle. Rouges
 * quand une Pierre philosophale nourrit la réaction (en main, dans le corps d'une Pierre vivante,
 * d'un homonculus). Le serveur pose une {@link DischargeEntity} ; le client dessine ses éclairs.
 */
public final class TransmutationLightning {
	/** Durée du crépitement, en ticks. */
	static final int DURATION = 12;
	/** Au-delà, un cercle géant n'a pas besoin de plus d'éclairs à la fois. */
	static final int MAX_BOLTS = 24;
	/** Un éclair isolé : le temps d'un claquement. */
	private static final int ARC_DURATION = 3;

	private TransmutationLightning() {
	}

	/** Le nombre d'éclairs d'une décharge : plus pour un grand cercle, moins pour un cercle qui fait long feu. */
	static int bolts(double radius, double intensity) {
		return Math.clamp(Math.round((2 + radius) * intensity), 1, MAX_BOLTS);
	}

	/** Combien de ticks une décharge crépite. */
	static int lifetime(double intensity) {
		return Math.max(3, (int) (DURATION * intensity));
	}

	/**
	 * Une décharge autour d'un cercle.
	 *
	 * @param radius     rayon du cercle, en blocs
	 * @param intensity  1 pour une transmutation ordinaire ; moins pour un cercle qui fait long feu
	 */
	public static void discharge(ServerLevel level, BlockPos circle, double radius, double intensity) {
		discharge(level, circle, radius, intensity, false);
	}

	/** Une décharge, rouge si des âmes la nourrissent (une Pierre philosophale). */
	public static void discharge(ServerLevel level, BlockPos circle, double radius, double intensity, boolean red) {
		Vec3 center = Vec3.atBottomCenterOf(circle).add(0, 0.1, 0);
		DischargeEntity.spawn(level, center, radius, bolts(radius, intensity), lifetime(intensity), red);
		level.playSound(null, circle, FmabSounds.TRANSMUTE, SoundSource.PLAYERS, (float) (0.6 * intensity),
				0.9f + level.getRandom().nextFloat() * 0.2f);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.2, center.z,
				(int) (20 * intensity * radius), radius * 0.6, 0.3, radius * 0.6, 0.1);
	}

	/** Une décharge de cette couleur crépite-t-elle en ce moment sur ce cercle ? */
	public static boolean crackles(ServerLevel level, BlockPos circle, boolean red) {
		return !level.getEntitiesOfClass(DischargeEntity.class, new AABB(circle).inflate(1),
				d -> d.isAlive() && d.red() == red && BlockPos.containing(d.position()).equals(circle)).isEmpty();
	}

	/** Un seul éclair, qui jaillit de ce point, sans bruit : de quoi faire crépiter tout un cercle. */
	public static void arc(ServerLevel level, Vec3 at, boolean red) {
		DischargeEntity.spawn(level, at, 0, 1, ARC_DURATION, red);
	}
}
