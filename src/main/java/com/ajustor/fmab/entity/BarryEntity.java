package com.ajustor.fmab.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import com.ajustor.fmab.compat.EntityCompat;
import com.ajustor.fmab.compat.SoundCompat;

/**
 * Barry le Boucher : un tueur en série dont l'âme a été scellée dans une armure, gardien du poste
 * en ruine au-dessus du Laboratoire 5. Le premier boss du chemin ({@link Butcher}) : il lève son
 * hachoir avant de balayer tout l'arc devant lui ; blessé, il baisse la tête et fonce, et s'assomme
 * s'il finit dans un mur ; son sceau tient au premier effondrement, il faut l'abattre deux fois.
 */
public class BarryEntity extends HauntedArmorEntity {
	/** Le sol marqué là où le hachoir va passer. */
	private static final DustParticleOptions MARK = new DustParticleOptions(0xC02020, 1.3f);

	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(),
			BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
	private int tauntCooldown;
	/** Hachoir levé : ticks avant le coup. */
	private int windup;
	private float sweepYaw;
	private int sweepCooldown;
	/** Tête baissée : ticks avant la ruée. */
	private int rushWindup;
	private int rushing;
	private double rushX;
	private double rushZ;
	/** A-t-il déjà foncé depuis qu'il s'est relevé ? */
	private boolean rushed;
	private int stunned;

	public BarryEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return HauntedArmorEntity.createAttributes()
				.add(Attributes.MAX_HEALTH, 70)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 2)
				.add(Attributes.SCALE, 1.15);
	}

	/** Le hachoir est-il levé, prêt à balayer ? */
	public boolean windingUp() {
		return windup > 0;
	}

	@Override
	protected boolean busy() {
		return windup > 0 || rushWindup > 0 || rushing > 0 || stunned > 0;
	}

	@Override
	public boolean sealExposed() {
		return Butcher.sealExposed(collapses());
	}

	@Override
	protected int reformTime() {
		return Butcher.reformTicks(collapses(), REFORM);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		if (--tauntCooldown <= 0 && target instanceof ServerPlayer p) {
			tauntCooldown = 6;
			p.sendSystemMessage(Component.translatable("entity.fmab.barry.taunt" + (1 + random.nextInt(3))));
		}
		return super.doHurtTarget(level, target);
	}

	@Override
	protected void onReform(ServerLevel level) {
		rushed = false;
		level.playSound(null, blockPosition(), SoundEvents.PILLAGER_CELEBRATE, SoundSource.HOSTILE, 1.5f, 0.6f);
		tell(level, false, sealExposed() ? "entity.fmab.barry.reforms" : "entity.fmab.barry.enraged");
	}

	@Override
	public void aiStep() {
		super.aiStep();
		BossBars.update(bossEvent, this, BossBars.engaged(this) || collapsed());
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (collapsed()) {
			windup = rushWindup = rushing = stunned = 0;
		} else {
			fight(level);
		}
	}

	private void fight(ServerLevel level) {
		if (sweepCooldown > 0) {
			sweepCooldown--;
		}
		LivingEntity target = getTarget();
		if (stunned > 0) {
			stunned--;
			getNavigation().stop();
			if (tickCount % 5 == 0) {
				level.sendParticles(ParticleTypes.CRIT, getX(), getY(1.1), getZ(), 4, 0.3, 0.1, 0.3, 0);
			}
		} else if (windup > 0) {
			getNavigation().stop();
			face(sweepYaw);
			if (windup % 4 == 0) {
				markArc(level, MARK);
			}
			if (--windup == 0) {
				sweep(level);
			}
		} else if (rushWindup > 0) {
			getNavigation().stop();
			if (target != null) {
				getLookControl().setLookAt(target, 30, 30);
			}
			if (rushWindup % 5 == 0) {
				level.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY(1.1), getZ(), 1, 0.3, 0.2, 0.3, 0);
			}
			if (--rushWindup == 0) {
				launch(target);
			}
		} else if (rushing > 0) {
			charge(level);
		} else if (target != null && target.isAlive()) {
			double distance = distanceTo(target);
			if (Butcher.rushes(getHealth(), getMaxHealth(), rushed, distance)) {
				rushed = true;
				rushWindup = Butcher.RUSH_WINDUP;
				level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2f, 1.1f);
				tell(level, true, "entity.fmab.barry.rush");
			} else if (sweepCooldown == 0 && distance <= Butcher.SWEEP_REACH - 0.5) {
				windup = Butcher.SWEEP_WINDUP;
				sweepYaw = (float) Math.toDegrees(Math.atan2(-(target.getX() - getX()), target.getZ() - getZ()));
				face(sweepYaw);
				level.playSound(null, blockPosition(), SoundCompat.AXE_SCRAPE, SoundSource.HOSTILE, 1.5f, 0.5f);
				tell(level, true, "entity.fmab.barry.windup");
			}
		}
	}

	private void face(float yaw) {
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
	}

	/** Le hachoir balaie l'arc : tout ce qui s'y trouve encore est frappé et renversé. */
	private void sweep(ServerLevel level) {
		EntityCompat.swing(this, InteractionHand.MAIN_HAND, true);
		level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5f, 0.6f);
		markArc(level, ParticleTypes.SWEEP_ATTACK);
		float rad = sweepYaw * Mth.DEG_TO_RAD;
		for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(Butcher.SWEEP_REACH),
				e -> e != this && e.isAlive() && !(e instanceof HauntedArmorEntity))) {
			if (Butcher.inSweep(hit.getX() - getX(), hit.getZ() - getZ(), sweepYaw)
					&& hit.hurtServer(level, damageSources().mobAttack(this), Butcher.SWEEP_DAMAGE)) {
				throwBack(hit, -Mth.sin(rad), Mth.cos(rad), Butcher.SWEEP_KNOCKBACK);
			}
		}
		sweepCooldown = Butcher.SWEEP_COOLDOWN;
	}

	/** Renversé : projeté dans cette direction, et un peu en l'air. */
	private static void throwBack(LivingEntity hit, double x, double z, double strength) {
		double resist = 1 - hit.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		hit.push(x * strength * 0.5 * resist, 0.35 * resist, z * strength * 0.5 * resist);
		EntityCompat.syncVelocity(hit);
	}

	/** Une rangée de particules sur l'arc du hachoir. */
	private void markArc(ServerLevel level, ParticleOptions particle) {
		double reach = Butcher.SWEEP_REACH * 0.85;
		for (double a = -Butcher.SWEEP_HALF_ANGLE; a <= Butcher.SWEEP_HALF_ANGLE; a += 14) {
			double rad = Math.toRadians(sweepYaw + a);
			level.sendParticles(particle, getX() - Math.sin(rad) * reach, getY() + 0.15, getZ() + Math.cos(rad) * reach, 1,
					0, 0, 0, 0);
		}
	}

	/** Il part tout droit vers là où était sa cible. */
	private void launch(LivingEntity target) {
		double dx = target != null ? target.getX() - getX() : -Math.sin(getYRot() * Mth.DEG_TO_RAD);
		double dz = target != null ? target.getZ() - getZ() : Math.cos(getYRot() * Mth.DEG_TO_RAD);
		double length = Math.max(1.0e-3, Math.sqrt(dx * dx + dz * dz));
		rushX = dx / length;
		rushZ = dz / length;
		face((float) Math.toDegrees(Math.atan2(-rushX, rushZ)));
		rushing = Butcher.RUSH_TICKS;
	}

	private void charge(ServerLevel level) {
		rushing--;
		setDeltaMovement(rushX * Butcher.RUSH_SPEED, getDeltaMovement().y, rushZ * Butcher.RUSH_SPEED);
		for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.6),
				e -> e != this && e.isAlive() && !(e instanceof HauntedArmorEntity))) {
			if (hit.hurtServer(level, damageSources().mobAttack(this), Butcher.RUSH_DAMAGE)) {
				throwBack(hit, rushX, rushZ, Butcher.RUSH_KNOCKBACK);
			}
			rushing = 0;
		}
		if (rushing > 0 && horizontalCollision) {
			// Esquivé, droit dans le mur : il reste sonné.
			rushing = 0;
			stunned = Butcher.STUN;
			level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1, 0.7f);
			tell(level, true, "entity.fmab.barry.stunned");
		}
	}

	/** Prévient les joueurs alentour, dans la barre d'action ou le chat. */
	private void tell(ServerLevel level, boolean overlay, String key) {
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(24))) {
			if (overlay) {
				p.sendOverlayMessage(Component.translatable(key));
			} else {
				p.sendSystemMessage(Component.translatable(key));
			}
		}
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
