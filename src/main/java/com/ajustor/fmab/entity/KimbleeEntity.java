package com.ajustor.fmab.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Solf J. Kimblee, l'Alchimiste Écarlate, dans les ruines d'Ishval où il a fait la guerre. Ses
 * paumes tatouées changent ce qu'il touche en explosif : il marque d'un cercle rouge le sol sous sa
 * cible, qui saute peu après (on a le temps de s'écarter), et ses coups au corps à corps détonent.
 * Ses propres explosions ne le blessent pas.
 */
public class KimbleeEntity extends Monster {
	/** Le cercle rouge, là où ça va sauter. */
	private static final DustParticleOptions MARK = new DustParticleOptions(0xE0202A, 1.4f);
	static final int FUSE = 30;
	private static final int MARK_COOLDOWN = 80;
	private static final double MARK_REACH = 16;
	private static final float BLAST = 2.2f;

	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(),
			BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
	private @Nullable BlockPos marked;
	private int fuse;
	private int cooldown = MARK_COOLDOWN / 2;
	private boolean taunted;

	public KimbleeEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 90)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.ATTACK_DAMAGE, 5)
				.add(Attributes.ARMOR, 4)
				.add(Attributes.FOLLOW_RANGE, 32);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// Ses propres détonations ne le touchent pas.
		if (source.getEntity() == this) {
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			// Ce qu'il touche, il le change en explosif.
			level.explode(this, target.getX(), target.getY(0.5), target.getZ(), 1.2f, Level.ExplosionInteraction.NONE);
		}
		return hit;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity target = getTarget();
		if (marked != null) {
			level.sendParticles(MARK, marked.getX() + 0.5, marked.getY() + 0.1, marked.getZ() + 0.5, 6, 0.6, 0, 0.6, 0);
			if (--fuse <= 0) {
				level.explode(this, marked.getX() + 0.5, marked.getY() + 0.2, marked.getZ() + 0.5, BLAST,
						Level.ExplosionInteraction.MOB);
				marked = null;
			}
			return;
		}
		if (target == null || !target.isAlive() || distanceToSqr(target) > MARK_REACH * MARK_REACH) {
			return;
		}
		if (!taunted && target instanceof ServerPlayer player) {
			taunted = true;
			player.sendSystemMessage(Component.translatable("entity.fmab.kimblee.taunt"));
		}
		if (--cooldown <= 0) {
			cooldown = MARK_COOLDOWN;
			marked = target.blockPosition();
			fuse = FUSE;
			level.playSound(null, blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.HOSTILE, 1.5f, 0.6f);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.6), getZ(), 12, 0.3, 0.3, 0.3, 0.1);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		BossBars.update(bossEvent, this, BossBars.engaged(this));
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}
}
