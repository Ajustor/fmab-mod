package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.transmutation.GreatCircle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Un cercle géant qui s'allume (voir {@link GreatCircle}) : quelques secondes où le tracé du cercle
 * central, agrandi jusqu'aux relais, luit sur le sol, et où une colonne de lumière monte de chaque relais. Comme une décharge, il ne fait rien et ne se sauvegarde pas ; il
 * porte au client la position des relais, relative au centre.
 */
public class GreatCircleEntity extends Entity {
	/** Le temps de l'allumage, en ticks. */
	public static final int LIFETIME = 80;

	private static final EntityDataAccessor<Float> RADIUS =
			SynchedEntityData.defineId(GreatCircleEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> COUNT =
			SynchedEntityData.defineId(GreatCircleEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> RED =
			SynchedEntityData.defineId(GreatCircleEntity.class, EntityDataSerializers.BOOLEAN);
	@SuppressWarnings("unchecked")
	private static final EntityDataAccessor<Integer>[] RELAYS = new EntityDataAccessor[GreatCircle.MAX_RELAYS];

	static {
		for (int i = 0; i < RELAYS.length; i++) {
			RELAYS[i] = SynchedEntityData.defineId(GreatCircleEntity.class, EntityDataSerializers.INT);
		}
	}

	public GreatCircleEntity(EntityType<? extends GreatCircleEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	public static GreatCircleEntity spawn(ServerLevel level, GreatCircle.Ring ring, boolean red) {
		GreatCircleEntity circle = new GreatCircleEntity(FmabEntities.GREAT_CIRCLE, level);
		circle.setPos(Vec3.atBottomCenterOf(ring.center()));
		circle.entityData.set(RADIUS, (float) ring.radius());
		circle.entityData.set(COUNT, ring.relays().size());
		circle.entityData.set(RED, red);
		for (int i = 0; i < ring.relays().size(); i++) {
			circle.entityData.set(RELAYS[i], pack(ring.relays().get(i).subtract(ring.center())));
		}
		level.addFreshEntity(circle);
		return circle;
	}

	/** Un décalage tient dans un entier : 12 bits pour x et z, 8 pour y. */
	static int pack(BlockPos offset) {
		return (offset.getX() & 0xFFF) << 20 | (offset.getZ() & 0xFFF) << 8 | offset.getY() & 0xFF;
	}

	static BlockPos unpack(int packed) {
		return new BlockPos(packed >> 20, (byte) packed, packed << 12 >> 20);
	}

	public float radius() {
		return entityData.get(RADIUS);
	}

	/** Les relais, relatifs au centre, dans l'ordre des angles. */
	public List<BlockPos> relays() {
		List<BlockPos> out = new ArrayList<>();
		for (int i = 0; i < Math.min(entityData.get(COUNT), RELAYS.length); i++) {
			out.add(unpack(entityData.get(RELAYS[i])));
		}
		return out;
	}

	/** Rouge quand le cercle géant consume des vies : celui d'une Pierre philosophale. */
	public boolean red() {
		return entityData.get(RED);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(RADIUS, 0f);
		builder.define(COUNT, 0);
		builder.define(RED, false);
		for (EntityDataAccessor<Integer> relay : RELAYS) {
			builder.define(relay, 0);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && tickCount >= LIFETIME) {
			discard();
		}
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		// Un cercle à l'échelle d'une ville se voit de loin.
		double far = radius() + 128;
		return distance < far * far;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}
}
