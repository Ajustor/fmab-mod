package com.ajustor.fmab.entity;

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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Barry le Boucher : un tueur en série dont l'âme a été scellée dans une armure, gardien du poste
 * en ruine au-dessus du Laboratoire 5. Le premier boss du chemin : une armure habitée plus grande,
 * plus rapide, un hachoir à la main, qui ricane en se reformant. Effacez son sceau pendant qu'il gît.
 */
public class BarryEntity extends HauntedArmorEntity {
	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(),
			BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
	private int tauntCooldown;

	public BarryEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return HauntedArmorEntity.createAttributes()
				.add(Attributes.MAX_HEALTH, 70)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 9)
				.add(Attributes.SCALE, 1.15);
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
		level.playSound(null, blockPosition(), SoundEvents.PILLAGER_CELEBRATE, SoundSource.HOSTILE, 1.5f, 0.6f);
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(24))) {
			p.sendSystemMessage(Component.translatable("entity.fmab.barry.reforms"));
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		bossEvent.setProgress(getHealth() / getMaxHealth());
		bossEvent.setVisible(getTarget() instanceof Player || collapsed());
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
}
