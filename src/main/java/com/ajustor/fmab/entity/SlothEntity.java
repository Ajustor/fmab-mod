package com.ajustor.fmab.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Sloth (Paresse) : le plus fort et le plus rapide des homonculus, et le plus paresseux. Il creuse
 * le grand tunnel sous Central. Provoqué, il se rue en ligne droite à une vitesse folle, renversant
 * tout et défonçant la roche ; puis il s'arrête, épuisé (« quelle corvée… ») : c'est le moment de
 * frapper.
 */
public class SlothEntity extends HomunculusEntity {
	private static final int SOULS = 5;
	private static final double CHARGE_SPEED = 1.5;
	private static final int CHARGE_LENGTH = 26;
	private static final float CHARGE_DAMAGE = 18;
	/** Après une charge, il souffle ; il encaisse alors davantage. */
	private static final int REST = 90;

	private int resting;

	public SlothEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level, SOULS);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 250)
				.add(Attributes.MOVEMENT_SPEED, 0.22)
				.add(Attributes.ATTACK_DAMAGE, 16)
				.add(Attributes.ATTACK_KNOCKBACK, 2.5)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1)
				.add(Attributes.FOLLOW_RANGE, 32)
				.add(Attributes.SCALE, 2.2);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new RestGoal());
		goalSelector.addGoal(2, new ChargeGoal());
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.9, false));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public boolean resting() {
		return resting > 0;
	}

	@Override
	protected float weakness(DamageSource source) {
		return resting() ? 1.5f : 1;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (resting > 0) {
			resting--;
			if (resting % 20 == 0 && level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.CLOUD, getX(), getEyeY(), getZ(), 3, 0.3, 0.2, 0.3, 0.01);
			}
		}
	}

	/** Épuisé, il ne bouge plus. */
	private class RestGoal extends Goal {
		RestGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return resting();
		}

		@Override
		public void tick() {
			getNavigation().stop();
		}
	}

	/** La charge : il se prépare en soupirant, puis file droit devant lui. */
	private class ChargeGoal extends Goal {
		private static final int WINDUP = 25;
		private static final int COOLDOWN = 60;
		private int windup;
		private int travelled;
		private int cooldown = 40;
		private Vec3 direction;
		private final Set<LivingEntity> hit = new HashSet<>();

		ChargeGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (cooldown > 0) {
				cooldown--;
				return false;
			}
			return !regenerating() && !resting() && getTarget() instanceof Player p && canReach(p, 26)
					&& distanceTo(p) > 4;
		}

		@Override
		public boolean canContinueToUse() {
			return !regenerating() && (windup > 0 || travelled < CHARGE_LENGTH);
		}

		@Override
		public void start() {
			windup = WINDUP;
			travelled = 0;
			hit.clear();
			getNavigation().stop();
			if (level() instanceof ServerLevel level) {
				level.playSound(null, blockPosition(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 1.5f, 0.5f);
				for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(24))) {
					p.sendOverlayMessage(Component.translatable("homunculus.fmab.sloth_sighs"));
				}
			}
		}

		@Override
		public void tick() {
			if (!(level() instanceof ServerLevel level)) {
				return;
			}
			if (windup > 0) {
				if (getTarget() != null) {
					getLookControl().setLookAt(getTarget(), 30, 30);
					Vec3 to = getTarget().position().subtract(position());
					direction = new Vec3(to.x, 0, to.z).normalize();
				}
				windup--;
				return;
			}
			if (direction == null) {
				travelled = CHARGE_LENGTH;
				return;
			}
			setDeltaMovement(direction.x * CHARGE_SPEED, getDeltaMovement().y, direction.z * CHARGE_SPEED);
			travelled += 1;
			trample(level);
			if (dig(level) || horizontalCollision && travelled > 3) {
				travelled = CHARGE_LENGTH;
			}
		}

		@Override
		public void stop() {
			resting = REST;
			cooldown = COOLDOWN;
			direction = null;
			setDeltaMovement(Vec3.ZERO);
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		/** Ce qui se trouve sur son passage est projeté. */
		private void trample(ServerLevel level) {
			AABB front = getBoundingBox().inflate(0.6);
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, front,
					e -> e != SlothEntity.this && e.isAlive() && !(e instanceof HomunculusEntity))) {
				if (hit.add(e)) {
					e.hurtServer(level, damageSources().mobAttack(SlothEntity.this), CHARGE_DAMAGE);
					e.push(direction.x * 2.5, 0.8, direction.z * 2.5);
					e.hurtMarked = true;
				}
			}
		}

		/**
		 * Il creuse en fonçant : la roche devant lui vole en éclats (si les créatures ont le droit de
		 * casser des blocs). Le socle, l'obsidienne et les blocs incassables l'arrêtent net.
		 *
		 * @return vrai s'il a heurté un mur qu'il ne peut pas briser
		 */
		private boolean dig(ServerLevel level) {
			boolean griefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
			BlockPos front = BlockPos.containing(position().add(direction.scale(getBbWidth() * 0.6 + 0.6)));
			boolean stopped = false;
			for (int dy = 0; dy < Math.ceil(getBbHeight()); dy++) {
				for (int side = -1; side <= 1; side++) {
					BlockPos p = front.offset((int) Math.round(-direction.z * side), dy,
							(int) Math.round(direction.x * side));
					BlockState state = level.getBlockState(p);
					if (state.isAir() || state.getCollisionShape(level, p).isEmpty()) {
						continue;
					}
					float hardness = state.getDestroySpeed(level, p);
					// Le puits d'accès (échelle et pilier de briques) tient : on ne reste pas coincé en bas.
					boolean shaft = state.is(Blocks.LADDER) || state.is(Blocks.STONE_BRICKS);
					if (!griefing || shaft || hardness < 0 || hardness >= 50) {
						stopped = true;
						continue;
					}
					level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.getX() + 0.5,
							p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.1);
					level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
				}
			}
			if (stopped) {
				level.playSound(null, front, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 0.7f);
			}
			return stopped;
		}
	}
}
