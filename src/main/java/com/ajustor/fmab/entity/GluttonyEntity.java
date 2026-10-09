package com.ajustor.fmab.entity;

import com.ajustor.fmab.homunculus.Belly;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Gluttony (Gourmandise) : lent, peu malin, toujours affamé. Il aspire tout ce qui traîne autour de
 * lui (objets, bêtes, joueurs) et dévore ce qui arrive à sa bouche ; un joueur trop proche est
 * avalé et se retrouve dans son Ventre (voir {@link Belly}).
 */
public class GluttonyEntity extends HomunculusEntity {
	private static final int SOULS = 8;
	private static final double INHALE_RANGE = 12;

	public GluttonyEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level, SOULS);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 120)
				.add(Attributes.MOVEMENT_SPEED, 0.18)
				.add(Attributes.ATTACK_DAMAGE, 8)
				.add(Attributes.FOLLOW_RANGE, 24)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
				.add(Attributes.SCALE, 1.35);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new InhaleGoal());
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			// Une bouchée : ce qu'il mord le nourrit.
			heal(3);
		}
		return hit;
	}

	/** Gluttony ouvre grand la bouche et aspire tout ce qui se trouve autour de lui. */
	private class InhaleGoal extends Goal {
		private static final int DURATION = 60;
		private static final int COOLDOWN = 160;
		private int left;
		private int cooldown = 40;

		InhaleGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (cooldown > 0) {
				cooldown--;
				return false;
			}
			return !regenerating() && getTarget() instanceof Player p && canReach(p, INHALE_RANGE);
		}

		@Override
		public boolean canContinueToUse() {
			return left > 0 && !regenerating();
		}

		@Override
		public void start() {
			left = DURATION;
			getNavigation().stop();
			if (level() instanceof ServerLevel level) {
				level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 0.5f);
			}
		}

		@Override
		public void stop() {
			cooldown = COOLDOWN;
		}

		@Override
		public void tick() {
			left--;
			if (!(level() instanceof ServerLevel level)) {
				return;
			}
			if (getTarget() != null) {
				getLookControl().setLookAt(getTarget(), 30, 30);
			}
			inhale(level);
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}
	}

	private void inhale(ServerLevel level) {
		Vec3 mouth = getEyePosition().subtract(0, 0.3, 0);
		for (Entity e : level.getEntities(this, getBoundingBox().inflate(INHALE_RANGE), e -> e.isAlive()
				&& !(e instanceof HomunculusEntity))) {
			Vec3 toward = mouth.subtract(e.position());
			double distance = toward.length();
			if (distance > INHALE_RANGE) {
				continue;
			}
			if (distance < 2) {
				devour(level, e);
				continue;
			}
			double pull = e instanceof Player ? 0.09 : 0.18;
			e.push(toward.normalize().scale(pull));
			e.hurtMarked = true;
		}
		for (int i = 0; i < 6; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = 2 + random.nextDouble() * (INHALE_RANGE - 2);
			Vec3 p = mouth.add(Math.cos(a) * r, random.nextDouble() * 2 - 1, Math.sin(a) * r);
			Vec3 v = mouth.subtract(p).scale(0.08);
			level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 0, v.x, v.y, v.z, 1);
		}
	}

	/** Ce qui arrive à sa bouche disparaît : les objets, les bêtes ; les joueurs, dans son Ventre. */
	private void devour(ServerLevel level, Entity e) {
		if (e instanceof ServerPlayer player) {
			if (!player.isCreative() && !player.isSpectator()) {
				Belly.swallow(player, this);
			}
			return;
		}
		if (e instanceof ItemEntity item) {
			item.discard();
			heal(1);
		} else if (e instanceof LivingEntity living && !(e instanceof Player)) {
			living.kill(level);
			heal(4);
		} else {
			return;
		}
		level.playSound(null, blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.HOSTILE, 1, 0.7f);
	}
}
