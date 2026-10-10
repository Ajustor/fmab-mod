package com.ajustor.fmab.entity;

import com.ajustor.fmab.gate.Wheelchairs;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Le fauteuil roulant : une monture que n'importe qui peut prendre, et qu'un joueur qui a perdu ses
 * deux jambes doit prendre pour ne pas ramper.
 *
 * <p>Ses règles : il ne saute pas ; il ne franchit qu'une demi-hauteur de bloc (une dalle, une marche
 * d'escalier), donc il lui faut des rampes ; il ne grimpe pas aux échelles ; une chute de plus de deux
 * blocs le renverse ; dans l'eau, on en tombe. Il avance à la force des deux bras de celui qui est
 * assis, qui ne peut alors rien faire d'autre de ses mains, ou parce qu'on le pousse : un autre
 * joueur, qui le dirige alors, ou un villageois payé d'une émeraude, qui suit derrière.
 *
 * <p>Celui qui le pilote en est le client : ses mouvements partent de son jeu, comme pour une barque.
 * Poussé par un joueur, le fauteuil n'a plus de pilote : c'est le serveur qui le place devant lui.
 */
public class WheelchairEntity extends Mob {
	/** Ce qu'il franchit sans rampe : une dalle ou une marche, jamais un bloc plein. */
	public static final float STEP = 0.5f;
	/** Au-delà de cette chute, le fauteuil se renverse et jette son occupant. */
	private static final double TIP_OVER_FALL = 2.5;
	/** Vitesse quand on fait tourner les roues soi-même, et quand on est poussé. */
	private static final float SELF_SPEED = 0.075f;
	private static final float PUSHED_SPEED = 0.1f;
	/** Les poignées : la distance entre le fauteuil et celui qui pousse. */
	private static final double HANDLES = 0.95;
	/** Trop loin des poignées, on les lâche. */
	private static final double GRIP_LOST = 3.0;
	/** Ce que paie une émeraude : cinq minutes de poussée. */
	public static final int HIRE_TICKS = 20 * 60 * 5;
	/** Le rayon des grandes roues, pour qu'elles tournent au rythme du sol. */
	private static final float WHEEL_RADIUS = 5 / 16f;

	/** Qui tient les poignées : l'identifiant réseau d'un joueur ou d'un villageois, ou -1. */
	private static final EntityDataAccessor<Integer> PUSHER =
			SynchedEntityData.defineId(WheelchairEntity.class, EntityDataSerializers.INT);

	/** Jusqu'à quand le villageois payé pousse encore (heure du monde). */
	private long hiredUntil;
	private @Nullable Vec3 lastPosition;
	/** L'occupant fait tourner les roues lui-même, à cet instant : ses mains sont prises. */
	private boolean propelling;
	/** Les roues, en radians, pour le rendu : la valeur de cette tick et celle d'avant. */
	private float wheel;
	private float wheelO;

	public WheelchairEntity(EntityType<? extends Mob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 4)
				.add(Attributes.MOVEMENT_SPEED, PUSHED_SPEED)
				.add(Attributes.STEP_HEIGHT, STEP);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PUSHER, -1);
	}

	// --- Qui le fait avancer ---------------------------------------------------------------------

	/** Celui qui tient les poignées, s'il est encore là. */
	public @Nullable LivingEntity pusher() {
		int id = entityData.get(PUSHER);
		return id < 0 ? null : level().getEntity(id) instanceof LivingEntity living ? living : null;
	}

	public boolean pushedBy(Entity entity) {
		return entityData.get(PUSHER) == entity.getId();
	}

	private boolean pushedByPlayer() {
		return pusher() instanceof Player;
	}

	private boolean pushedByVillager() {
		return pusher() instanceof Villager;
	}

	/** L'occupant fait-il avancer le fauteuil de ses propres bras, en ce moment ? */
	public boolean propelling() {
		return propelling;
	}

	private void grab(LivingEntity pusher) {
		entityData.set(PUSHER, pusher.getId());
	}

	/** Les poignées sont lâchées ; l'occupant en est prévenu. */
	public void letGo() {
		if (entityData.get(PUSHER) < 0) {
			return;
		}
		entityData.set(PUSHER, -1);
		hiredUntil = 0;
		if (getFirstPassenger() instanceof ServerPlayer rider) {
			rider.sendOverlayMessage(Component.translatable("wheelchair.fmab.let_go"));
		}
	}

	/** Un villageois, payé, se met aux poignées pour cinq minutes. */
	public void hire(Villager villager) {
		grab(villager);
		hiredUntil = level().getGameTime() + HIRE_TICKS;
	}

	// --- Monture -----------------------------------------------------------------------------------

	/**
	 * L'occupant pilote, sauf quand un joueur pousse : c'est alors lui qui dirige, et le serveur qui
	 * place le fauteuil.
	 */
	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		if (pushedByPlayer()) {
			return null;
		}
		return getFirstPassenger() instanceof Player player ? player : null;
	}

	@Override
	protected void tickRidden(Player controller, Vec3 riddenInput) {
		super.tickRidden(controller, riddenInput);
		setRot(controller.getYRot(), 0);
		yRotO = yBodyRot = yHeadRot = getYRot();
	}

	/** On avance et on recule (moins vite) ; on tourne en regardant ailleurs. Pas de pas de côté. */
	@Override
	protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
		float forward = controller.zza;
		if (forward == 0) {
			return Vec3.ZERO;
		}
		if (!pushedByVillager() && !Wheelchairs.canPropel(controller)) {
			// Une seule main ne fait tourner qu'une roue : il faut qu'on pousse.
			if (level().isClientSide() && tickCount % 40 == 0) {
				controller.sendOverlayMessage(Component.translatable("wheelchair.fmab.need_hands"));
			}
			return Vec3.ZERO;
		}
		return new Vec3(0, 0, forward < 0 ? forward * 0.5 : forward);
	}

	@Override
	protected float getRiddenSpeed(Player controller) {
		return pushedByVillager() ? PUSHED_SPEED : SELF_SPEED;
	}

	/** Vanilla laisse un cavalier franchir un bloc entier : pas un fauteuil. */
	@Override
	public float maxUpStep() {
		return STEP;
	}

	@Override
	public boolean onClimbable() {
		return false;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().isEmpty();
	}

	// --- Chaque tick -------------------------------------------------------------------------------

	@Override
	public void tick() {
		super.tick();
		Vec3 here = position();
		double moved = lastPosition == null ? 0 : here.subtract(lastPosition).horizontalDistance();
		if (level().isClientSide() && lastPosition != null) {
			// Les roues tournent au rythme du sol, dans le sens de la marche.
			Vec3 facing = Vec3.directionFromRotation(0, getYRot());
			double along = here.subtract(lastPosition).dot(facing);
			wheelO = wheel;
			wheel += (float) (along / WHEEL_RADIUS);
		}
		propelling = getFirstPassenger() instanceof Player && entityData.get(PUSHER) < 0 && moved > 0.01;
		lastPosition = here;
		if (level() instanceof ServerLevel server) {
			serverTick(server);
		}
	}

	private void serverTick(ServerLevel level) {
		if (isInWater() && isVehicle()) {
			// Un fauteuil ne flotte pas : on en tombe.
			ejectPassengers();
		}
		LivingEntity pusher = pusher();
		if (entityData.get(PUSHER) >= 0 && (pusher == null || !pusher.isAlive())) {
			letGo();
			return;
		}
		if (pusher instanceof Player player) {
			pushAlong(player);
		} else if (pusher instanceof Villager villager) {
			followedBy(villager);
		}
	}

	/** Un joueur pousse : le fauteuil reste devant lui, tourné comme lui. */
	private void pushAlong(Player player) {
		if (player.isShiftKeyDown() || player.isPassenger() || distanceTo(player) > GRIP_LOST
				|| !Wheelchairs.canPropel(player)) {
			letGo();
			return;
		}
		player.setSprinting(false);
		Vec3 ahead = Vec3.directionFromRotation(0, player.getYRot()).scale(HANDLES);
		Vec3 target = player.position().add(ahead);
		Vec3 delta = target.subtract(position());
		move(MoverType.SELF, new Vec3(delta.x, 0, delta.z));
		setRot(player.getYRot(), 0);
		yRotO = yBodyRot = yHeadRot = getYRot();
	}

	/** Un villageois payé suit derrière, aux poignées ; l'occupant dirige. */
	private void followedBy(Villager villager) {
		if (level().getGameTime() > hiredUntil || villager.isSleeping() || distanceTo(villager) > GRIP_LOST) {
			letGo();
			return;
		}
		villager.getNavigation().stop();
		Vec3 behind = position().subtract(Vec3.directionFromRotation(0, getYRot()).scale(HANDLES));
		Vec3 step = behind.subtract(villager.position());
		if (level().noCollision(villager, villager.getBoundingBox().move(step.x, 0, step.z))) {
			villager.setPos(behind.x, villager.getY(), behind.z);
		}
		villager.setYRot(getYRot());
		villager.setYHeadRot(getYRot());
		villager.yBodyRot = getYRot();
	}

	// --- Interactions ------------------------------------------------------------------------------

	/**
	 * Clic droit : s'asseoir dans un fauteuil vide ; prendre les poignées d'un fauteuil occupé ; les
	 * lâcher. Accroupi, on replie un fauteuil vide pour l'emporter.
	 */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player.isPassenger()) {
			return InteractionResult.PASS;
		}
		if (pushedBy(player)) {
			if (!level().isClientSide()) {
				letGo();
			}
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive()) {
			if (isVehicle() || entityData.get(PUSHER) >= 0) {
				return InteractionResult.PASS;
			}
			if (level() instanceof ServerLevel server) {
				fold(server, player);
			}
			return InteractionResult.SUCCESS;
		}
		if (!isVehicle()) {
			if (!level().isClientSide()) {
				player.startRiding(this);
			}
			return InteractionResult.SUCCESS;
		}
		if (entityData.get(PUSHER) >= 0) {
			return InteractionResult.PASS;
		}
		if (!Wheelchairs.canPropel(player)) {
			player.sendOverlayMessage(Component.translatable("wheelchair.fmab.need_hands_to_push"));
			return InteractionResult.FAIL;
		}
		if (!level().isClientSide()) {
			grab(player);
		}
		return InteractionResult.SUCCESS;
	}

	/** Replié, le fauteuil redevient un objet. */
	private void fold(ServerLevel level, Player player) {
		ItemStack stack = new ItemStack(FmabItems.WHEELCHAIR);
		if (!player.getAbilities().instabuild && !player.getInventory().add(stack)) {
			spawnAtLocation(level, stack);
		}
		level.playSound(null, blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 0.8f, 1.2f);
		discard();
	}

	/** Un coup de joueur le démonte ; le reste du monde ne l'abîme pas. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		if (!(source.getEntity() instanceof Player player) || isVehicle()) {
			return false;
		}
		if (!player.getAbilities().instabuild) {
			spawnAtLocation(level, new ItemStack(FmabItems.WHEELCHAIR));
		}
		level.playSound(null, blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.NEUTRAL, 0.8f, 1.0f);
		discard();
		return true;
	}

	/** Une vraie chute le renverse : l'occupant tombe, et la chute est pour lui. */
	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		if (fallDistance > TIP_OVER_FALL) {
			for (Entity passenger : getPassengers()) {
				passenger.stopRiding();
				passenger.causeFallDamage(fallDistance, damageModifier, damageSource);
			}
			letGo();
		}
		return false;
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(FmabItems.WHEELCHAIR);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	/** L'angle des roues, interpolé pour le rendu. */
	public float wheel(float partialTicks) {
		return Mth.lerp(partialTicks, wheelO, wheel);
	}
}
