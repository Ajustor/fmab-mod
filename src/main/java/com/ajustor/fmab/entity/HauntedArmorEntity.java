package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Une armure habitée : une âme criminelle liée par un sceau de sang à une armure vide, comme les
 * frères Slicer. Abattue, elle tombe en morceaux ; mais tant que le sceau tient, elle se reforme.
 * Il faut, pendant qu'elle gît, effacer le sceau à la main (clic droit) pour libérer l'âme.
 */
public class HauntedArmorEntity extends Monster {
	private static final EntityDataAccessor<Boolean> COLLAPSED =
			SynchedEntityData.defineId(HauntedArmorEntity.class, EntityDataSerializers.BOOLEAN);
	private static final DustParticleOptions SEAL = new DustParticleOptions(0xA01020, 1.0f);
	/** Le temps qu'elle met à se reformer, en ticks. */
	private static final int REFORM = 20 * 15;

	private int collapsedFor;
	/** Le sceau est effacé : le prochain coup est le dernier. */
	private boolean released;

	public HauntedArmorEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 34)
				.add(Attributes.MOVEMENT_SPEED, 0.24)
				.add(Attributes.ATTACK_DAMAGE, 6)
				.add(Attributes.ARMOR, 10)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
				.add(Attributes.FOLLOW_RANGE, 24);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(COLLAPSED, false);
	}

	public boolean collapsed() {
		return entityData.get(COLLAPSED);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
			@Override
			public boolean canUse() {
				return !collapsed() && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (released || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		if (collapsed()) {
			return false;
		}
		if (damage >= getHealth()) {
			collapse(level);
			return true;
		}
		return super.hurtServer(level, source, damage);
	}

	/** Elle tombe en morceaux ; le sceau, lui, tient encore. */
	private void collapse(ServerLevel level) {
		entityData.set(COLLAPSED, true);
		collapsedFor = 0;
		setHealth(1);
		setTarget(null);
		getNavigation().stop();
		level.playSound(null, blockPosition(), FmabSounds.ARMOR_COLLAPSE, SoundSource.HOSTILE, 1.5f, 0.9f);
		level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6f, 1.6f);
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(16))) {
			p.sendOverlayMessage(Component.translatable("entity.fmab.haunted_armor.collapsed"));
		}
	}

	/** Le joueur efface le sceau de sang : l'âme s'en va, l'armure n'est plus qu'une armure. */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!collapsed()) {
			return InteractionResult.PASS;
		}
		if (level() instanceof ServerLevel level) {
			level.sendParticles(SEAL, getX(), getY(0.5), getZ(), 40, 0.4, 0.4, 0.4, 0);
			level.playSound(null, blockPosition(), FmabSounds.SEAL_ERASE, SoundSource.HOSTILE, 2, 1);
			player.sendOverlayMessage(Component.translatable("entity.fmab.haunted_armor.released"));
			released = true;
			hurtServer(level, damageSources().playerAttack(player), Float.MAX_VALUE);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!(level() instanceof ServerLevel level) || !collapsed()) {
			return;
		}
		getNavigation().stop();
		setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
		if (tickCount % 10 == 0) {
			level.sendParticles(SEAL, getX(), getY(0.2), getZ(), 3, 0.2, 0.05, 0.2, 0);
		}
		if (++collapsedFor >= REFORM) {
			entityData.set(COLLAPSED, false);
			setHealth(getMaxHealth());
			level.playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.HOSTILE, 1.5f,
					0.6f);
			onReform(level);
		}
	}

	/** Ce qui se passe quand elle se reforme (Barry, lui, ricane). */
	protected void onReform(ServerLevel level) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("collapsed", collapsed());
		output.putInt("collapsed_for", collapsedFor);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(COLLAPSED, input.getBooleanOr("collapsed", false));
		collapsedFor = input.getIntOr("collapsed_for", 0);
	}
}
