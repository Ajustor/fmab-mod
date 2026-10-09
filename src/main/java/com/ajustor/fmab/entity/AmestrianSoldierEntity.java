package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.stone.Karma;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Un soldat d'Amestris : uniforme bleu à Central, manteau blanc d'hiver à Briggs. Neutre : il
 * garde les lieux et taille en pièces les monstres qui s'en approchent (et, à Briggs, les soldats
 * de Drachma). Il se retourne contre un joueur au karma trop bas, ou recherché pour avoir levé la
 * main sur l'armée ; ses camarades accourent.
 */
public class AmestrianSoldierEntity extends PathfinderMob {
	/** En dessous, l'armée vous considère comme un criminel. */
	public static final int OUTLAW = -40;
	/** Durée de la traque après avoir frappé un soldat, en ticks. */
	private static final int WANTED = 20 * 60 * 5;

	public AmestrianSoldierEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		setDropChance(EquipmentSlot.MAINHAND, 0.05f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 26)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 5)
				.add(Attributes.ARMOR, 4)
				.add(Attributes.FOLLOW_RANGE, 24);
	}

	/** Un soldat de Briggs, en manteau d'hiver. */
	public boolean briggs() {
		return getType() == FmabEntities.BRIGGS_SOLDIER;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
				(target, level) -> target instanceof ServerPlayer p && outlaw(p)));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
				(target, level) -> !(target instanceof HomunculusEntity)));
	}

	/** Le joueur est-il un criminel aux yeux de l'armée ? */
	public static boolean outlaw(ServerPlayer player) {
		if (player.isCreative() || player.isSpectator()) {
			return false;
		}
		Long until = player.getAttached(FmabAttachments.WANTED);
		return Karma.of(player) < OUTLAW || until != null && until > player.level().getGameTime();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.getEntity() instanceof ServerPlayer p && !p.isCreative()) {
			// Lever la main sur l'armée : recherché, pour un temps.
			p.setAttached(FmabAttachments.WANTED, level.getGameTime() + WANTED);
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer p && getTarget() != p) {
			p.sendSystemMessage(Component.translatable(outlaw(p) ? "npc.fmab.soldier.halt"
					: briggs() ? "npc.fmab.soldier.briggs" : "npc.fmab.soldier.salute"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return !(target instanceof AmestrianSoldierEntity) && super.canAttack(target);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}
}
