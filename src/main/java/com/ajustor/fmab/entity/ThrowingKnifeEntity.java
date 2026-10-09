package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Un couteau de lancer de Maes Hughes, en vol ou planté : on le ramasse comme une flèche. */
public class ThrowingKnifeEntity extends AbstractArrow {
	public ThrowingKnifeEntity(EntityType<? extends ThrowingKnifeEntity> type, Level level) {
		super(type, level);
	}

	public ThrowingKnifeEntity(Level level, LivingEntity owner, ItemStack stack) {
		super(FmabEntities.THROWING_KNIFE, owner, level, stack.copyWithCount(1), null);
		setBaseDamage(2.5);
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(FmabItems.THROWING_KNIFE);
	}
}
