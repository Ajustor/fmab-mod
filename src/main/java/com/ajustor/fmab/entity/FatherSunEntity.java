package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Le petit soleil de Père : une boule de feu lente, qu'on esquive en s'abritant ou qu'on renvoie
 * d'un coup bien placé, comme celle d'un ghast. Elle éclate sans abîmer la salle du trône.
 */
public class FatherSunEntity extends LargeFireball {
	private float power = 2.5f;

	public FatherSunEntity(EntityType<? extends FatherSunEntity> type, Level level) {
		super(type, level);
	}

	/** Lance un soleil depuis {@code from} vers {@code toward}. */
	public static FatherSunEntity launch(ServerLevel level, LivingEntity owner, Vec3 from, Vec3 toward, float power) {
		FatherSunEntity sun = new FatherSunEntity(FmabEntities.FATHER_SUN, level);
		sun.setOwner(owner);
		sun.setPos(from);
		Vec3 direction = toward.subtract(from).normalize();
		sun.setDeltaMovement(direction.scale(0.35));
		sun.accelerationPower = 0.02;
		sun.power = power;
		level.addFreshEntity(sun);
		return sun;
	}

	@Override
	protected void onHit(HitResult hit) {
		if (hit instanceof EntityHitResult entityHit) {
			onHitEntity(entityHit);
		}
		if (level() instanceof ServerLevel level) {
			level.explode(this, getX(), getY(), getZ(), power, true, Level.ExplosionInteraction.NONE);
			discard();
		}
	}
}
