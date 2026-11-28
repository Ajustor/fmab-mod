package com.ajustor.fmab.arts;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import com.ajustor.fmab.util.PlayerMaps;
import com.ajustor.fmab.alchemy.knowledge.Knowledge;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabSounds;
import com.ajustor.fmab.stone.LivingStone;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.GloveCasting;
import com.ajustor.fmab.util.Throttle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import com.ajustor.fmab.platform.event.InteractionEvents;
import com.ajustor.fmab.platform.event.PlayerEvents;

/**
 * Le bras de Scar : le motif d'Ishval tatoué autour d'un bras ({@link IshvalArm}), qui unit l'alchimie
 * d'Amestris et l'alkahestry de Xing. Sa moitié de décomposition : main nue, frapper un bloc le réduit
 * en poussière (il tombe en ressources) ; frapper une créature la déchire de l'intérieur. C'est le bras
 * de la main principale qui frappe. Sa moitié de recomposition : main nue, accroupi, toucher un bloc en
 * fait l'objet auquel on pense ({@link ArmRecomposition}), avec le bras de cette main. Il ne craint pas
 * la zone de Père : il puise aussi au flux du dragon.
 */
public final class ScarArm {
	/** L'école de savoir que le bras fait progresser. */
	private static final String SCHOOL = "destruction";
	private static final float BLOCK_COST = 1;
	/** Un point de concentration de plus par tranche de dureté : l'obsidienne coûte six fois la pierre. */
	private static final float HARDNESS_PER_POINT = 10;
	/** La Destruction progresse d'un point tous les cinq usages du bras. */
	static final int USES_PER_MASTERY = 5;
	private static final Map<UUID, Integer> USES = PlayerMaps.track(new HashMap<>());
	private static final float ENTITY_COST = 2;
	private static final float ENTITY_DAMAGE = 7;
	/** Recomposer coûte plus que défaire : il faut tenir la forme de l'objet. */
	private static final float RECOMPOSE_COST = 3;
	private static final Throttle READY = new Throttle(8);

	private ScarArm() {
	}

	/**
	 * Le joueur frappe-t-il du bras de Scar : main principale nue, son bras tatoué de la moitié de
	 * décomposition ? Valable des deux côtés.
	 */
	public static boolean armed(Player player) {
		return player.getMainHandItem().isEmpty()
				&& IshvalArm.has(player, IshvalArm.mainArmLeft(player), IshvalArm.Half.DECOMPOSE);
	}

	/**
	 * Cette main recompose-t-elle : nue, le joueur accroupi, son bras tatoué de la moitié de
	 * recomposition ? Côté serveur seulement (l'alkahestry ne se lit que là).
	 */
	static boolean recomposes(Player player, InteractionHand hand) {
		if (!player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) {
			return false;
		}
		boolean left = IshvalArm.mainArmLeft(player) == (hand == InteractionHand.MAIN_HAND);
		return IshvalArm.has(player, left, IshvalArm.Half.RECOMPOSE);
	}

	public static void register() {
		PlayerEvents.JOIN.register((player, server) -> IshvalArm.migrate(player));
		InteractionEvents.USE_BLOCK.register((player, level, hand, hit) -> {
			if (!(player instanceof ServerPlayer p) || !(level instanceof ServerLevel server) || player.isSpectator()
					|| !recomposes(p, hand)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hit.getBlockPos();
			if (!p.isWithinBlockInteractionRange(pos, 1)) {
				return InteractionResult.PASS;
			}
			if (ready(p, server)) {
				ArmRecomposition.recompose(p, server, pos, hit.getDirection(), () -> pay(p, RECOMPOSE_COST));
			}
			return InteractionResult.SUCCESS;
		});
		InteractionEvents.ATTACK_BLOCK.register((player, level, hand, pos, direction) -> {
			// Un gantelet gravé frappe avec son propre cercle : il passe avant le bras.
			if (hand != InteractionHand.MAIN_HAND || !armed(player) || player.isSpectator()
					|| GloveCasting.canStrike(player)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer p && level instanceof ServerLevel server) {
				decompose(p, server, pos);
			}
			return InteractionResult.SUCCESS;
		});
		InteractionEvents.ATTACK_ENTITY.register((player, level, hand, entity, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || !armed(player) || !(entity instanceof LivingEntity target)
					|| !(player instanceof ServerPlayer p) || !(level instanceof ServerLevel server)) {
				return InteractionResult.PASS;
			}
			if (ready(p, server) && pay(p, ENTITY_COST)) {
				target.hurtServer(server, server.damageSources().indirectMagic(p, p), ENTITY_DAMAGE);
				server.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5), target.getZ(), 25,
						0.3, 0.5, 0.3, 0.1);
				server.playSound(null, target.blockPosition(), FmabSounds.DECOMPOSE, SoundSource.PLAYERS,
						0.8f, 1.4f);
			}
			// Le coup de poing ordinaire suit.
			return InteractionResult.PASS;
		});
	}

	private static void decompose(ServerPlayer player, ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		// Ni en mode aventure, ni sur la protection du point d'apparition, ni hors de portée.
		if (!player.mayInteract(level, pos) || player.blockActionRestricted(level, pos, player.gameMode())
				|| !player.isWithinBlockInteractionRange(pos, 1)) {
			return;
		}
		if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || !ready(player, level)) {
			return;
		}
		if (!pay(player, blockCost(state.getDestroySpeed(level, pos)))) {
			return;
		}
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5,
				pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.15);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.4,
				0.4, 0.4, 0.1);
		level.destroyBlock(pos, !player.isCreative(), player);
	}

	/** Le bras reprend son souffle entre deux usages, Pierre vivante comprise. */
	private static boolean ready(ServerPlayer player, ServerLevel level) {
		return READY.pass(player.getUUID(), level.getGameTime());
	}

	/**
	 * Le bras puise dans la concentration, comme toute transmutation (une Pierre vivante n'en a plus
	 * besoin), et fait progresser l'école de la Destruction, qui le rend moins coûteux.
	 */
	private static boolean pay(ServerPlayer player, float cost) {
		AlchemistData data = FmabAttachments.ALCHEMIST.getOrCreate(player);
		Knowledge knowledge = AlchemyRules.of(player.level().registryAccess()).knowledge(data);
		cost = (float) Math.max(0.5, cost * knowledge.practice(SCHOOL) - knowledge.perk(SCHOOL, "concentration_discount") * 0.5);
		if (LivingStone.fuels(player)) {
			// Une Pierre vivante ne puise pas dans sa concentration, mais dans ses âmes.
			LivingStone.spend(player, 1);
		} else if (!player.isCreative()) {
			float concentration = Concentration.of(player);
			if (concentration < cost) {
				player.sendOverlayMessage(Component.translatable("transmutation.fmab.tired", (int) Math.ceil(cost),
						(int) concentration));
				return false;
			}
			Concentration.set(player, concentration - cost);
		}
		int uses = USES.merge(player.getUUID(), 1, Integer::sum);
		if (masteryDue(uses)) {
			FmabAttachments.ALCHEMIST.set(player, data.addMastery(SCHOOL, 1));
		}
		return true;
	}

	/** Ce que coûte un bloc, selon sa dureté (la pierre : un peu plus d'un point). */
	static float blockCost(float hardness) {
		return BLOCK_COST + Math.max(0, hardness) / HARDNESS_PER_POINT;
	}

	/** Cet usage du bras (compté à partir de 1) fait-il progresser la Destruction ? */
	static boolean masteryDue(int uses) {
		return uses % USES_PER_MASTERY == 0;
	}
}
