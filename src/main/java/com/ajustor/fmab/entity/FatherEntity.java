package com.ajustor.fmab.entity;

import com.ajustor.fmab.homunculus.AntiAlchemy;
import com.ajustor.fmab.network.CinematicPayload;
import com.ajustor.fmab.promised.NationalCircle;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.registry.FmabSounds;
import com.ajustor.fmab.stone.Eclipse;
import com.ajustor.fmab.stone.LivingStone;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Père, l'Homonculus originel, sur son trône sous Central. Le combat final, en trois formes :
 * <ol>
 * <li>le vieillard, assis : il transmute sans cercle, d'un geste, et la pierre du sol jaillit sous
 * ses ennemis ;</li>
 * <li>la forme sans visage : il se lève, et forge entre ses mains un petit soleil qu'il jette ;</li>
 * <li>la forme divine, instable : il a absorbé plus qu'il ne peut contenir ; il dévore les âmes
 * autour de lui pour tenir, et se défait sinon.</li>
 * </ol>
 * Tant qu'il vit, l'alchimie d'Amestris est captée autour de lui ({@link AntiAlchemy}) : seules
 * l'alkahestry de Xing et les armes ordinaires le touchent. Il traque les Pierres vivantes et se
 * nourrit de leurs âmes.
 */
public class FatherEntity extends HomunculusEntity {
	private static final EntityDataAccessor<Integer> PHASE =
			SynchedEntityData.defineId(FatherEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SUN =
			SynchedEntityData.defineId(FatherEntity.class, EntityDataSerializers.INT);
	/** Deux « morts » à encaisser : une par changement de forme. */
	private static final int FORMS = 2;
	private static final DustParticleOptions RED = new DustParticleOptions(0xD01020, 1.3f);
	private static final DustParticleOptions BLACK = new DustParticleOptions(0x080808, 1.8f);
	private static final double REACH = 32;
	/** La charge du petit soleil, en ticks. */
	public static final int SUN_CHARGE = 50;
	/** Ce qu'il perd chaque seconde sous sa forme divine, s'il ne mange pas. */
	private static final float UNSTABLE = 3;

	private int strikeCooldown = 40;
	private int sunCooldown = 80;
	private int devourCooldown = 60;
	private int huntCooldown;

	public FatherEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level, FORMS);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 260)
				.add(Attributes.MOVEMENT_SPEED, 0.27)
				.add(Attributes.ATTACK_DAMAGE, 10)
				.add(Attributes.FOLLOW_RANGE, 40)
				.add(Attributes.ARMOR, 8)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PHASE, 0);
		builder.define(SUN, 0);
	}

	/** 0 : le vieillard ; 1 : sans visage ; 2 : la forme divine. */
	public int phase() {
		return entityData.get(PHASE);
	}

	/** Le soleil qu'il forge, de 0 (rien) à {@link #SUN_CHARGE} (prêt à partir). */
	public int sun() {
		return entityData.get(SUN);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
			@Override
			public boolean canUse() {
				// Le vieillard ne quitte pas son trône.
				return phase() > 0 && sun() == 0 && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	protected boolean announcesReconstitution() {
		return false;
	}

	@Override
	protected void onReconstitute(ServerLevel level) {
		int next = Math.min(2, phase() + 1);
		entityData.set(PHASE, next);
		entityData.set(SUN, 0);
		if (next == 2) {
			getAttribute(Attributes.SCALE).setBaseValue(1.6);
			getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
			getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(16);
		}
		level.sendParticles(BLACK, getX(), getY(0.5), getZ(), 200, 1.2, 1.5, 1.2, 0);
		level.playSound(null, blockPosition(), next == 2 ? SoundEvents.WITHER_SPAWN : SoundEvents.WARDEN_EMERGE,
				SoundSource.HOSTILE, 2, 0.5f);
		say(level, "homunculus.fmab.father_phase" + next);
	}

	/** Une Pierre vivante ne puise pas en lui : c'est lui qui puise. */
	@Override
	public int takeSouls(int wanted) {
		return 0;
	}

	@Override
	protected float weakness(DamageSource source) {
		// Sa forme divine se fissure : elle encaisse mal.
		return phase() == 2 ? 1.25f : 1;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!(level() instanceof ServerLevel level) || regenerating()) {
			return;
		}
		if (phase() == 0) {
			// Assis sur son trône, il ne bouge pas.
			getNavigation().stop();
			setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
		}
		Player target = target(level);
		hunt(level);
		promisedDay(level);
		switch (phase()) {
			case 0 -> {
				if (target != null && --strikeCooldown <= 0) {
					erupt(level, target);
					strikeCooldown = 50;
				}
			}
			case 1 -> {
				if (target != null && --strikeCooldown <= 0) {
					erupt(level, target);
					strikeCooldown = 90;
				}
				forgeSun(level, target);
			}
			default -> {
				forgeSun(level, target);
				if (--devourCooldown <= 0) {
					devour(level);
					devourCooldown = 60;
				}
				// Il ne tient qu'en mangeant : chaque seconde, il se défait un peu.
				if (tickCount % 20 == 0) {
					setHealth(Math.max(1, getHealth() - UNSTABLE));
					level.sendParticles(RED, getX(), getY(0.6), getZ(), 15, 0.6, 1.2, 0.6, 0);
				}
			}
		}
	}

	private Player target(ServerLevel level) {
		if (getTarget() instanceof Player p && canReach(p, REACH)) {
			return p;
		}
		Player near = level.getNearestPlayer(this, REACH);
		if (near != null && !near.isCreative() && !near.isSpectator() && hasLineOfSight(near)) {
			setTarget(near);
			return near;
		}
		return null;
	}

	/** D'un geste, sans cercle : la pierre du sol jaillit sous la cible et la projette. */
	private void erupt(ServerLevel level, Player target) {
		swing(InteractionHand.MAIN_HAND);
		Vec3 at = target.position();
		BlockParticleOption stone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState());
		level.sendParticles(stone, at.x, at.y + 0.3, at.z, 60, 0.6, 0.6, 0.6, 0.2);
		level.sendParticles(RED, at.x, at.y + 0.2, at.z, 30, 0.8, 0.1, 0.8, 0);
		level.playSound(null, target.blockPosition(), SoundEvents.DRIPSTONE_BLOCK_BREAK, SoundSource.HOSTILE, 1.5f,
				0.6f);
		target.hurtServer(level, damageSources().indirectMagic(this, this), phase() == 0 ? 7 : 9);
		target.setDeltaMovement(target.getDeltaMovement().add(0, 1.0, 0));
		target.hurtMarked = true;
	}

	/** Le petit soleil : il le forge entre ses mains, puis le jette sur sa cible. */
	private void forgeSun(ServerLevel level, Player target) {
		int sun = sun();
		if (sun == 0) {
			if (target != null && --sunCooldown <= 0) {
				entityData.set(SUN, 1);
				getNavigation().stop();
				level.playSound(null, blockPosition(), FmabSounds.FATHER_SUN, SoundSource.HOSTILE, 2.5f, 1);
			}
			return;
		}
		getNavigation().stop();
		double size = 0.2 + sun / (double) SUN_CHARGE;
		Vec3 above = position().add(0, getBbHeight() + 0.8, 0);
		level.sendParticles(ParticleTypes.FLAME, above.x, above.y, above.z, 6, size * 0.4, size * 0.4, size * 0.4, 0.01);
		if (sun % 5 == 0) {
			level.sendParticles(ParticleTypes.LAVA, above.x, above.y, above.z, 1, size * 0.3, size * 0.3, size * 0.3, 0);
		}
		if (sun < SUN_CHARGE) {
			entityData.set(SUN, sun + 1);
			return;
		}
		entityData.set(SUN, 0);
		sunCooldown = phase() == 2 ? 100 : 140;
		if (target == null) {
			return;
		}
		// Le soleil file vers la cible, puis éclate.
		Vec3 to = target.position().add(0, 1, 0);
		Vec3 step = to.subtract(above);
		int points = (int) (step.length() * 2);
		for (int i = 0; i <= points; i++) {
			Vec3 p = above.add(step.scale((double) i / points));
			level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 3, 0.15, 0.15, 0.15, 0);
		}
		level.explode(this, to.x, to.y, to.z, phase() == 2 ? 3.5f : 2.5f, Level.ExplosionInteraction.NONE);
		target.igniteForSeconds(4);
	}

	/** La forme divine dévore les âmes autour d'elle pour tenir. */
	private void devour(ServerLevel level) {
		boolean fed = false;
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(14),
				p -> !p.isCreative() && !p.isSpectator())) {
			p.hurtServer(level, damageSources().indirectMagic(this, this), 5);
			p.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
			draw(level, p);
			fed = true;
		}
		if (fed) {
			heal(10);
			level.playSound(null, blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.5f, 0.5f);
		}
	}

	/** Le Jour promis : pendant l'éclipse, tant que le cercle national tient, il se régénère. */
	private void promisedDay(ServerLevel level) {
		if (tickCount % 20 != 0 || !Eclipse.now(level) || NationalCircle.get(level.getServer()).broken()) {
			return;
		}
		heal(4);
		level.sendParticles(RED, getX(), getY(0.5), getZ(), 20, 0.8, 1.2, 0.8, 0.02);
	}

	/** Les Pierres vivantes à sa portée : il leur arrache des âmes, et s'en nourrit. */
	private void hunt(ServerLevel level) {
		if (--huntCooldown > 0) {
			return;
		}
		huntCooldown = 40;
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(24))) {
			int souls = LivingStone.souls(p);
			if (souls <= 0 || p.isCreative()) {
				continue;
			}
			int taken = Math.min(souls, 2 + phase());
			p.setAttached(FmabAttachments.LIVING_STONE, souls - taken);
			heal(taken * 4);
			draw(level, p);
			p.sendOverlayMessage(Component.translatable("homunculus.fmab.father_drains", taken));
		}
	}

	/** Un filet rouge de l'âme arrachée jusqu'à lui. */
	private void draw(ServerLevel level, LivingEntity from) {
		Vec3 a = from.position().add(0, from.getBbHeight() * 0.6, 0);
		Vec3 b = position().add(0, getBbHeight() * 0.6, 0);
		Vec3 step = b.subtract(a);
		int points = (int) (step.length() * 2);
		for (int i = 0; i <= points; i++) {
			Vec3 p = a.add(step.scale((double) i / points));
			level.sendParticles(RED, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
		}
	}

	private void say(ServerLevel level, String key) {
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48))) {
			p.sendSystemMessage(Component.translatable(key));
		}
	}

	@Override
	public void die(DamageSource source) {
		if (level() instanceof ServerLevel level) {
			// La Vérité vient reprendre ce qui lui revient : des mains noires l'entraînent.
			level.sendParticles(BLACK, getX(), getY(0.5), getZ(), 400, 1.5, 2, 1.5, 0.02);
			level.playSound(null, blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 2, 0.5f);
			level.playSound(null, blockPosition(), FmabSounds.GATE_OPEN, SoundSource.HOSTILE, 3, 0.8f);
			say(level, "homunculus.fmab.father_falls");
			// Des bras noirs jaillissent du sol tout autour et se referment sur lui.
			for (int i = 0; i < 10; i++) {
				double a = Math.PI * 2 * i / 10;
				GateHandEntity.reach(level, position().add(Math.cos(a) * 3, 0, Math.sin(a) * 3), null, 60, 0);
			}
			for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48))) {
				CinematicPayload.play(p, CinematicPayload.FATHER_FALL, 90);
			}
		}
		super.die(source);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		// Ce qui reste de la Pierre d'un pays entier.
		spawnAtLocation(level, new ItemStack(FmabItems.PHILOSOPHER_STONE));
		spawnAtLocation(level, new ItemStack(FmabItems.PHILOSOPHER_STONE));
		spawnAtLocation(level, new ItemStack(FmabItems.PHILOSOPHER_STONE_CORE, 3));
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("phase", phase());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(PHASE, input.getIntOr("phase", 0));
		if (phase() == 2) {
			getAttribute(Attributes.SCALE).setBaseValue(1.6);
		}
	}
}
