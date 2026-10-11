package com.ajustor.fmab.entity;

import com.ajustor.fmab.Fmab;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Lust (Luxure) : ses doigts s'allongent en lames et transpercent tout en ligne droite, boucliers
 * compris. Elle frappe de loin, après un bref temps d'arrêt. Le feu la brûle deux fois plus.
 */
public class LustEntity extends HomunculusEntity {
	/** Les lames de Lust : des dégâts que le bouclier n'arrête pas. */
	public static final ResourceKey<DamageType> LANCE = ResourceKey.create(Registries.DAMAGE_TYPE, Fmab.id("lust_lance"));
	private static final DustParticleOptions BLADE = new DustParticleOptions(0x1A0A12, 0.7f);
	private static final int SOULS = 6;
	private static final double LANCE_RANGE = 16;
	private static final float LANCE_DAMAGE = 9;
	/** Le geste des doigts-lames, pour le rendu : rien, la main qui vise, les lames lancées. */
	public enum Lance {
		NONE, AIMING, STRIKING
	}

	private static final EntityDataAccessor<Integer> LANCE_STATE = SynchedEntityData.defineId(LustEntity.class,
			EntityDataSerializers.INT);
	/** Les lames restent tendues un instant après le coup. */
	private static final int STRIKE_SHOWN = 8;
	private int striking;

	public LustEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level, SOULS);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 60)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 6)
				.add(Attributes.FOLLOW_RANGE, 32)
				.add(Attributes.ARMOR, 4);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(LANCE_STATE, Lance.NONE.ordinal());
	}

	public Lance lance() {
		return Lance.values()[entityData.get(LANCE_STATE)];
	}

	private void lance(Lance lance) {
		entityData.set(LANCE_STATE, lance.ordinal());
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (striking > 0 && --striking == 0 && lance() == Lance.STRIKING) {
			lance(Lance.NONE);
		}
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new LanceGoal());
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected float weakness(DamageSource source) {
		return source.is(DamageTypeTags.IS_FIRE) ? 2 : 1;
	}

	/**
	 * Les doigts-lames : Lust s'arrête, pointe, et ses doigts filent en ligne droite jusqu'à seize
	 * blocs, transperçant tout ce qu'ils rencontrent.
	 */
	private class LanceGoal extends Goal {
		private static final int WINDUP = 14;
		private static final int COOLDOWN = 50;
		private int windup;
		private int cooldown;
		private Vec3 aim;

		LanceGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (cooldown > 0) {
				cooldown--;
				return false;
			}
			LivingEntity target = getTarget();
			return !regenerating() && target instanceof Player p && canReach(p, LANCE_RANGE) && distanceTo(p) > 3;
		}

		@Override
		public boolean canContinueToUse() {
			return windup > 0 && !regenerating();
		}

		@Override
		public void start() {
			windup = WINDUP;
			lance(Lance.AIMING);
			getNavigation().stop();
			if (level() instanceof ServerLevel level) {
				level.playSound(null, blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.HOSTILE, 1, 1.6f);
			}
		}

		@Override
		public void tick() {
			LivingEntity target = getTarget();
			if (target != null && windup > 4) {
				getLookControl().setLookAt(target, 60, 60);
				aim = target.getEyePosition();
			}
			if (--windup == 0 && aim != null && level() instanceof ServerLevel level) {
				strike(level, aim);
				cooldown = COOLDOWN;
				lance(Lance.STRIKING);
				striking = STRIKE_SHOWN;
			}
		}

		@Override
		public void stop() {
			if (lance() == Lance.AIMING) {
				lance(Lance.NONE);
			}
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}
	}

	private void strike(ServerLevel level, Vec3 aim) {
		Vec3 from = getEyePosition().subtract(0, 0.4, 0);
		Vec3 dir = aim.subtract(from).normalize();
		Vec3 to = from.add(dir.scale(LANCE_RANGE));
		DamageSource lance = new DamageSource(level.damageSources().damageTypes.getOrThrow(LANCE), this);
		AABB sweep = new AABB(from, to).inflate(0.8);
		for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != this && e.isAlive())) {
			if (hit.getBoundingBox().inflate(0.4).clip(from, to).isPresent()) {
				hit.hurtServer(level, lance, LANCE_DAMAGE);
			}
		}
		// Cinq doigts, cinq traits sombres.
		for (int finger = 0; finger < 5; finger++) {
			Vec3 offset = new Vec3((finger - 2) * 0.08, (finger % 2) * 0.06, 0);
			for (double d = 0; d < LANCE_RANGE; d += 0.3) {
				Vec3 p = from.add(dir.scale(d)).add(offset);
				level.sendParticles(BLADE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
			}
		}
		level.playSound(null, blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 1.2f, 0.7f);
	}
}
