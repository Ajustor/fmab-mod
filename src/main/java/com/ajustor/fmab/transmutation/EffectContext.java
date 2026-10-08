package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.knowledge.Knowledge;
import com.ajustor.fmab.alchemy.rules.Analysis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Ce qu'un effet sait de la transmutation en cours.
 *
 * @param circle    position du cercle (le bloc de craie, posé sur son support)
 * @param pageUp    direction du monde qui correspond au haut de la page du carnet
 * @param flow      matière produite par l'étage précédent, quand il est relié en série ; les effets
 *                  y puisent avant le monde, et y déposent ce qu'ils décomposent
 * @param knowledge savoir de l'alchimiste, dont certains bonus changent la forme des effets
 */
public record EffectContext(ServerLevel level, BlockPos circle, Direction pageUp, ServerPlayer caster,
		Analysis.StageEffect stage, List<ItemStack> flow, Knowledge knowledge) {
	/** Un satellite agit à ce nombre de blocs du centre, du côté de son sommet. */
	private static final int SATELLITE_OFFSET = 2;

	/** Le bloc qui porte le cercle. */
	public BlockPos support() {
		return circle.below();
	}

	/** D'où part l'effet : le cercle, ou le côté de son satellite. */
	public BlockPos origin() {
		if (stage.satellite() < 0) {
			return circle;
		}
		return circle.relative(toWorld(stage.origin().angle()), SATELLITE_OFFSET);
	}

	/** Zone où l'on pose les objets à transmuter : le cercle et un peu autour. */
	public AABB onCircle() {
		return new AABB(circle).inflate(1, 0.5, 1);
	}

	/**
	 * Direction de l'effet : celle de la flèche du cercle (ou du sommet d'un satellite) si elle
	 * existe, sinon celle où regarde l'alchimiste.
	 */
	public Direction direction() {
		if (!stage.hasDirection()) {
			return caster.getDirection();
		}
		return toWorld(stage.direction());
	}

	/** Bonus de savoir de l'école de l'effet. */
	public double perk(String type) {
		return knowledge.perk(stage.combination().school(), type);
	}

	public int range() {
		return Math.max(1, (int) Math.round(stage.range()));
	}

	/** Ce que les satellites d'infusion ajoutent à une créature touchée par l'effet. */
	public void afflict(LivingEntity target) {
		if (stage.infused("fire")) {
			target.igniteForSeconds(5);
		}
		if (stage.infused("water")) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
		}
		if (stage.infused("air")) {
			Direction d = direction();
			target.push(d.getStepX() * 0.8, 0.3, d.getStepZ() * 0.8);
			target.hurtMarked = true;
		}
	}

	/** Angle de la page (radians, x à droite, y vers le bas) vers la direction du monde la plus proche. */
	private Direction toWorld(double angle) {
		// Le bas de la page est derrière pageUp.
		Direction right = pageUp.getClockWise();
		Direction down = pageUp.getOpposite();
		double cos = Math.cos(angle), sin = Math.sin(angle);
		double x = right.getStepX() * cos + down.getStepX() * sin;
		double z = right.getStepZ() * cos + down.getStepZ() * sin;
		if (Math.abs(x) >= Math.abs(z)) {
			return x >= 0 ? Direction.EAST : Direction.WEST;
		}
		return z >= 0 ? Direction.SOUTH : Direction.NORTH;
	}
}
