package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * Une mine alchimique de Kimblee : la matière d'un bloc transmutée en explosif. Invisible, sinon une
 * lueur écarlate de temps en temps ; elle saute quand une créature s'en approche, ou quand son
 * poseur claque des mains. Elle se défait d'elle-même au bout de dix minutes.
 */
public class AlchemicalMineEntity extends Entity {
	private static final DustParticleOptions CRIMSON = new DustParticleOptions(0xC01030, 0.7f);
	private static final int LIFETIME = 20 * 60 * 10;
	private static final double TRIGGER = 1.6;
	private static final float POWER = 2.8f;

	private UUID owner;
	private int age;

	public AlchemicalMineEntity(EntityType<? extends AlchemicalMineEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	public AlchemicalMineEntity(Level level, Vec3 at, UUID owner) {
		this(FmabEntities.ALCHEMICAL_MINE, level);
		setPos(at);
		this.owner = owner;
	}

	public Optional<UUID> owner() {
		return Optional.ofNullable(owner);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (++age > LIFETIME) {
			discard();
			return;
		}
		if (age % 30 == 0) {
			level.sendParticles(CRIMSON, getX(), getY() + 0.1, getZ(), 1, 0.1, 0.05, 0.1, 0);
		}
		// Un délai d'armement, pour que Kimblee ait le temps de s'écarter.
		if (age > 40 && age % 4 == 0 && !level.getEntitiesOfClass(LivingEntity.class,
				getBoundingBox().inflate(TRIGGER), e -> e.isAlive() && !e.getUUID().equals(owner) && !e.isSpectator()).isEmpty()) {
			detonate(level);
		}
	}

	/** La mine saute. */
	public void detonate(ServerLevel level) {
		if (isRemoved()) {
			return;
		}
		discard();
		Entity source = owner == null ? null : level.getEntity(owner);
		level.explode(source, getX(), getY(), getZ(), POWER, Level.ExplosionInteraction.TNT);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.putInt("age", age);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		age = input.getIntOr("age", 0);
	}
}
