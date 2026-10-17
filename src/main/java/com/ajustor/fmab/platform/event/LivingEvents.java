package com.ajustor.fmab.platform.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Les êtres vivants côté serveur : coups reçus et morts. */
public final class LivingEvents {
	/** Un être va être blessé : un seul refus annule le coup. */
	public static final Event<AllowDamage> ALLOW_DAMAGE = Event.create(listeners -> (entity, source, amount) -> {
		for (AllowDamage l : listeners) {
			if (!l.allowDamage(entity, source, amount)) {
				return false;
			}
		}
		return true;
	});
	/** Un être vient d'être blessé (ou d'avoir paré le coup). */
	public static final Event<AfterDamage> AFTER_DAMAGE = Event.create(listeners ->
			(entity, source, baseDamage, damage, blocked) ->
					listeners.forEach(l -> l.afterDamage(entity, source, baseDamage, damage, blocked)));
	/** Un être vient de mourir. */
	public static final Event<AfterDeath> AFTER_DEATH = Event.create(listeners -> (entity, source) ->
			listeners.forEach(l -> l.afterDeath(entity, source)));

	private LivingEvents() {
	}

	@FunctionalInterface
	public interface AllowDamage {
		boolean allowDamage(LivingEntity entity, DamageSource source, float amount);
	}

	@FunctionalInterface
	public interface AfterDamage {
		/**
		 * @param baseDamage les dégâts avant armure et effets
		 * @param damage     les dégâts réellement subis
		 * @param blocked    le coup a été paré (bouclier)
		 */
		void afterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damage, boolean blocked);
	}

	@FunctionalInterface
	public interface AfterDeath {
		void afterDeath(LivingEntity entity, DamageSource source);
	}
}
