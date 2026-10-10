package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.data.Transient;
import com.ajustor.fmab.registry.FmabSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Les éclairs bleus de la transmutation : des arcs en zigzag qui jaillissent du cercle et crépitent
 * quelques instants, plus nombreux et plus longs pour un grand cercle.
 */
public final class TransmutationLightning {
	/** Le bleu de la réaction alchimique, et le blanc de son cœur. */
	private static final DustParticleOptions BLUE = new DustParticleOptions(0x3FA0FF, 0.9f);
	private static final DustParticleOptions CORE = new DustParticleOptions(0xD8F0FF, 0.6f);
	/** Durée du crépitement, en ticks. */
	private static final int DURATION = 12;

	private static final class Discharge {
		final ServerLevel level;
		final Vec3 center;
		final double radius;
		final int boltsPerTick;
		int ticksLeft;

		Discharge(ServerLevel level, Vec3 center, double radius, int boltsPerTick, int ticksLeft) {
			this.level = level;
			this.center = center;
			this.radius = radius;
			this.boltsPerTick = boltsPerTick;
			this.ticksLeft = ticksLeft;
		}
	}

	private static final List<Discharge> ACTIVE = Transient.perServer(new ArrayList<>());
	/** Au-delà, les décharges les plus anciennes cèdent la place : le ciel n'a pas besoin de plus. */
	private static final int MAX_ACTIVE = 64;

	private TransmutationLightning() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Iterator<Discharge> it = ACTIVE.iterator(); it.hasNext(); ) {
				Discharge d = it.next();
				if (d.level.getServer() != server) {
					// Un monde fermé (solo) : ses décharges ne le suivent pas dans le suivant.
					it.remove();
					continue;
				}
				for (int i = 0; i < d.boltsPerTick; i++) {
					bolt(d.level, d.center, d.radius, d.level.getRandom());
				}
				if (--d.ticksLeft <= 0) {
					it.remove();
				}
			}
		});
	}

	/**
	 * Une décharge autour d'un cercle.
	 *
	 * @param radius     rayon du cercle, en blocs
	 * @param intensity  1 pour une transmutation ordinaire ; moins pour un cercle qui fait long feu
	 */
	public static void discharge(ServerLevel level, BlockPos circle, double radius, double intensity) {
		Vec3 center = Vec3.atBottomCenterOf(circle).add(0, 0.1, 0);
		int bolts = Math.max(1, (int) Math.round((2 + radius) * intensity));
		if (ACTIVE.size() >= MAX_ACTIVE) {
			ACTIVE.removeFirst();
		}
		ACTIVE.add(new Discharge(level, center, radius, bolts, Math.max(3, (int) (DURATION * intensity))));
		level.playSound(null, circle, FmabSounds.TRANSMUTE, SoundSource.PLAYERS, (float) (0.6 * intensity),
				0.9f + level.getRandom().nextFloat() * 0.2f);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.2, center.z,
				(int) (20 * intensity * radius), radius * 0.6, 0.3, radius * 0.6, 0.1);
	}

	/** Un éclair : il part d'un point du cercle et zigzague vers le haut et vers l'extérieur. */
	private static void bolt(ServerLevel level, Vec3 center, double radius, RandomSource random) {
		double angle = random.nextDouble() * Math.PI * 2;
		double r = random.nextDouble() * radius;
		Vec3 point = center.add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
		int segments = 3 + random.nextInt(4);
		for (int s = 0; s < segments; s++) {
			Vec3 next = point.add(
					(random.nextDouble() - 0.5) * 0.9 + Math.cos(angle) * 0.25,
					0.15 + random.nextDouble() * 0.45,
					(random.nextDouble() - 0.5) * 0.9 + Math.sin(angle) * 0.25);
			line(level, point, next);
			point = next;
		}
	}

	private static void line(ServerLevel level, Vec3 from, Vec3 to) {
		double length = from.distanceTo(to);
		int steps = Math.max(2, (int) (length / 0.08));
		for (int i = 0; i <= steps; i++) {
			Vec3 p = from.lerp(to, (double) i / steps);
			level.sendParticles(i % 3 == 0 ? CORE : BLUE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
	}
}
