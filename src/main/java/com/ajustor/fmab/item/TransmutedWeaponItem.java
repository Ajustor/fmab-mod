package com.ajustor.fmab.item;

import com.ajustor.fmab.entity.StateExaminerEntity;
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

	/** À cette distance d'un examinateur, l'arme ne s'effrite pas. */
	private static final double EXAMINER_REACH = 16;
	/** Le sursis, renouvelé tant qu'on reste près de lui. */
	private static final int EXAMINER_GRACE = 20 * 10;

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
		// Sous les yeux d'un examinateur d'État, l'arme tient : on la lui remet sans course contre la
		// montre. Elle reprend son effritement une fois qu'on s'éloigne.
		if (!level.getEntitiesOfClass(StateExaminerEntity.class, owner.getBoundingBox().inflate(EXAMINER_REACH)).isEmpty()) {
			stack.set(FmabComponents.EXPIRES, level.getGameTime() + EXAMINER_GRACE);
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
