package com.ajustor.fmab.entity;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.stone.Karma;
import com.ajustor.fmab.stone.Recruitment;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Lust (Luxure) : ses doigts s'allongent en lames et transpercent tout en ligne droite, boucliers
 * compris. Elle frappe de loin, après un bref temps d'arrêt. Le feu la brûle deux fois plus. Chez une
 * âme noire qui n'a pas encore répondu, elle vient d'abord porter l'offre de Père ({@link Recruitment}).
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
	/** Le joueur à qui elle porte l'offre de Père : tant qu'il n'a pas répondu, elle ne l'attaque pas. */
	private @Nullable UUID envoyTo;
	private int waited;

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
		if (envoyTo != null && level() instanceof ServerLevel level) {
			await(level);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (envoyTo != null) {
			output.putString("envoy", envoyTo.toString());
			output.putInt("waited", waited);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		envoyTo = input.getString("envoy").map(UUID::fromString).orElse(null);
		waited = input.getIntOr("waited", 0);
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
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
			@Override
			public boolean canUse() {
				return envoyTo == null && super.canUse();
			}
		});
	}

	/** Elle vient porter l'offre de Père à ce joueur, sans l'attaquer. */
	public void bringOffer(ServerPlayer player) {
		envoyTo = player.getUUID();
		waited = 0;
		setTarget(null);
		player.sendSystemMessage(Component.translatable("stone.fmab.offer"));
	}

	/** Attend-elle encore la réponse d'un joueur ? */
	public boolean envoy() {
		return envoyTo != null;
	}

	/**
	 * La réponse du joueur. Oui : un noyau de Pierre en acompte, et elle s'en va. Non : elle attaque,
	 * et les traques commencent.
	 */
	private void answer(ServerPlayer player, boolean accepted) {
		envoyTo = null;
		player.setAttached(FmabAttachments.RECRUITMENT, accepted ? Recruitment.ACCEPTED : Recruitment.REFUSED);
		Karma.add(player, accepted ? Recruitment.ACCEPT_KARMA : Recruitment.REFUSE_KARMA);
		ServerLevel level = player.level();
		if (accepted) {
			ItemStack advance = new ItemStack(FmabItems.PHILOSOPHER_STONE_CORE);
			if (!player.getInventory().add(advance)) {
				player.drop(advance, false);
			}
			player.sendSystemMessage(Component.translatable("stone.fmab.offer_accepted"));
			level.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_WOLOLO, SoundSource.HOSTILE, 1, 0.7f);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 30, 0.3, 0.6, 0.3, 0.02);
			discard();
		} else {
			player.sendSystemMessage(Component.translatable("stone.fmab.offer_refused"));
			level.playSound(null, blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.HOSTILE, 1, 0.8f);
			setTarget(player);
		}
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (envoyTo == null || !player.getUUID().equals(envoyTo)) {
			return super.mobInteract(player, hand);
		}
		if (player instanceof ServerPlayer p) {
			answer(p, true);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// La frapper, c'est répondre non.
		if (envoyTo != null && source.getEntity() instanceof ServerPlayer p && p.getUUID().equals(envoyTo)) {
			answer(p, false);
		}
		return super.hurtServer(level, source, damage);
	}

	/** Elle s'approche et attend ; partir loin, ou la faire trop attendre, c'est répondre non. */
	private void await(ServerLevel level) {
		Player player = level.getPlayerByUUID(envoyTo);
		double distance = player == null ? Double.MAX_VALUE : distanceTo(player);
		if (!Recruitment.stillWaiting(++waited, distance)) {
			if (player instanceof ServerPlayer p) {
				answer(p, false);
			} else {
				envoyTo = null;
			}
			return;
		}
		if (distance > 4) {
			if (waited % 20 == 1) {
				getNavigation().moveTo(player, 0.9);
			}
		} else {
			getNavigation().stop();
			getLookControl().setLookAt(player, 30, 30);
		}
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
