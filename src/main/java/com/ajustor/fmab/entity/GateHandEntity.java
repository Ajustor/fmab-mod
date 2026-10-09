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
 * Un bras noir de la Porte : il jaillit d'un point (la Porte ouverte, ou le sol du cercle), s'allonge
 * vers sa proie, l'agrippe et la tire vers son origine. Sans proie, il se dresse vers le ciel. Il
 * n'existe que le temps d'une cinématique.
 */
public class GateHandEntity extends Entity {
	private static final EntityDataAccessor<Integer> TARGET =
			SynchedEntityData.defineId(GateHandEntity.class, EntityDataSerializers.INT);
	/** Le temps qu'il met à atteindre sa proie, en ticks. */
	public static final int REACH = 18;

	private int lifetime = 60;
	private double pull = 0.08;

	public GateHandEntity(EntityType<? extends GateHandEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	/**
	 * @param target   la proie, ou null pour un bras qui se dresse
	 * @param lifetime durée de vie, en ticks
	 * @param pull     force avec laquelle il tire sa proie vers son origine, par tick
	 */
	public static GateHandEntity reach(ServerLevel level, Vec3 from, Entity target, int lifetime, double pull) {
		GateHandEntity hand = new GateHandEntity(FmabEntities.GATE_HAND, level);
		hand.setPos(from);
		hand.entityData.set(TARGET, target == null ? -1 : target.getId());
		hand.lifetime = lifetime;
		hand.pull = pull;
		level.addFreshEntity(hand);
		return hand;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(TARGET, -1);
	}

	/** La proie, si elle est encore là. */
	public Entity target() {
		int id = entityData.get(TARGET);
		return id < 0 ? null : level().getEntity(id);
	}

	/** Où en est le bras, de 0 (il sort) à 1 (il tient sa proie). */
	public float reach(float partialTicks) {
		return Math.min(1, (tickCount + partialTicks) / REACH);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			return;
		}
		if (tickCount > lifetime) {
			discard();
			return;
		}
		Entity target = target();
		if (target != null && tickCount >= REACH && pull > 0) {
			// Il tient sa proie, et la tire vers lui.
			Vec3 toward = position().subtract(target.position()).normalize().scale(pull);
			target.setDeltaMovement(target.getDeltaMovement().scale(0.6).add(toward));
			target.hurtMarked = true;
			// Tenu, il ne tombe pas : pas de chute à payer quand le bras le lâche.
			target.resetFallDistance();
		}
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
