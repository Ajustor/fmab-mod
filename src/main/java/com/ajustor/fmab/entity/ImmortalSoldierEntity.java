package com.ajustor.fmab.entity;

import com.ajustor.fmab.item.BriggsSabreItem;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Un soldat immortel : un corps vide animé par une âme captive, fabriqué en série sous Central. Il
 * se régénère sans cesse et se relève une fois abattu. Le feu, le sabre de Briggs ou l'alkahestry
 * l'achèvent pour de bon.
 */
public class ImmortalSoldierEntity extends Monster {
	private static final DustParticleOptions FLESH = new DustParticleOptions(0x9A8C80, 1.0f);
	private static final int RISE = 60;

	private boolean risen;
	private int down;

	public ImmortalSoldierEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30)
				.add(Attributes.MOVEMENT_SPEED, 0.26)
				.add(Attributes.ATTACK_DAMAGE, 6)
				.add(Attributes.FOLLOW_RANGE, 28);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** Ce qui l'achève : le feu, le sabre de Briggs, l'alkahestry. */
	private static boolean finishes(DamageSource source) {
		ItemStack weapon = source.getWeaponItem();
		return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
				|| weapon != null && weapon.getItem() instanceof BriggsSabreItem
				|| source.typeHolder().unwrapKey().map(k -> k.identifier().getPath().equals("alkahestry")).orElse(false);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (down > 0 && !finishes(source)) {
			return false;
		}
		if (damage >= getHealth() && !risen && !finishes(source)) {
			// Abattu, il s'effondre... et se relèvera.
			risen = true;
			down = RISE;
			setHealth(getMaxHealth() / 2);
			setTarget(null);
			level.playSound(null, blockPosition(), SoundEvents.ZOMBIE_DEATH, SoundSource.HOSTILE, 1, 0.7f);
			return true;
		}
		return super.hurtServer(level, source, damage);
	}

	/** Effondré, il attend de se relever. */
	public boolean down() {
		return down > 0;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (down > 0) {
			down--;
			getNavigation().stop();
			setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
			level.sendParticles(FLESH, getX(), getY(0.3), getZ(), 2, 0.3, 0.2, 0.3, 0);
			if (down == 0) {
				level.playSound(null, blockPosition(), SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 1, 0.6f);
			}
			return;
		}
		if (tickCount % 10 == 0 && getHealth() < getMaxHealth()) {
			heal(1);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("risen", risen);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		risen = input.getBooleanOr("risen", false);
	}
}
