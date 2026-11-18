package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Une décharge de transmutation : les éclairs qui jaillissent d'un cercle et crépitent quelques ticks.
 * Elle ne fait rien, ne se sauvegarde pas : elle porte au client de quoi dessiner les éclairs (leur
 * nombre, le rayon du cercle, leur couleur), comme l'éclair d'un orage.
 */
public class DischargeEntity extends Entity {
	private static final EntityDataAccessor<Float> RADIUS =
			SynchedEntityData.defineId(DischargeEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> BOLTS =
			SynchedEntityData.defineId(DischargeEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LIFETIME =
			SynchedEntityData.defineId(DischargeEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> RED =
			SynchedEntityData.defineId(DischargeEntity.class, EntityDataSerializers.BOOLEAN);

	public DischargeEntity(EntityType<? extends DischargeEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	/** Une décharge posée en {@code at}, sur un cercle de ce rayon. */
	public static DischargeEntity spawn(ServerLevel level, Vec3 at, double radius, int bolts, int lifetime, boolean red) {
		DischargeEntity discharge = new DischargeEntity(FmabEntities.DISCHARGE, level);
		discharge.setPos(at);
		discharge.entityData.set(RADIUS, (float) radius);
		discharge.entityData.set(BOLTS, bolts);
		discharge.entityData.set(LIFETIME, lifetime);
		discharge.entityData.set(RED, red);
		level.addFreshEntity(discharge);
		return discharge;
	}

	public float radius() {
		return entityData.get(RADIUS);
	}

	public int bolts() {
		return entityData.get(BOLTS);
	}

	public int lifetime() {
		return entityData.get(LIFETIME);
	}

	/** Rouge quand des âmes nourrissent la réaction (une Pierre philosophale). */
	public boolean red() {
		return entityData.get(RED);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(RADIUS, 0f);
		builder.define(BOLTS, 1);
		builder.define(LIFETIME, 1);
		builder.define(RED, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && tickCount >= lifetime()) {
			discard();
		}
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		// Comme un éclair d'orage : visible de loin, même pour une décharge minuscule.
		return distance < 96 * 96;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}
}
