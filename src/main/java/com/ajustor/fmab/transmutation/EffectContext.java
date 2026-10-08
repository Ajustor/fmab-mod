package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.rules.Analysis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/**
 * Ce qu'un effet sait de la transmutation en cours.
 *
 * @param circle position du cercle (le bloc de craie, posé sur son support)
 * @param pageUp direction du monde qui correspond au haut de la page du carnet
 */
public record EffectContext(ServerLevel level, BlockPos circle, Direction pageUp, ServerPlayer caster,
		Analysis.StageEffect stage) {

	/** Le bloc qui porte le cercle. */
	public BlockPos support() {
		return circle.below();
	}

	/** Zone où l'on pose les objets à transmuter : le cercle et un peu autour. */
	public AABB onCircle() {
		return new AABB(circle).inflate(1, 0.5, 1);
	}

	/**
	 * Direction de l'effet : celle de la flèche du cercle si elle existe, sinon celle où regarde
	 * l'alchimiste.
	 */
	public Direction direction() {
		if (!stage.hasDirection()) {
			return caster.getDirection();
		}
		// Page : x vers la droite, y vers le bas. Le bas de la page est derrière pageUp.
		Direction right = pageUp.getClockWise();
		Direction down = pageUp.getOpposite();
		double cos = Math.cos(stage.direction()), sin = Math.sin(stage.direction());
		double x = right.getStepX() * cos + down.getStepX() * sin;
		double z = right.getStepZ() * cos + down.getStepZ() * sin;
		if (Math.abs(x) >= Math.abs(z)) {
			return x >= 0 ? Direction.EAST : Direction.WEST;
		}
		return z >= 0 ? Direction.SOUTH : Direction.NORTH;
	}

	public int range() {
		return Math.max(1, (int) Math.round(stage.range()));
	}
}
