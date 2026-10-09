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
 * @param circle     position du cercle (le bloc inscrit, posé contre son support)
 * @param frame      orientation du cercle : sol, mur ou plafond
 * @param flow       matière produite par l'étage précédent, quand il est relié en série ; les effets
 *                   y puisent avant le monde, et y déposent ce qu'ils décomposent
 * @param knowledge  savoir de l'alchimiste, dont certains bonus changent la forme des effets
 * @param rangeBonus portée ajoutée par la montre d'Alchimiste d'État
 * @param power      multiplicateur de portée et de dégâts : la taille du cercle tracé
 */
public record EffectContext(ServerLevel level, BlockPos circle, CircleFrame frame, ServerPlayer caster,
		Analysis.StageEffect stage, List<ItemStack> flow, Knowledge knowledge, int rangeBonus, double power) {
	/** Un satellite agit à ce nombre de blocs du centre, du côté de son sommet. */
	private static final int SATELLITE_OFFSET = 2;

	/** Le bloc qui porte le cercle. */
	public BlockPos support() {
		return circle.relative(frame.normal().getOpposite());
	}

	/** D'où part l'effet : le cercle, ou le côté de son satellite. */
	public BlockPos origin() {
		if (stage.satellite() < 0) {
			return circle;
		}
		return circle.relative(frame.toWorld(stage.origin().angle()), SATELLITE_OFFSET);
	}

	/** Zone où l'on pose les objets à transmuter : le cercle et un peu autour. */
	public AABB onCircle() {
		return new AABB(circle).inflate(1, 0.5, 1);
	}

	/**
	 * Direction de l'effet. Au sol : celle de la flèche du cercle (ou du sommet d'un satellite) si
	 * elle existe, sinon celle où regarde l'alchimiste. Sur un mur ou un plafond : droit devant la
	 * surface.
	 */
	public Direction direction() {
		if (!frame.onFloor()) {
			return frame.normal();
		}
		if (!stage.hasDirection()) {
			return caster.getDirection();
		}
		Direction d = frame.toWorld(stage.direction());
		return d.getAxis().isHorizontal() ? d : caster.getDirection();
	}

	/** Comme {@link #direction()}, mais toujours horizontale : sous un plafond, celle de l'alchimiste. */
	public Direction horizontalDirection() {
		Direction d = direction();
		return d.getAxis().isHorizontal() ? d : caster.getDirection();
	}

	/** Bonus de savoir de l'école de l'effet. */
	public double perk(String type) {
		return knowledge.perk(stage.combination().school(), type);
	}

	public int range() {
		return Math.max(1, (int) Math.round((stage.range() + rangeBonus) * power));
	}

	/** Des dégâts à la mesure du cercle. */
	public float damage(float base) {
		return (float) (base * power);
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
}
