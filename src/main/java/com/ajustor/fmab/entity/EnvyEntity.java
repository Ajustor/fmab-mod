package com.ajustor.fmab.entity;

import com.ajustor.fmab.transmutation.TransmutationLightning;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Envy (Envie) : il prend l'apparence de qui il veut. Il erre déguisé (un joueur, un habitant
 * d'Amestris) et se révèle quand on l'approche de trop près ou qu'on le frappe. Sa Pierre à moitié
 * vide, il devient un monstre géant fait de visages ; à la dernière âme, il ne reste qu'un petit
 * lézard vert, fragile, qui fuit.
 */
public class EnvyEntity extends HomunculusEntity {
	public enum Form {
		/** Sous les traits d'un joueur (sa peau est copiée). */
		DISGUISED_PLAYER,
		/** Sous les traits d'un habitant d'Amestris (Izumi, Winry, un militaire…). */
		DISGUISED_CITIZEN,
		HUMAN,
		GIANT,
		LIZARD;

		public boolean disguised() {
			return this == DISGUISED_PLAYER || this == DISGUISED_CITIZEN;
		}

		public String serializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** Les habitants dont Envy peut prendre les traits : les textures des PNJ du mod. */
	public static final List<String> CITIZENS = List.of("izumi", "winry", "state_examiner");

	private static final EntityDataAccessor<Integer> FORM =
			SynchedEntityData.defineId(EnvyEntity.class, EntityDataSerializers.INT);
	/** La peau copiée : l'UUID d'un joueur, ou le nom d'un habitant. */
	private static final EntityDataAccessor<String> DISGUISE =
			SynchedEntityData.defineId(EnvyEntity.class, EntityDataSerializers.STRING);

	private static final int SOULS = 10;
	/** Distance à laquelle Envy perd patience et se révèle. */
	private static final double REVEAL_DISTANCE = 3;

	private int closeTicks;

	public EnvyEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level, SOULS);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 50)
				.add(Attributes.MOVEMENT_SPEED, 0.36)
				.add(Attributes.ATTACK_DAMAGE, 7)
				.add(Attributes.FOLLOW_RANGE, 32);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(FORM, Form.DISGUISED_CITIZEN.ordinal());
		builder.define(DISGUISE, CITIZENS.getFirst());
	}

	public Form form() {
		return Form.values()[entityData.get(FORM)];
	}

	public String disguise() {
		return entityData.get(DISGUISE);
	}

	/** Envy prend les traits d'un joueur. */
	public void disguiseAs(ServerPlayer player) {
		entityData.set(FORM, Form.DISGUISED_PLAYER.ordinal());
		entityData.set(DISGUISE, player.getUUID().toString());
	}

	/** Envy prend les traits d'un habitant. */
	public void disguiseAs(String citizen) {
		entityData.set(FORM, Form.DISGUISED_CITIZEN.ordinal());
		entityData.set(DISGUISE, citizen);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 10, 1.2, 1.5) {
			@Override
			public boolean canUse() {
				return form() == Form.LIZARD && super.canUse();
			}
		});
		goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.45f) {
			@Override
			public boolean canUse() {
				return form() == Form.HUMAN && super.canUse();
			}
		});
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false) {
			@Override
			public boolean canUse() {
				return !form().disguised() && form() != Form.LIZARD && super.canUse();
			}
		});
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				return form() != Form.LIZARD && super.canUse();
			}
		});
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
			@Override
			public boolean canUse() {
				return !form().disguised() && form() != Form.LIZARD && super.canUse();
			}
		});
	}

	@Override
	protected boolean showBossBar() {
		return !form().disguised();
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!(level() instanceof ServerLevel level) || !form().disguised()) {
			return;
		}
		// Déguisé, il supporte mal qu'on le dévisage de trop près.
		Player near = level.getNearestPlayer(this, REVEAL_DISTANCE);
		closeTicks = near != null && !near.isCreative() ? closeTicks + 1 : 0;
		if (closeTicks > 40) {
			reveal(level);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (form().disguised()) {
			reveal(level);
		}
		return super.hurtServer(level, source, damage);
	}

	/** Le déguisement tombe : le vrai visage d'Envy, et son rire. */
	private void reveal(ServerLevel level) {
		setForm(Form.HUMAN);
		TransmutationLightning.discharge(level, blockPosition(), 1, 0.7);
		level.playSound(null, blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1.5f, 0.8f);
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(24))) {
			p.sendSystemMessage(Component.translatable("homunculus.fmab.envy_reveals"));
		}
	}

	/** Chaque mort le transforme : un monstre géant quand sa Pierre faiblit, un lézard à la fin. */
	@Override
	protected void onReconstitute(ServerLevel level) {
		if (souls() == 0) {
			setForm(Form.LIZARD);
		} else if (souls() <= SOULS / 2 && form() != Form.GIANT) {
			setForm(Form.GIANT);
			level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2, 0.4f);
		}
	}

	private void setForm(Form form) {
		entityData.set(FORM, form.ordinal());
		switch (form) {
			case GIANT -> attributes(150, 0.26, 14, 2.8);
			case LIZARD -> attributes(8, 0.42, 0, 0.5);
			default -> attributes(50, 0.36, 7, 1);
		}
		setHealth(getMaxHealth());
	}

	private void attributes(double health, double speed, double damage, double scale) {
		set(Attributes.MAX_HEALTH, health);
		set(Attributes.MOVEMENT_SPEED, speed);
		set(Attributes.ATTACK_DAMAGE, damage);
		set(Attributes.SCALE, scale);
	}

	private void set(Holder<Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	/** Un joueur dont Envy a pris les traits, s'il en a pris un. */
	public Optional<UUID> copiedPlayer() {
		if (form() != Form.DISGUISED_PLAYER) {
			return Optional.empty();
		}
		try {
			return Optional.of(UUID.fromString(disguise()));
		} catch (IllegalArgumentException notAPlayer) {
			return Optional.empty();
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("form", form().serializedName());
		output.putString("disguise", disguise());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		try {
			entityData.set(FORM, Form.valueOf(input.getStringOr("form", "disguised_citizen")
					.toUpperCase(Locale.ROOT)).ordinal());
		} catch (IllegalArgumentException unknown) {
			entityData.set(FORM, Form.DISGUISED_CITIZEN.ordinal());
		}
		entityData.set(DISGUISE, input.getStringOr("disguise", CITIZENS.getFirst()));
	}
}
