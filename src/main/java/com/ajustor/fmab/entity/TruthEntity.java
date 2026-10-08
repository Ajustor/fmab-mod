package com.ajustor.fmab.entity;

import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.SoulBinding;
import com.ajustor.fmab.registry.FmabAttachments;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * La Vérité : une copie toute blanche de l'alchimiste qui lui fait face. Ce qu'elle lui prend, elle
 * le porte : un bras volé devient son bras, avec la peau de son ancien propriétaire. On ne la
 * combat pas ; une âme qui erre devant la Porte peut lui demander de rejoindre un de ses sceaux.
 */
public class TruthEntity extends PathfinderMob {
	private static final EntityDataAccessor<String> OWNER =
			SynchedEntityData.defineId(TruthEntity.class, EntityDataSerializers.STRING);
	/** Les parties volées, un bit par {@link BodyPart}. */
	private static final EntityDataAccessor<Integer> STOLEN =
			SynchedEntityData.defineId(TruthEntity.class, EntityDataSerializers.INT);

	public TruthEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 1000)
				.add(Attributes.MOVEMENT_SPEED, 0.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(OWNER, "");
		builder.define(STOLEN, 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 32, 1.0f));
	}

	/** La Vérité de cet alchimiste : sa silhouette, et ce qu'elle lui a déjà pris. */
	public void mirror(ServerPlayer owner, Set<BodyPart> stolen) {
		entityData.set(OWNER, owner.getUUID().toString());
		setStolen(stolen);
	}

	public void setStolen(Set<BodyPart> stolen) {
		int mask = 0;
		for (BodyPart part : stolen) {
			mask |= 1 << part.ordinal();
		}
		entityData.set(STOLEN, mask);
	}

	public Optional<UUID> owner() {
		String id = entityData.get(OWNER);
		return id.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(id));
	}

	public Set<BodyPart> stolen() {
		Set<BodyPart> out = EnumSet.noneOf(BodyPart.class);
		int mask = entityData.get(STOLEN);
		for (BodyPart part : BodyPart.values()) {
			if ((mask & (1 << part.ordinal())) != 0) {
				out.add(part);
			}
		}
		return out;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server && owner().filter(server.getUUID()::equals).isPresent()) {
			GateState gate = server.getAttachedOrCreate(FmabAttachments.GATE);
			if (gate.adrift()) {
				SoulBinding.recall(server);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return source.isCreativePlayer() && super.hurtServer(level, source, damage);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("owner", entityData.get(OWNER));
		output.putInt("stolen", entityData.get(STOLEN));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(OWNER, input.getStringOr("owner", ""));
		entityData.set(STOLEN, input.getIntOr("stolen", 0));
	}
}
