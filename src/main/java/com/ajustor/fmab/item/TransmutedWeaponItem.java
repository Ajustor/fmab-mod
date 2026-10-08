package com.ajustor.fmab.item;

import com.ajustor.fmab.registry.FmabComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Arme tirée de la matière par transmutation (lance de pierre, lame-bras). Elle ne tient pas :
 * passé {@link FmabComponents#EXPIRES}, elle se défait et rend sa matière ({@link FmabComponents#REMAINS}).
 */
public class TransmutedWeaponItem extends Item {
	/** Durée de vie d'une arme transmutée : une minute. */
	public static final int LIFETIME_TICKS = 20 * 60;

	public TransmutedWeaponItem(Properties properties) {
		super(properties);
	}

	public static ItemStack create(Item item, ServerLevel level, ItemStack remains) {
		ItemStack stack = new ItemStack(item);
		stack.set(FmabComponents.EXPIRES, level.getGameTime() + LIFETIME_TICKS);
		stack.set(FmabComponents.REMAINS, remains.copy());
		return stack;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		Long expires = stack.get(FmabComponents.EXPIRES);
		if (expires == null || level.getGameTime() < expires) {
			return;
		}
		ItemStack remains = stack.getOrDefault(FmabComponents.REMAINS, ItemStack.EMPTY).copy();
		stack.setCount(0);
		level.playSound(null, owner.blockPosition(), SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1, 0.8f);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, owner.getX(), owner.getY() + 1, owner.getZ(), 10, 0.3,
				0.3, 0.3, 0.05);
		if (remains.isEmpty()) {
			return;
		}
		if (!(owner instanceof Player player) || !player.getInventory().add(remains)) {
			owner.spawnAtLocation(level, remains);
		}
	}
}
