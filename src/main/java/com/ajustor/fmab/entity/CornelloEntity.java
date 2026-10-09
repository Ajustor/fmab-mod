package com.ajustor.fmab.entity;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Le père Cornello, faux prophète de Leto, dans son temple de Liore. Ses « miracles » sont des
 * transmutations faites grâce à une pierre rouge impure. Un Alchimiste qui le confronte voit clair
 * dans son jeu : démasqué, il lâche sa chimère et s'enfuit ; abattu, il laisse sa pierre.
 */
public class CornelloEntity extends PathfinderMob {
	private static final EntityDataAccessor<Boolean> EXPOSED =
			SynchedEntityData.defineId(CornelloEntity.class, EntityDataSerializers.BOOLEAN);
	private static final DustParticleOptions RED = new DustParticleOptions(0xC02030, 1.0f);
	private int sermon;

	public CornelloEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30)
				.add(Attributes.MOVEMENT_SPEED, 0.28);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(EXPOSED, false);
	}

	public boolean exposed() {
		return entityData.get(EXPOSED);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 10, 1.0, 1.3) {
			@Override
			public boolean canUse() {
				return exposed() && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer p) || !(level() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		if (exposed()) {
			p.sendSystemMessage(Component.translatable("npc.fmab.cornello.cornered"));
			return InteractionResult.SUCCESS;
		}
		if (!p.getAttachedOrCreate(FmabAttachments.ALCHEMIST).rank().atLeast(Rank.ALCHEMIST)) {
			// Un « miracle » : une fleur de pierre, et les fidèles applaudissent.
			level.sendParticles(RED, getX(), getY(1.1), getZ(), 20, 0.3, 0.3, 0.3, 0);
			p.sendSystemMessage(Component.translatable("npc.fmab.cornello.miracle"));
			return InteractionResult.SUCCESS;
		}
		expose(level, p);
		return InteractionResult.SUCCESS;
	}

	/** L'alchimiste voit clair : la pierre est fausse, et Cornello lâche sa chimère. */
	private void expose(ServerLevel level, ServerPlayer by) {
		entityData.set(EXPOSED, true);
		by.sendSystemMessage(Component.translatable("npc.fmab.cornello.exposed"));
		level.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5f, 0.8f);
		ChimeraBeastEntity chimera = FmabEntities.CHIMERA_BEAST.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (chimera != null) {
			chimera.snapTo(getX() + 1, getY(), getZ() + 1, getYRot(), 0);
			chimera.setTarget(by);
			chimera.setPersistenceRequired();
			level.addFreshEntity(chimera);
			level.sendParticles(RED, chimera.getX(), chimera.getY(0.5), chimera.getZ(), 60, 0.6, 0.6, 0.6, 0);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (!exposed() && source.getEntity() instanceof ServerPlayer p) {
			expose(level, p);
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		spawnAtLocation(level, new ItemStack(FmabItems.RED_STONE_SHARD));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level() instanceof ServerLevel level && !exposed() && ++sermon >= 20 * 40) {
			sermon = 0;
			Player near = level.getNearestPlayer(this, 10);
			if (near instanceof ServerPlayer p) {
				p.sendSystemMessage(Component.translatable("npc.fmab.cornello.sermon"));
			}
		}
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("exposed", exposed());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(EXPOSED, input.getBooleanOr("exposed", false));
	}
}
