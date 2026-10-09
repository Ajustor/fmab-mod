package com.ajustor.fmab.entity;

import com.ajustor.fmab.item.BriggsSabreItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashSet;
import java.util.Set;

/**
 * Un homonculus : un être artificiel dont le cœur est une Pierre philosophale. Tant que la Pierre
 * contient des âmes, chaque mort n'est qu'un contretemps : il se reconstitue sur place, et la Pierre
 * s'épuise. Le nombre d'âmes est caché ; la reconstitution le laisse deviner, de plus en plus pâle.
 * À la dernière, il meurt pour de bon et laisse son noyau de Pierre.
 */
public abstract class HomunculusEntity extends Monster {
	/** Durée d'une reconstitution, en ticks : il ne bouge ni ne craint rien pendant ce temps. */
	private static final int REGENERATION = 60;
	private static final DustParticleOptions FLESH = new DustParticleOptions(0xB0101A, 1.2f);

	private final ServerBossEvent bossEvent;
	private int souls;
	private int regenerating;

	protected HomunculusEntity(EntityType<? extends Monster> type, Level level, int souls) {
		super(type, level);
		this.souls = souls;
		this.bossEvent = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(),
				BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
		setPersistenceRequired();
	}

	/** La barre de boss se montre-t-elle ? Un homonculus déguisé ne s'annonce pas. */
	protected boolean showBossBar() {
		return true;
	}

	/** Multiplicateur des dégâts subis : la faiblesse de l'homonculus. */
	protected float weakness(DamageSource source) {
		return 1;
	}

	public boolean regenerating() {
		return regenerating > 0;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (regenerating() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		float dealt = damage * weakness(source);
		ItemStack weapon = source.getWeaponItem();
		if (weapon != null && weapon.getItem() instanceof BriggsSabreItem) {
			dealt *= BriggsSabreItem.HOMUNCULUS_BONUS;
		}
		if (dealt >= getHealth() && souls > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			reconstitute(level);
			return true;
		}
		return super.hurtServer(level, source, dealt);
	}

	/** La mort n'est qu'un contretemps : la Pierre paie, et la chair se reforme. */
	private void reconstitute(ServerLevel level) {
		souls--;
		regenerating = REGENERATION;
		setHealth(getMaxHealth());
		setTarget(null);
		getNavigation().stop();
		level.playSound(null, blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.HOSTILE, 1, 0.6f);
		if (announcesReconstitution()) {
			Component name = getDisplayName();
			for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32))) {
				p.sendSystemMessage(Component.translatable("homunculus.fmab.reconstitutes", name));
			}
		}
		onReconstitute(level);
	}

	/** Le message ordinaire de reconstitution ; Père annonce lui-même ses métamorphoses. */
	protected boolean announcesReconstitution() {
		return true;
	}

	/** Ce qui change quand l'homonculus se reconstitue (Envy change de forme, par exemple). */
	protected void onReconstitute(ServerLevel level) {
	}

	/** Les âmes qui restent dans la Pierre. */
	public int souls() {
		return souls;
	}

	/** Une Pierre vivante lui arrache des âmes ; il en rend au plus ce qu'il a. */
	public int takeSouls(int wanted) {
		int taken = Math.min(wanted, souls);
		souls -= taken;
		return taken;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (regenerating > 0) {
			regenerating--;
			getNavigation().stop();
			setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
			// La chair rouge et les éclairs de la Pierre ; d'autant plus pâles que la Pierre s'épuise.
			int intensity = 2 + Math.min(souls, 8);
			level.sendParticles(FLESH, getX(), getY(0.5), getZ(), intensity, getBbWidth() * 0.4, getBbHeight() * 0.4,
					getBbWidth() * 0.4, 0);
			if (tickCount % 3 == 0) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.5), getZ(), intensity / 2,
						getBbWidth() * 0.5, getBbHeight() * 0.5, getBbWidth() * 0.5, 0.05);
			}
		}
		bossEvent.setProgress(getHealth() / getMaxHealth());
		bossEvent.setVisible(showBossBar());
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		spawnAtLocation(level, new ItemStack(FmabItems.PHILOSOPHER_STONE_CORE));
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			Component name = getDisplayName();
			String slain = BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();
			// Tous ceux qui étaient là l'ont vu tomber, et son tueur aussi, même de loin (un fusil, un arc) :
			// le sceau de Père les reconnaîtra.
			Set<ServerPlayer> witnesses = new HashSet<>(level.getEntitiesOfClass(ServerPlayer.class,
					getBoundingBox().inflate(48)));
			if (source.getEntity() instanceof ServerPlayer killer) {
				witnesses.add(killer);
			}
			if (getLastHurtByPlayer() instanceof ServerPlayer last) {
				witnesses.add(last);
			}
			for (ServerPlayer p : witnesses) {
				p.sendSystemMessage(Component.translatable("homunculus.fmab.destroyed", name));
				Set<String> seen = new HashSet<>(p.getAttachedOrCreate(FmabAttachments.SLAIN));
				seen.add(slain);
				p.setAttached(FmabAttachments.SLAIN, Set.copyOf(seen));
			}
		}
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
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("souls", souls);
		output.putInt("regenerating", regenerating);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		souls = input.getIntOr("souls", souls);
		regenerating = input.getIntOr("regenerating", 0);
		bossEvent.setName(getDisplayName());
	}

	/** Le joueur visé est-il à portée de vue et de coup ? */
	protected boolean canReach(Player target, double range) {
		return target.isAlive() && !target.isCreative() && !target.isSpectator() && distanceTo(target) <= range
				&& hasLineOfSight(target);
	}
}
