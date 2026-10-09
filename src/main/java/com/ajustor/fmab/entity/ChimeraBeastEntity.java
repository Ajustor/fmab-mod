package com.ajustor.fmab.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Une chimère massive : un lion greffé d'un serpent et d'un bouc, sortie des laboratoires de
 * l'armée. Elle se dresse avant de frapper, et sa queue de serpent empoisonne.
 */
public class ChimeraBeastEntity extends Monster {
	private static final EntityDataAccessor<Boolean> REARING =
			SynchedEntityData.defineId(ChimeraBeastEntity.class, EntityDataSerializers.BOOLEAN);
	private int rearTicks;

	public ChimeraBeastEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 40)
				.add(Attributes.MOVEMENT_SPEED, 0.27)
				.add(Attributes.ATTACK_DAMAGE, 7)
				.add(Attributes.FOLLOW_RANGE, 24)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(REARING, false);
	}

	/** Dressée sur ses pattes arrière, au moment de frapper. */
	public boolean rearing() {
		return entityData.get(REARING);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		entityData.set(REARING, true);
		rearTicks = 12;
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living && random.nextInt(3) == 0) {
			living.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
		}
		return hit;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (rearTicks > 0 && --rearTicks == 0) {
			entityData.set(REARING, false);
		}
	}
}
