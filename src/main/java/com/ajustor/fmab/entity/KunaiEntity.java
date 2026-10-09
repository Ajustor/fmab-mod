package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.xing.Alkahestry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Un kunaï d'alkahestry lancé. Planté dans le sol, il est un point du cercle : cinq kunaï du même
 * lanceur qui entourent une zone y dessinent un cercle d'alkahestry ({@link Alkahestry}). On les
 * ramasse ensuite comme des flèches.
 */
public class KunaiEntity extends AbstractArrow {
	private static final EntityDataAccessor<Boolean> TRAP =
			SynchedEntityData.defineId(KunaiEntity.class, EntityDataSerializers.BOOLEAN);
	/** Ce kunaï a déjà servi à un cercle : il attend qu'on le ramasse. */
	private boolean spent;

	public KunaiEntity(EntityType<? extends KunaiEntity> type, Level level) {
		super(type, level);
	}

	public KunaiEntity(Level level, LivingEntity owner, ItemStack stack) {
		super(FmabEntities.KUNAI, owner, level, stack.copyWithCount(1), null);
		entityData.set(TRAP, Boolean.TRUE.equals(stack.get(FmabComponents.KUNAI_TRAP)));
		setBaseDamage(3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TRAP, false);
	}

	/** Vrai pour un kunaï de piège, faux pour un kunaï de soin. */
	public boolean trap() {
		return entityData.get(TRAP);
	}

	public boolean spent() {
		return spent;
	}

	public void spend() {
		spent = true;
	}

	/** Planté dans le sol, et pas encore pris dans un cercle. */
	public boolean planted() {
		return isInGround() && !spent;
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (getOwner() instanceof ServerPlayer thrower && !spent) {
			Alkahestry.landed(this, thrower);
		}
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(FmabItems.KUNAI);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("trap", trap());
		output.putBoolean("spent", spent);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(TRAP, input.getBooleanOr("trap", false));
		spent = input.getBooleanOr("spent", false);
	}
}
