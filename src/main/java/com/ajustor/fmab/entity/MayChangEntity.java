package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.stone.Karma;
import com.ajustor.fmab.xing.Alkahestry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * May Chang, alkahestriste du clan Chang, dans son pavillon de Xing. Elle se méfie de qui sent le
 * sang et la Pierre ; aux autres, elle enseigne l'alkahestry et confie ses kunaï. Elle soigne les
 * blessés qui viennent la voir, et vend des kunaï contre des émeraudes.
 */
public class MayChangEntity extends PathfinderMob {
	/** Karma minimal pour qu'elle vous fasse confiance. */
	public static final int TRUST = 10;
	private static final int TEACHING_KUNAI = 8;
	private static final int KUNAI_PER_EMERALD = 4;
	private static final int HEAL_COOLDOWN = 20 * 60;

	private long lastHeal = -HEAL_COOLDOWN;

	public MayChangEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.SCALE, 0.85);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer p) || !(level() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		if (Karma.of(p) < TRUST && !p.isCreative()) {
			p.sendSystemMessage(Component.translatable("npc.fmab.may.distrust"));
			return InteractionResult.SUCCESS;
		}
		if (!Alkahestry.knows(p)) {
			p.setAttached(FmabAttachments.ALKAHESTRY, true);
			give(p, new ItemStack(FmabItems.KUNAI, TEACHING_KUNAI));
			p.sendSystemMessage(Component.translatable("npc.fmab.may.teaches"));
			level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5f, 1.2f);
			return InteractionResult.SUCCESS;
		}
		ItemStack held = p.getItemInHand(hand);
		if (held.is(Items.EMERALD)) {
			held.consume(1, p);
			give(p, new ItemStack(FmabItems.KUNAI, KUNAI_PER_EMERALD));
			p.sendSystemMessage(Component.translatable("npc.fmab.may.sells", KUNAI_PER_EMERALD));
			return InteractionResult.SUCCESS;
		}
		long now = level.getGameTime();
		if (p.getHealth() < p.getMaxHealth() && now - lastHeal >= HEAL_COOLDOWN) {
			lastHeal = now;
			p.heal(p.getMaxHealth());
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY(0.6), p.getZ(), 20, 0.4, 0.6, 0.4, 0);
			p.sendSystemMessage(Component.translatable("npc.fmab.may.heals"));
			return InteractionResult.SUCCESS;
		}
		p.sendSystemMessage(Component.translatable("npc.fmab.may.hint"));
		return InteractionResult.SUCCESS;
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return source.isCreativePlayer() && super.hurtServer(level, source, damage);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}
}
