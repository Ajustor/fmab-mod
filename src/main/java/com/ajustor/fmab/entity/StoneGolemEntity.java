package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.state.StateExam;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * Golem de pierre de l'examen d'État. Il est lié à un cercle gravé au sol : tant que le cercle
 * tient, il se reconstruit. Il faut briser le cercle, puis le golem.
 */
public class StoneGolemEntity extends PathfinderMob {
	/** Rayon autour du cercle au-delà duquel le golem retourne le garder. */
	private static final double LEASH = 16;
	private static final int ATTACK_ANIMATION = 10;

	private BlockPos anchor;
	private UUID candidate;
	private int attackAnimation;

	public StoneGolemEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 80)
				.add(Attributes.MOVEMENT_SPEED, 0.24)
				.add(Attributes.ATTACK_DAMAGE, 7)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1)
				.add(Attributes.FOLLOW_RANGE, 24);
	}

	/** Lie le golem à son cercle et au candidat qu'il éprouve. */
	public void bind(BlockPos anchor, ServerPlayer candidate) {
		this.anchor = anchor.immutable();
		this.candidate = candidate.getUUID();
		setTarget(candidate);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	public boolean anchored() {
		return anchor != null && level().getBlockState(anchor).is(FmabBlocks.TRANSMUTATION_CIRCLE);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		ServerPlayer player = candidate == null ? null : level.getServer().getPlayerList().getPlayer(candidate);
		if (player == null || !player.isAlive() || player.level() != level) {
			// Sans candidat, l'épreuve s'arrête : le golem retourne à la pierre.
			crumble(level);
			return;
		}
		setTarget(player);
		if (anchored() && tickCount % 20 == 0 && getHealth() < getMaxHealth()) {
			heal(2);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.5, getZ(), 6, 0.4, 0.6, 0.4, 0.05);
		}
		if (anchor != null && distanceToSqr(Vec3.atCenterOf(anchor)) > LEASH * LEASH) {
			getNavigation().moveTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5, 1.2);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (attackAnimation > 0) {
			attackAnimation--;
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		attackAnimation = ATTACK_ANIMATION;
		level.broadcastEntityEvent(this, (byte) 4);
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt) {
			target.setDeltaMovement(target.getDeltaMovement().add(0, 0.4, 0));
		}
		return hurt;
	}

	@Override
	public void handleEntityEvent(byte event) {
		if (event == 4) {
			attackAnimation = ATTACK_ANIMATION;
		} else {
			super.handleEntityEvent(event);
		}
	}

	public float attackAnimation(float partialTicks) {
		return attackAnimation > 0 ? attackAnimation - partialTicks : 0;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (anchored() && damage >= getHealth()) {
			// Le cercle le reconstruit : il retombe en morceaux et se relève aussitôt.
			setHealth(getMaxHealth());
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1, getZ(), 30, 0.6, 1, 0.6, 0.1);
			level.playSound(null, blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.5f, 0.6f);
			if (source.getEntity() instanceof ServerPlayer player) {
				player.sendOverlayMessage(Component.translatable("entity.fmab.stone_golem.rebuilt"));
			}
			return true;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level && candidate != null) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(candidate);
			if (player != null) {
				StateExam.pass(player);
			}
		}
	}

	private void crumble(ServerLevel level) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 20, 0.5, 1, 0.5, 0.02);
		if (anchor != null && anchored()) {
			level.removeBlock(anchor, false);
		}
		discard();
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.storeNullable("anchor", BlockPos.CODEC, anchor);
		if (candidate != null) {
			output.putString("candidate", candidate.toString());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		anchor = input.read("anchor", BlockPos.CODEC).orElse(null);
		candidate = input.getString("candidate").map(UUID::fromString).orElse(null);
	}

	public Optional<BlockPos> anchor() {
		return Optional.ofNullable(anchor);
	}
}
