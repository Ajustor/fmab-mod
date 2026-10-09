package com.ajustor.fmab.entity;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.Gifts;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.registry.FmabSounds;
import com.ajustor.fmab.stone.Karma;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Scar, l'Ishvalien au bras tatoué, dans les ruines de son peuple. Il traque les Alchimistes d'État,
 * « le jugement de Dieu » : à leur vue, il attaque, et son bras décompose tout ce qu'il touche, la
 * chair comme la pierre. Aux autres, s'ils ont le cœur droit, il confie les notes de son frère.
 */
public class ScarEntity extends PathfinderMob {
	private static final float DECOMPOSE = 6;
	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(),
			BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
	private static final String GIFT = "scar_notes";

	public ScarEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 90)
				.add(Attributes.MOVEMENT_SPEED, 0.34)
				.add(Attributes.ATTACK_DAMAGE, 6)
				.add(Attributes.ARMOR, 4)
				.add(Attributes.FOLLOW_RANGE, 32);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.6));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
				(target, level) -> target instanceof ServerPlayer p && stateAlchemist(p)));
	}

	private static boolean stateAlchemist(ServerPlayer player) {
		return !player.isCreative() && !player.isSpectator()
				&& player.getAttachedOrCreate(FmabAttachments.ALCHEMIST).rank().atLeast(Rank.STATE);
	}

	@Override
	public void setTarget(LivingEntity target) {
		if (target instanceof ServerPlayer p && getTarget() != target && level() instanceof ServerLevel) {
			p.sendSystemMessage(Component.translatable("npc.fmab.scar.judgment"));
		}
		super.setTarget(target);
	}

	/** Chaque coup de son bras décompose : la chair, et la pierre sous les pieds de sa cible. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		swing(InteractionHand.MAIN_HAND);
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			living.invulnerableTime = 0;
			living.hurtServer(level, damageSources().indirectMagic(this, this), DECOMPOSE);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, living.getX(), living.getY(0.5), living.getZ(), 30, 0.3,
					0.5, 0.3, 0.15);
			BlockPos below = living.blockPosition().below();
			BlockState ground = level.getBlockState(below);
			if (!ground.isAir() && ground.getDestroySpeed(level, below) >= 0 && ground.getDestroySpeed(level, below) < 5) {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), below.getX() + 0.5,
						below.getY() + 1, below.getZ() + 0.5, 20, 0.4, 0.1, 0.4, 0.1);
				level.destroyBlock(below, false, this);
			}
			level.playSound(null, target.blockPosition(), FmabSounds.DECOMPOSE, SoundSource.HOSTILE, 1,
					1.3f);
		}
		return hit;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer p) || getTarget() == p) {
			return InteractionResult.PASS;
		}
		if (stateAlchemist(p)) {
			setTarget(p);
			return InteractionResult.SUCCESS;
		}
		boolean gave = Gifts.received(p, GIFT);
		if (gave || Karma.of(p) < 0) {
			p.sendSystemMessage(Component.translatable(gave ? "npc.fmab.scar.silent" : "npc.fmab.scar.distrust"));
			return InteractionResult.SUCCESS;
		}
		Gifts.give(p, GIFT, new ItemStack(FmabItems.ISHVAL_TATTOO));
		p.sendSystemMessage(Component.translatable("npc.fmab.scar.gives"));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		// Le carnet tombe pour qui l'a vaincu sans l'avoir reçu.
		if (source.getEntity() instanceof ServerPlayer killer && !Gifts.received(killer, GIFT)) {
			Gifts.give(killer, GIFT, new ItemStack(FmabItems.ISHVAL_TATTOO));
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		bossEvent.setProgress(getHealth() / getMaxHealth());
		bossEvent.setVisible(getTarget() instanceof Player);
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		// Il ne quitte pas son lieu.
		if (!hasHome()) {
			setHomeTo(blockPosition(), 16);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

}
