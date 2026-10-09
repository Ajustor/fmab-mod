package com.ajustor.fmab.entity;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.Gifts;
import com.ajustor.fmab.network.OpenIzumiPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabTags;
import com.ajustor.fmab.training.Trainings;
import com.ajustor.fmab.training.Trial;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import com.ajustor.fmab.world.Maps;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;

import java.util.UUID;

/**
 * Izumi Curtis, maîtresse d'entraînement. Elle propose des épreuves et, à qui le demande, un combat
 * d'entraînement : elle transmute sans cercle, en joignant les mains.
 *
 * <p>Hors combat, rien ne l'atteint. En combat, seul son élève peut la toucher ; elle s'arrête quand
 * elle a perdu la moitié de sa santé (l'élève a tenu tête) ou quand l'élève est à bout.
 */
public class IzumiEntity extends PathfinderMob {
	/** Un élève à moins de cette santé ne commence pas de combat, et le combat s'arrête là. */
	private static final float STUDENT_LIMIT = 6;
	private static final float MIN_HEALTH_TO_SPAR = 12;
	private static final double SPAR_DISTANCE = 24;
	/** Une pique jaillit sous l'élève toutes les quatre secondes. */
	private static final int CLAP_INTERVAL = 80;
	private static final float CLAP_DAMAGE = 4;

	private static final String ISLAND_MAP = "yock_map";
	private static final TagKey<Structure> YOCK_MAPS = TagKey.create(Registries.STRUCTURE, Fmab.id("on_yock_island_maps"));

	private UUID student;
	/** Sa boutique, retenue le temps d'un combat d'entraînement. */
	private BlockPos shop;
	/** Pas d'île de Yock à portée : on ne recherche pas avant cette heure. */
	private long nextMapSearch;
	private int sparTicks;

	public IzumiEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.ATTACK_DAMAGE, 4)
				.add(Attributes.FOLLOW_RANGE, SPAR_DISTANCE)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer && student == null) {
			serverPlayer.setAttached(FmabAttachments.TRAINING,
					serverPlayer.getAttachedOrCreate(FmabAttachments.TRAINING).meet());
			giveIslandMap(serverPlayer);
			ServerPlayNetworking.send(serverPlayer, OpenIzumiPayload.of(this, serverPlayer));
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * La dernière épreuve se passe sur l'île de Yock : Izumi en donne la carte à chaque élève, une
	 * fois. S'il n'y a pas d'île à portée, elle n'en donne pas (et réessaiera la prochaine fois).
	 */
	private void giveIslandMap(ServerPlayer player) {
		if (Gifts.received(player, ISLAND_MAP) || !(level() instanceof ServerLevel level)
				|| level.getGameTime() < nextMapSearch) {
			return;
		}
		ItemStack map = Maps.toStructure(level, blockPosition(), YOCK_MAPS, MapDecorationTypes.RED_X,
				Component.translatable("filled_map.fmab.yock_island"));
		if (map.isEmpty()) {
			// La recherche coûte cher : on ne la refait pas avant cinq minutes.
			nextMapSearch = level.getGameTime() + 20 * 60 * 5;
			return;
		}
		if (Gifts.give(player, ISLAND_MAP, map)) {
			player.sendSystemMessage(Component.translatable("entity.fmab.izumi.island_map"));
		}
	}

	/** L'élève demande un combat d'entraînement. */
	public void startSpar(ServerPlayer player) {
		if (student != null) {
			return;
		}
		if (player.getHealth() < MIN_HEALTH_TO_SPAR) {
			player.sendSystemMessage(Component.translatable("entity.fmab.izumi.rest_first"));
			return;
		}
		student = player.getUUID();
		sparTicks = 0;
		setHealth(getMaxHealth());
		setTarget(player);
		player.sendSystemMessage(Component.translatable("entity.fmab.izumi.spar_start"));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		// Elle ne quitte pas sa boutique, sauf pour suivre un élève en combat.
		if (student != null) {
			if (hasHome()) {
				shop = getHomePosition();
				clearHome();
			}
		} else if (!hasHome()) {
			setHomeTo(shop != null ? shop : blockPosition(), 10);
		}
		if (student == null) {
			return;
		}
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(student);
		if (player == null || !player.isAlive() || player.level() != level
				|| player.distanceToSqr(this) > SPAR_DISTANCE * SPAR_DISTANCE) {
			endSpar(player, false);
			return;
		}
		if (player.getHealth() <= STUDENT_LIMIT) {
			endSpar(player, false);
			return;
		}
		setTarget(player);
		if (++sparTicks % CLAP_INTERVAL == 0) {
			clap(level, player);
		}
	}

	/** Elle joint les mains : la terre sous l'élève jaillit. */
	private void clap(ServerLevel level, ServerPlayer player) {
		level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1, 1.6f);
		BlockPos feet = player.blockPosition();
		BlockPos ground = feet.below();
		BlockState earth = level.getBlockState(ground);
		TransmutationLightning.discharge(level, feet, 1.5, 0.8);
		if (!earth.is(FmabTags.elementBlocks("earth")) || earth.hasBlockEntity()
				|| !level.getBlockState(feet).canBeReplaced()) {
			return;
		}
		// Échange équivalent : la pique est faite du bloc qu'elle laisse vide sous elle.
		level.setBlockAndUpdate(ground, Blocks.AIR.defaultBlockState());
		level.setBlockAndUpdate(feet, earth);
		player.setPos(player.getX(), feet.getY() + 1, player.getZ());
		player.hurtServer(level, level.damageSources().mobAttack(this), CLAP_DAMAGE);
		player.setDeltaMovement(player.getDeltaMovement().add(0, 0.6, 0));
		player.hurtMarked = true;
	}

	private void endSpar(ServerPlayer player, boolean studentHeld) {
		student = null;
		setTarget(null);
		setHealth(getMaxHealth());
		if (player == null) {
			return;
		}
		if (studentHeld) {
			player.sendSystemMessage(Component.translatable("entity.fmab.izumi.spar_won"));
			Trainings.achieve(player, Trial.SPAR);
		} else {
			player.sendSystemMessage(Component.translatable("entity.fmab.izumi.spar_lost"));
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (student == null || source.getEntity() == null || !student.equals(source.getEntity().getUUID())) {
			return false;
		}
		// Un combat d'entraînement ne tue personne, elle non plus.
		boolean hurt = super.hurtServer(level, source, Math.min(damage, getHealth() - 1));
		if (getHealth() <= getMaxHealth() / 2 && source.getEntity() instanceof ServerPlayer player) {
			endSpar(player, true);
		}
		return hurt;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}
}
