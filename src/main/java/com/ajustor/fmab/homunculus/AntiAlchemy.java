package com.ajustor.fmab.homunculus;

import com.ajustor.fmab.entity.FatherEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * La zone anti-alchimie de Père : son réseau de Pierres capte l'énergie tectonique qui nourrit
 * l'alchimie d'Amestris. Autour de lui, aucun cercle ne s'allume, ni gant, ni tatouage, ni mains
 * jointes, ni Pierre. L'alkahestry de Xing, qui puise au flux du dragon, n'en souffre pas, pas plus
 * que les armes ordinaires.
 */
public final class AntiAlchemy {
	/** Rayon de la zone, en blocs. */
	public static final double RADIUS = 48;

	private AntiAlchemy() {
	}

	/** L'alchimie d'Amestris est-elle captée ici ? */
	public static boolean suppressed(ServerLevel level, BlockPos pos) {
		AABB box = new AABB(pos).inflate(RADIUS);
		return !level.getEntitiesOfClass(FatherEntity.class, box,
				f -> f.isAlive() && f.distanceToSqr(Vec3.atCenterOf(pos)) <= RADIUS * RADIUS).isEmpty();
	}

	/**
	 * Vérifie la zone avant une transmutation : si elle est captée, l'énergie ne vient pas, et
	 * l'alchimiste le sent.
	 *
	 * @return vrai si la transmutation doit s'arrêter là
	 */
	public static boolean blocks(ServerLevel level, BlockPos circle, ServerPlayer caster) {
		if (!suppressed(level, circle)) {
			return false;
		}
		caster.sendOverlayMessage(Component.translatable("transmutation.fmab.suppressed"));
		level.sendParticles(ParticleTypes.SMOKE, circle.getX() + 0.5, circle.getY() + 0.2, circle.getZ() + 0.5, 12,
				0.4, 0.05, 0.4, 0.01);
		level.playSound(null, circle, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 0.6f);
		return true;
	}
}
