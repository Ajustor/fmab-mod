package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Un œil qui s'ouvre dans le sol, sous chaque vie que le grand cercle de la Pierre consume (pas sous
 * celle qu'une Pierre vivante absorbe de la main). Il suit sa proie, la regarde mourir, et se referme. Le temps d'une cinématique : il n'est jamais sauvegardé.
 */
public class SoulEyeEntity extends Entity {
	private static final EntityDataAccessor<Integer> TARGET =
			SynchedEntityData.defineId(SoulEyeEntity.class, EntityDataSerializers.INT);
	/** Le tick (de son âge) où il sera refermé : le client le voit venir et ferme la paupière à temps. */
	private static final EntityDataAccessor<Integer> SHUT =
			SynchedEntityData.defineId(SoulEyeEntity.class, EntityDataSerializers.INT);
	/** Le temps d'ouvrir la paupière, et de la refermer. */
	public static final int OPEN = 8;
	public static final int CLOSE = 6;

	public SoulEyeEntity(EntityType<? extends SoulEyeEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	/**
	 * Un œil sous cet être, ouvert encore {@code ticks} ticks au moins : celui qui y est déjà, gardé ouvert
	 * plus longtemps, ou un nouveau.
	 */
	public static SoulEyeEntity under(ServerLevel level, LivingEntity target, int ticks) {
		for (SoulEyeEntity eye : level.getEntitiesOfClass(SoulEyeEntity.class, target.getBoundingBox().inflate(1),
				e -> e.target() == target)) {
			eye.entityData.set(SHUT, Math.max(eye.shut(), eye.tickCount + ticks + CLOSE));
			return eye;
		}
		SoulEyeEntity eye = new SoulEyeEntity(FmabEntities.SOUL_EYE, level);
		eye.setPos(target.position());
		eye.entityData.set(TARGET, target.getId());
		eye.entityData.set(SHUT, ticks + CLOSE);
		level.addFreshEntity(eye);
		return eye;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(TARGET, -1);
		builder.define(SHUT, OPEN + CLOSE);
	}

	public Entity target() {
		int id = entityData.get(TARGET);
		return id < 0 ? null : level().getEntity(id);
	}

	public int shut() {
		return entityData.get(SHUT);
	}

	/** Combien la paupière est ouverte, de 0 à 1, à cet âge : elle s'ouvre, reste ouverte, se referme. */
	public static float openness(float age, int shut) {
		float open = Mth.clamp(age / OPEN, 0, 1);
		open = 1 - (1 - open) * (1 - open);
		return open * Mth.clamp((shut - age) / CLOSE, 0, 1);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			return;
		}
		if (tickCount >= shut()) {
			discard();
			return;
		}
		Entity target = target();
		if (target == null || !target.isAlive()) {
			// Sa proie est morte : il se referme.
			if (shut() > tickCount + CLOSE) {
				entityData.set(SHUT, tickCount + CLOSE);
			}
			return;
		}
		setPos(target.position());
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		// Jamais appelé : le type n'est pas sauvegardé (une cinématique ne survit pas à un rechargement).
	}
}
