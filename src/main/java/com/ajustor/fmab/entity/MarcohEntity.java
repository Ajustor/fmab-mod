package com.ajustor.fmab.entity;

import com.ajustor.fmab.item.CipheredNotesItem;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.level.Level;

/**
 * Le docteur Tim Marcoh, l'Alchimiste de cristal, caché dans un dispensaire de campagne. Il a fait
 * des Pierres pour l'armée, à Ishval, et ne s'en remet pas. Il soigne qui vient le voir, et lui seul
 * sait lire ses notes chiffrées (déguisées en recettes de cuisine) : il les rend en clair, en tome.
 */
public class MarcohEntity extends PathfinderMob {
	private static final int HEAL_COOLDOWN = 20 * 60;
	private long lastHeal = -HEAL_COOLDOWN;
	private int line;

	public MarcohEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20)
				.add(Attributes.MOVEMENT_SPEED, 0.22);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.4));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer p) || !(level() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		if (!hasHome()) {
			setHomeTo(blockPosition(), 8);
		}
		ItemStack held = p.getItemInHand(hand);
		if (held.is(FmabItems.CIPHERED_NOTES)) {
			// Il reconnaît son écriture : la recette de cuisine redevient un traité.
			ItemStack tome = CipheredNotesItem.decipher(held);
			held.consume(1, p);
			if (!p.getInventory().add(tome)) {
				p.drop(tome, false);
			}
			p.sendSystemMessage(Component.translatable("npc.fmab.marcoh.deciphers"));
			level.playSound(null, blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1, 1);
			return InteractionResult.SUCCESS;
		}
		long now = level.getGameTime();
		if (p.getHealth() < p.getMaxHealth() && now - lastHeal >= HEAL_COOLDOWN) {
			lastHeal = now;
			p.heal(p.getMaxHealth());
			level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY(1), p.getZ(), 6, 0.4, 0.3, 0.4, 0);
			p.sendSystemMessage(Component.translatable("npc.fmab.marcoh.heals"));
			return InteractionResult.SUCCESS;
		}
		line = line % 3 + 1;
		p.sendSystemMessage(Component.translatable("npc.fmab.marcoh.line" + line));
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return (source.isCreativePlayer() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
				&& super.hurtServer(level, source, damage);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}
}
