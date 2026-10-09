package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.stone.Karma;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * La générale Olivier Mira Armstrong, qui tient Fort Briggs. Elle taille en pièces les soldats de
 * Drachma et quiconque l'attaque. Elle méprise les faibles et les tricheurs : à qui a bon karma,
 * elle reconnaît le statut d'allié de Briggs et confie un sabre de Briggs.
 */
public class OlivierEntity extends PathfinderMob {
	/** Karma minimal pour être reconnu allié de Briggs. */
	public static final int ALLY = 20;

	public OlivierEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FmabItems.BRIGGS_SABRE));
		setDropChance(EquipmentSlot.MAINHAND, 0);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 80)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 10)
				.add(Attributes.ARMOR, 8)
				.add(Attributes.FOLLOW_RANGE, 24);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, DrachmaSoldierEntity.class, false));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer p) || !(level() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		if (getTarget() == p) {
			return InteractionResult.PASS;
		}
		int karma = Karma.of(p);
		if (karma < 0 && !p.isCreative()) {
			p.sendSystemMessage(Component.translatable("npc.fmab.olivier.contempt"));
			return InteractionResult.SUCCESS;
		}
		boolean ally = Boolean.TRUE.equals(p.getAttached(FmabAttachments.BRIGGS_ALLY));
		if (!ally && (karma >= ALLY || p.isCreative())) {
			p.setAttached(FmabAttachments.BRIGGS_ALLY, true);
			ItemStack sabre = new ItemStack(FmabItems.BRIGGS_SABRE);
			if (!p.getInventory().add(sabre)) {
				p.drop(sabre, false);
			}
			level.playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1, 0.8f);
			p.sendSystemMessage(Component.translatable("npc.fmab.olivier.ally"));
			return InteractionResult.SUCCESS;
		}
		p.sendSystemMessage(Component.translatable(ally ? "npc.fmab.olivier.orders" : "npc.fmab.olivier.prove"));
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}
}
