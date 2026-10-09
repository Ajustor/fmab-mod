package com.ajustor.fmab.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Wrath (Colère) : King Bradley, Généralissime d'Amestris. Un homonculus sans Pierre à épuiser : il
 * vieillit et ne se régénère pas. Mais son Œil ultime voit les coups venir : il esquive les
 * projectiles, évite une bonne part des coups portés en face et riposte aussitôt. Seul un coup dans
 * le dos ou pendant qu'il frappe le touche à coup sûr. Il ne se bat que si on le provoque.
 */
public class WrathEntity extends HomunculusEntity {
	/** Pas de Pierre à épuiser : une seule vie. */
	private static final int SOULS = 0;
	/** Part des coups portés de face qu'il esquive. */
	private static final float DODGE = 0.45f;
	/** Le temps de son propre coup, il ne voit rien venir. */
	private static final int COMMITTED = 12;

	private int committed;

	public WrathEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level, SOULS);
		// Ses sabres : il ne s'en sépare jamais (posé par une structure, il ne passe pas par finalizeSpawn).
		setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
		setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_SWORD));
		setDropChance(EquipmentSlot.MAINHAND, 0.5f);
		setDropChance(EquipmentSlot.OFFHAND, 0);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 120)
				.add(Attributes.MOVEMENT_SPEED, 0.38)
				.add(Attributes.ATTACK_DAMAGE, 12)
				.add(Attributes.ATTACK_SPEED, 2)
				.add(Attributes.FOLLOW_RANGE, 32)
				.add(Attributes.ARMOR, 6);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	protected boolean showBossBar() {
		return getTarget() != null;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		committed = COMMITTED;
		return super.doHurtTarget(level, target);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || regenerating()) {
			return super.hurtServer(level, source, damage);
		}
		boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE);
		boolean blindSpot = committed > 0 || fromBehind(source);
		if (projectile || !blindSpot && random.nextFloat() < DODGE) {
			dodge(level, source);
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	/** L'attaquant est-il dans son dos ? L'Œil ultime voit devant, pas derrière. */
	private boolean fromBehind(DamageSource source) {
		Vec3 from = source.getSourcePosition();
		if (from == null) {
			return false;
		}
		Vec3 look = getViewVector(1).multiply(1, 0, 1).normalize();
		Vec3 toAttacker = from.subtract(position()).multiply(1, 0, 1).normalize();
		return look.dot(toAttacker) < -0.3;
	}

	/** Il voit le coup venir : un pas de côté, et il riposte. */
	private void dodge(ServerLevel level, DamageSource source) {
		Vec3 side = getViewVector(1).multiply(1, 0, 1).normalize().cross(new Vec3(0, 1, 0)).scale(random.nextBoolean() ? 1 : -1);
		Vec3 to = position().add(side.scale(1.6));
		if (level.noCollision(this, getBoundingBox().move(side.scale(1.6)))) {
			teleportTo(to.x, getY(), to.z);
		}
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY(1), getZ(), 1, 0, 0, 0, 0);
		level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.HOSTILE, 1, 1.4f);
		if (source.getEntity() instanceof LivingEntity attacker && attacker != this) {
			setTarget(attacker);
			if (distanceTo(attacker) < 3.5) {
				doHurtTarget(level, attacker);
				swing(InteractionHand.MAIN_HAND);
			}
			if (attacker instanceof ServerPlayer p && random.nextInt(4) == 0) {
				p.sendOverlayMessage(Component.translatable("homunculus.fmab.wrath_eye"));
			}
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (committed > 0) {
			committed--;
		}
	}
}
