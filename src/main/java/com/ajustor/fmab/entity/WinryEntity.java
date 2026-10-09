package com.ajustor.fmab.entity;

import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.item.AutomailItem;
import com.ajustor.fmab.network.OpenWinryPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Winry Rockbell, mécanicienne d'automail : elle pose la pièce qu'on lui tend, répare (contre du
 * fer) et retire les automails. Elle travaille à Rush Valley et ne s'éloigne guère de son atelier.
 */
public class WinryEntity extends PathfinderMob {
	public WinryEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20)
				.add(Attributes.MOVEMENT_SPEED, 0.2);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.6));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			ItemStack held = player.getItemInHand(hand);
			if (!(held.getItem() instanceof AutomailItem)) {
				// Sans bras droit, on lui tend la pièce de la main gauche.
				held = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND
						: InteractionHand.MAIN_HAND);
			}
			if (held.getItem() instanceof AutomailItem) {
				Automails.fit(serverPlayer, held);
			} else {
				ServerPlayNetworking.send(serverPlayer, new OpenWinryPayload(getId()));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return source.isCreativePlayer() && super.hurtServer(level, source, damage);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		// Il ne quitte pas son lieu.
		if (!hasHome()) {
			setHomeTo(blockPosition(), 10);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}
}
