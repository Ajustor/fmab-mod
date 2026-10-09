package com.ajustor.fmab.homunculus;

import com.ajustor.fmab.entity.FatherEntity;
import com.ajustor.fmab.promised.NationalCircle;
import com.ajustor.fmab.stone.Eclipse;
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
 * que les armes ordinaires. Elle tient au cercle de transmutation national ({@link NationalCircle}) :
 * brisez-le, et elle s'effondre.
 */
public final class AntiAlchemy {
	/** Rayon de la zone, en blocs. */
	public static final double RADIUS = 48;
	/** Hauteur de la zone au-dessus et au-dessous de Père : la salle du trône, pas le tunnel. */
	private static final double VERTICAL = 14;

	private AntiAlchemy() {
	}

	/**
	 * L'alchimie d'Amestris est-elle captée ici ? Plus du tout une fois le cercle national brisé ;
	 * deux fois plus loin pendant le Jour promis (l'éclipse), tant qu'il tient.
	 */
	public static boolean suppressed(ServerLevel level, BlockPos pos) {
		NationalCircle circle = NationalCircle.get(level.getServer());
		if (circle.broken()) {
			return false;
		}
		double radius = radius(level, circle);
		// La zone tient au repaire : elle s'étend autour de Père, mais pas vers le tunnel, au-dessus.
		AABB box = new AABB(pos).inflate(radius, VERTICAL, radius);
		return !level.getEntitiesOfClass(FatherEntity.class, box,
				f -> f.isAlive() && Math.hypot(f.getX() - pos.getX(), f.getZ() - pos.getZ()) <= radius).isEmpty();
	}

	/** Le rayon de la zone, qui grandit pendant le Jour promis avec les points de sang encore actifs. */
	public static double radius(ServerLevel level, NationalCircle circle) {
		if (!Eclipse.now(level) || circle.broken()) {
			return RADIUS;
		}
		return RADIUS * (1 + (NationalCircle.POINTS - circle.sealedCount()) / (double) NationalCircle.POINTS);
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
