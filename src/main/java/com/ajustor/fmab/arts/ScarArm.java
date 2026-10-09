package com.ajustor.fmab.arts;

import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabSounds;
import com.ajustor.fmab.transmutation.GloveCasting;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Le bras droit de Scar : un tatouage qui unit l'alchimie d'Amestris et l'alkahestry de Xing, mais
 * dont on n'a gardé que la décomposition. Main nue, frapper un bloc le réduit en poussière (il
 * tombe en ressources) ; frapper une créature la déchire de l'intérieur. Il ne craint pas la zone
 * de Père : il puise aussi au flux du dragon.
 */
public final class ScarArm {
	private static final float BLOCK_COST = 1;
	private static final float ENTITY_COST = 2;
	private static final float ENTITY_DAMAGE = 7;
	private static final int COOLDOWN = 8;
	private static final Map<UUID, Long> LAST = new HashMap<>();

	private ScarArm() {
	}

	/** Le joueur porte-t-il le bras de Scar, main droite nue ? Valable des deux côtés. */
	public static boolean armed(Player player) {
		return Boolean.TRUE.equals(player.getAttached(FmabAttachments.SCAR_ARM)) && player.getMainHandItem().isEmpty();
	}

	public static void register() {
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
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
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
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
		if (!pay(player, BLOCK_COST)) {
			return;
		}
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5,
				pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.15);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.4,
				0.4, 0.4, 0.1);
		level.destroyBlock(pos, !player.isCreative(), player);
	}

	private static boolean ready(ServerPlayer player, ServerLevel level) {
		long now = level.getGameTime();
		Long last = LAST.get(player.getUUID());
		if (last != null && now - last < COOLDOWN) {
			return false;
		}
		LAST.put(player.getUUID(), now);
		return true;
	}

	/** Le bras puise dans la concentration, comme toute transmutation. */
	private static boolean pay(ServerPlayer player, float cost) {
		if (player.isCreative()) {
			return true;
		}
		AlchemistData data = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		if (data.concentration() < cost) {
			player.sendOverlayMessage(Component.translatable("transmutation.fmab.tired", (int) cost,
					(int) data.concentration()));
			return false;
		}
		player.setAttached(FmabAttachments.ALCHEMIST, data.withConcentration(data.concentration() - cost));
		return true;
	}
}
