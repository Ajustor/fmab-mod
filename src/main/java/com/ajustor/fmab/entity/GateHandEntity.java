package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.util.Mth;
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
	/** L'encre qui s'échappe du bras : une poussière noire, à peine violacée. */
	private static final DustParticleOptions INK = new DustParticleOptions(0x120C1C, 1.1f);

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

	/** Où en est le bras, de 0 (il sort) à 1 (il tient sa proie) : vite au départ, il ralentit en arrivant. */
	public float reach(float partialTicks) {
		float t = Math.min(1, (tickCount + partialTicks) / REACH);
		return 1 - (1 - t) * (1 - t);
	}

	/** Longueur d'un bras sans proie, en blocs : il se dresse vers le ciel. */
	public static final float RISE = 4;

	/** Vers où le bras s'étire, depuis sa base : la poitrine de sa proie, ou le ciel. */
	public Vec3 toward(float partialTicks) {
		Entity target = target();
		return target == null ? new Vec3(0, RISE, 0)
				: target.getPosition(partialTicks).add(0, target.getBbHeight() * 0.6, 0).subtract(getPosition(partialTicks));
	}

	/**
	 * Un point du bras, par rapport à sa base. Le bras n'est pas raide : il ondule comme une encre
	 * vivante, mais reste ancré à ses deux bouts (l'ondulation s'annule à la base et à la main).
	 *
	 * @param toward vers où il s'étire, depuis sa base
	 * @param reach  où il en est, de 0 à 1
	 * @param s      position le long du bras, de 0 (la base) à 1 (la main)
	 * @param age    temps, en ticks
	 * @param seed   pour que deux bras n'ondulent pas d'un même mouvement
	 */
	public static Vec3 point(Vec3 toward, float reach, float s, float age, int seed) {
		double length = toward.length();
		if (length < 1.0e-4) {
			return Vec3.ZERO;
		}
		Vec3 dir = toward.scale(1 / length);
		// Deux directions perpendiculaires au bras, pour onduler dans l'espace et pas dans un plan.
		Vec3 side = dir.cross(Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
		Vec3 up = side.cross(dir);
		// Il ondule fort en cherchant sa proie, moins une fois qu'il la tient.
		double amplitude = Math.min(0.75, length * 0.1) * (reach >= 1 ? 0.45 : 1);
		double envelope = Mth.sin((float) Math.PI * s) * amplitude;
		double phase = seed * 1.37;
		double a = Mth.sin(age * 0.23f + s * 6.5f + (float) phase);
		double b = 0.6 * Mth.cos(age * 0.17f + s * 4.1f + (float) phase * 1.7f);
		return toward.scale(s * reach).add(side.scale(a * envelope)).add(up.scale(b * envelope));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			// Une encre qui fume : des volutes noires s'échappent le long du bras.
			if (random.nextInt(4) == 0) {
				float s = random.nextFloat();
				Vec3 at = position().add(point(toward(1), reach(1), s, tickCount, getId()));
				level().addParticle(INK,
						at.x, at.y, at.z, (random.nextDouble() - 0.5) * 0.02, 0.01, (random.nextDouble() - 0.5) * 0.02);
			}
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
