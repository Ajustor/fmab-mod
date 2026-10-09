package com.ajustor.fmab.entity;

import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Greed (Avarice) : il veut tout. Son Bouclier ultime durcit sa peau en carbone : rien ne le
 * blesse, sauf quand il le retire pour frapper. Il ne cherche pas la bagarre ; on peut même
 * l'acheter (or, émeraudes, diamants) ou gagner son respect en le battant, et il devient un allié,
 * tant qu'on le paie.
 */
public class GreedEntity extends HomunculusEntity {
	private static final EntityDataAccessor<Boolean> SHIELD =
			SynchedEntityData.defineId(GreedEntity.class, EntityDataSerializers.BOOLEAN);
	private static final int SOULS = 5;
	/** Le bouclier se retire le temps de frapper. */
	private static final int OPENING = 50;
	/** Ce que vaut une offrande, en lingots d'or ; il en veut au moins tant pour un pacte. */
	private static final int PRICE = 32;
	/** Défaites après lesquelles il reconnaît son vainqueur. */
	private static final int RESPECT = 2;
	/** Il se fait payer tous les jours de jeu. */
	private static final int WAGE_PERIOD = 24000;

	private int opening;
	private UUID owner;
	private UUID respected;
	private int defeats;
	private long nextWage;

	public GreedEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level, SOULS);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 80)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 10)
				.add(Attributes.FOLLOW_RANGE, 24)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SHIELD, true);
	}

	public boolean shielded() {
		return entityData.get(SHIELD);
	}

	public Optional<UUID> owner() {
		return Optional.ofNullable(owner);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
		goalSelector.addGoal(4, new FollowOwnerGoal());
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7) {
			@Override
			public boolean canUse() {
				return owner == null && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new DefendOwnerGoal());
		targetSelector.addGoal(2, new HurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				// Il ne se retourne pas contre celui qui le paie.
				return super.canUse() && !(getLastHurtByMob() instanceof Player p && p.getUUID().equals(owner));
			}
		});
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		// Pour frapper, il retire son bouclier : c'est le moment de le toucher.
		openShield(level);
		return super.doHurtTarget(level, target);
	}

	private void openShield(ServerLevel level) {
		opening = OPENING;
		entityData.set(SHIELD, false);
		level.playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.HOSTILE, 1, 0.6f);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (shielded() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			// Le carbone durci : le coup glisse, mais Greed n'aime pas qu'on essaie.
			level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6f, 1.6f);
			level.sendParticles(ParticleTypes.CRIT, getX(), getY(0.6), getZ(), 8, 0.3, 0.4, 0.3, 0.1);
			if (source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player p
					&& p.getUUID().equals(owner))) {
				setTarget(attacker);
			}
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	protected void onReconstitute(ServerLevel level) {
		// Battu : il retient le nom de qui l'a mis à terre.
		if (getLastHurtByMob() instanceof ServerPlayer winner && owner == null) {
			if (!winner.getUUID().equals(respected)) {
				respected = winner.getUUID();
				defeats = 0;
			}
			if (++defeats >= RESPECT) {
				setTarget(null);
				winner.sendSystemMessage(Component.translatable("homunculus.fmab.greed_yields"));
			}
		}
		entityData.set(SHIELD, true);
	}

	@Override
	protected boolean showBossBar() {
		return owner == null && getTarget() != null;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server) || regenerating()) {
			return InteractionResult.SUCCESS;
		}
		if (player.getUUID().equals(owner)) {
			server.sendOverlayMessage(Component.translatable("homunculus.fmab.greed_ally"));
			return InteractionResult.SUCCESS;
		}
		if (owner != null) {
			server.sendOverlayMessage(Component.translatable("homunculus.fmab.greed_taken"));
			return InteractionResult.SUCCESS;
		}
		if (player.getUUID().equals(respected) && defeats >= RESPECT) {
			pact(server);
			return InteractionResult.SUCCESS;
		}
		ItemStack held = player.getItemInHand(hand);
		int value = value(held);
		if (value == 0) {
			server.sendSystemMessage(Component.translatable("homunculus.fmab.greed_wants", PRICE));
			return InteractionResult.SUCCESS;
		}
		int units = (int) Math.ceil((double) PRICE / value);
		if (held.getCount() < units && !player.isCreative()) {
			server.sendSystemMessage(Component.translatable("homunculus.fmab.greed_more", units));
			return InteractionResult.SUCCESS;
		}
		if (!player.isCreative()) {
			held.shrink(units);
		}
		pact(server);
		return InteractionResult.SUCCESS;
	}

	/** Ce que vaut un objet aux yeux de Greed, en lingots d'or. */
	private static int value(ItemStack stack) {
		if (stack.is(Items.GOLD_INGOT)) {
			return 1;
		}
		if (stack.is(Items.EMERALD)) {
			return 2;
		}
		if (stack.is(Items.DIAMOND)) {
			return 4;
		}
		if (stack.is(Items.GOLD_BLOCK)) {
			return 9;
		}
		return 0;
	}

	private void pact(ServerPlayer player) {
		owner = player.getUUID();
		nextWage = level().getGameTime() + WAGE_PERIOD;
		setTarget(null);
		player.sendSystemMessage(Component.translatable("homunculus.fmab.greed_pact"));
		level().playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1, 0.7f);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (opening > 0 && --opening == 0) {
			entityData.set(SHIELD, true);
		}
		if (owner != null && level.getGameTime() >= nextWage) {
			collectWage(level);
		}
	}

	/** Le salaire du jour : un lingot d'or, ou une émeraude ; sinon, le pacte est rompu. */
	private void collectWage(ServerLevel level) {
		ServerPlayer payer = level.getServer().getPlayerList().getPlayer(owner);
		if (payer == null) {
			// Il attendra le retour de son employeur.
			nextWage = level.getGameTime() + 1200;
			return;
		}
		for (ItemStack stack : payer.getInventory()) {
			if (stack.is(Items.GOLD_INGOT) || stack.is(Items.EMERALD)) {
				stack.shrink(1);
				nextWage = level.getGameTime() + WAGE_PERIOD;
				payer.sendOverlayMessage(Component.translatable("homunculus.fmab.greed_paid"));
				return;
			}
		}
		payer.sendSystemMessage(Component.translatable("homunculus.fmab.greed_leaves"));
		owner = null;
		defeats = 0;
		respected = null;
	}

	/** Il suit son employeur ; trop loin, il le rejoint d'un bond. */
	private class FollowOwnerGoal extends Goal {
		private Player target;

		FollowOwnerGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (owner == null || getTarget() != null) {
				return false;
			}
			Player p = level().getPlayerByUUID(owner);
			if (p == null || p.isSpectator() || distanceToSqr(p) < 36) {
				return false;
			}
			target = p;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return target != null && getTarget() == null && distanceToSqr(target) > 9;
		}

		@Override
		public void tick() {
			if (distanceToSqr(target) > 24 * 24) {
				Vec3 at = target.position().add(target.getLookAngle().scale(-2));
				teleportTo(at.x, target.getY(), at.z);
				getNavigation().stop();
				return;
			}
			getNavigation().moveTo(target, 1.2);
		}

		@Override
		public void stop() {
			target = null;
			getNavigation().stop();
		}
	}

	/** Il frappe ce qui frappe son employeur, et ce que son employeur frappe. */
	private class DefendOwnerGoal extends TargetGoal {
		private LivingEntity enemy;

		DefendOwnerGoal() {
			super(GreedEntity.this, false);
			setFlags(EnumSet.of(Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			if (owner == null) {
				return false;
			}
			Player p = level().getPlayerByUUID(owner);
			if (p == null) {
				return false;
			}
			LivingEntity candidate = p.getLastHurtByMob() != null ? p.getLastHurtByMob() : p.getLastHurtMob();
			if (candidate == null || candidate == GreedEntity.this || candidate == p) {
				return false;
			}
			enemy = candidate;
			return canAttack(enemy, TargetingConditions.DEFAULT);
		}

		@Override
		public void start() {
			mob.setTarget(enemy);
			super.start();
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (owner != null) {
			output.putString("owner", owner.toString());
		}
		if (respected != null) {
			output.putString("respected", respected.toString());
		}
		output.putInt("defeats", defeats);
		output.putLong("next_wage", nextWage);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		owner = input.getString("owner").map(UUID::fromString).orElse(null);
		respected = input.getString("respected").map(UUID::fromString).orElse(null);
		defeats = input.getIntOr("defeats", 0);
		nextWage = input.getLongOr("next_wage", 0);
	}
}
