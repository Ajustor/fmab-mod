package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Les frères Slicer, gardiens du Laboratoire 5 : deux âmes de tueurs dans une seule armure, l'aîné
 * dans le corps, le cadet dans le heaume. Effacer le sceau de l'aîné ne suffit pas : le cadet reprend
 * aussitôt l'armure, plus vif et moins fort. Il faut l'abattre encore et effacer son sceau à lui.
 */
public class SlicerEntity extends HauntedArmorEntity {
	private static final DustParticleOptions SEAL = new DustParticleOptions(0xA01020, 1.2f);

	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(),
			BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);
	/** L'aîné tient encore l'armure. */
	private boolean elder = true;
	/** Le sceau de l'aîné vient d'être effacé : le cadet se relève tout de suite. */
	private boolean handover;

	public SlicerEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return HauntedArmorEntity.createAttributes()
				.add(Attributes.MAX_HEALTH, 60)
				.add(Attributes.ATTACK_DAMAGE, 6)
				.add(Attributes.SCALE, 1.2);
	}

	/** L'aîné tient-il encore l'armure ? */
	public boolean elder() {
		return elder;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!collapsed() || !elder || !(level() instanceof ServerLevel level)) {
			return super.mobInteract(player, hand);
		}
		// Le sceau du corps s'efface ; celui du heaume tient, et le cadet se relève.
		elder = false;
		handover = true;
		level.sendParticles(SEAL, getX(), getY(0.5), getZ(), 40, 0.4, 0.4, 0.4, 0);
		level.playSound(null, blockPosition(), FmabSounds.SEAL_ERASE, SoundSource.HOSTILE, 2, 0.8f);
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(16))) {
			p.sendSystemMessage(Component.translatable("entity.fmab.slicer.younger"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected int reformTime() {
		return handover ? 20 : super.reformTime();
	}

	@Override
	protected void onReform(ServerLevel level) {
		if (handover) {
			handover = false;
			// Le cadet : plus vif, moins fort.
			getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32);
			getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		BossBars.update(bossEvent, this, BossBars.engaged(this) || collapsed());
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("elder", elder);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		elder = input.getBooleanOr("elder", true);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}
}
