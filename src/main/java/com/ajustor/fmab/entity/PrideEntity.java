package com.ajustor.fmab.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Pride (Orgueil) : Selim Bradley, un enfant en apparence. Sa vraie arme, ce sont ses ombres :
 * elles tranchent et dévorent tout ce qui se trouve à leur portée. Mais une ombre ne vit qu'avec
 * une source de lumière : dans le noir complet, ou sous une lumière intense, ses ombres
 * disparaissent, et son corps d'enfant est vulnérable.
 */
public class PrideEntity extends HomunculusEntity {
	private static final EntityDataAccessor<Boolean> REVEALED =
			SynchedEntityData.defineId(PrideEntity.class, EntityDataSerializers.BOOLEAN);
	private static final DustParticleOptions SHADOW = new DustParticleOptions(0x050508, 1.6f);
	private static final int SOULS = 7;
	private static final double REACH = 20;
	private static final float SHADOW_DAMAGE = 8;
	/** Ses ombres ne vivent qu'entre ces niveaux de lumière. */
	private static final int DARKNESS = 2;
	private static final int BRIGHTNESS = 14;

	private int shadowCooldown;
	private int closeTicks;

	public PrideEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level, SOULS);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 40)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.ATTACK_DAMAGE, 3)
				.add(Attributes.FOLLOW_RANGE, 24)
				.add(Attributes.SCALE, 0.75);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(REVEALED, false);
	}

	public boolean revealed() {
		return entityData.get(REVEALED);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5) {
			@Override
			public boolean canUse() {
				return !revealed() && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	/** Ses ombres vivent-elles ici ? Ni dans le noir complet, ni sous une lumière intense. */
	public boolean shadowsAlive() {
		int light = level().getMaxLocalRawBrightness(blockPosition());
		return light > DARKNESS && light < BRIGHTNESS;
	}

	@Override
	protected boolean showBossBar() {
		return revealed();
	}

	@Override
	protected float weakness(DamageSource source) {
		// Sans ses ombres, ce n'est qu'un corps d'enfant.
		return revealed() && !shadowsAlive() ? 2.5f : 1;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (!revealed()) {
			reveal(level);
		}
		// Ses ombres parent les coups tant qu'elles vivent.
		if (shadowsAlive() && random.nextFloat() < 0.5f) {
			level.sendParticles(SHADOW, getX(), getY(0.5), getZ(), 20, 0.4, 0.4, 0.4, 0);
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	private void reveal(ServerLevel level) {
		entityData.set(REVEALED, true);
		level.playSound(null, blockPosition(), SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.5f, 1.4f);
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(24))) {
			p.sendSystemMessage(Component.translatable("homunculus.fmab.pride_reveals"));
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!(level() instanceof ServerLevel level) || regenerating()) {
			return;
		}
		if (!revealed()) {
			// L'enfant sage se lasse vite qu'on le dévisage dans la pénombre.
			Player near = level.getNearestPlayer(this, 5);
			closeTicks = near != null && !near.isCreative() && shadowsAlive() ? closeTicks + 1 : 0;
			if (closeTicks > 60) {
				reveal(level);
				setTarget(near);
			}
			return;
		}
		if (getTarget() == null) {
			Player near = level.getNearestPlayer(this, REACH);
			if (near != null && !near.isCreative() && !near.isSpectator()) {
				setTarget(near);
			}
		}
		if (!shadowsAlive()) {
			// Sans ombres, il fuit vers la pénombre en titubant.
			addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1, false, false));
			return;
		}
		if (shadowCooldown > 0) {
			shadowCooldown--;
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && target.isAlive() && distanceTo(target) <= REACH) {
			lash(level, target);
			shadowCooldown = 30;
		}
		devour(level);
	}

	/** Une ombre file au ras du sol et tranche la cible. */
	private void lash(ServerLevel level, LivingEntity target) {
		Vec3 from = position();
		Vec3 to = target.position();
		Vec3 step = to.subtract(from);
		int points = (int) (step.length() * 3);
		for (int i = 0; i <= points; i++) {
			Vec3 p = from.add(step.scale((double) i / points));
			level.sendParticles(SHADOW, p.x, p.y + 0.1 + Math.sin(i * 0.6) * 0.3, p.z, 2, 0.1, 0.05, 0.1, 0);
		}
		target.hurtServer(level, damageSources().mobAttack(this), SHADOW_DAMAGE);
		target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
		level.playSound(null, target.blockPosition(), SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 1, 0.5f);
	}

	/** Ses ombres dévorent les bêtes qui passent à leur portée, et il s'en nourrit. */
	private void devour(ServerLevel level) {
		if (random.nextInt(40) != 0) {
			return;
		}
		for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(8),
				m -> !(m instanceof HomunculusEntity) && m.isAlive())) {
			BlockPos at = mob.blockPosition();
			level.sendParticles(SHADOW, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 30, 0.4, 0.6, 0.4, 0);
			mob.kill(level);
			heal(6);
			return;
		}
	}
}
