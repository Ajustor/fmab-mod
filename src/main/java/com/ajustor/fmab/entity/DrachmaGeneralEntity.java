package com.ajustor.fmab.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Le général de Drachma, dans le donjon de son fort : plus grand, plus solide et plus dur à l'assaut
 * que ses soldats, il les commande de la voix quand on l'attaque.
 */
public class DrachmaGeneralEntity extends DrachmaSoldierEntity {
	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(),
			BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
	private boolean rallied;

	public DrachmaGeneralEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return DrachmaSoldierEntity.createAttributes()
				.add(Attributes.MAX_HEALTH, 110)
				.add(Attributes.ATTACK_DAMAGE, 6)
				.add(Attributes.ARMOR, 8)
				.add(Attributes.SCALE, 1.2);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity target = getTarget();
		if (target instanceof ServerPlayer player && !rallied) {
			// À la première attaque, il appelle sa garde.
			rallied = true;
			player.sendSystemMessage(Component.translatable("entity.fmab.drachma_general.rally"));
			level.getEntitiesOfClass(DrachmaSoldierEntity.class, getBoundingBox().inflate(24), s -> s != this)
					.forEach(s -> s.setTarget(player));
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		BossBars.update(bossEvent, this, BossBars.engaged(this));
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
