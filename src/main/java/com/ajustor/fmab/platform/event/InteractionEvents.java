package com.ajustor.fmab.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Ce que fait un joueur de ses mains : frapper, utiliser, casser. Déclenchés des deux côtés (le client
 * prédit, le serveur tranche). Pour frapper et utiliser, le premier écouteur qui répond autre chose
 * que {@link InteractionResult#PASS} décide, et le jeu ne fait pas son geste habituel.
 */
public final class InteractionEvents {
	public static final Event<AttackEntity> ATTACK_ENTITY = Event.create(listeners -> (player, level, hand, entity, hit) ->
			first(listeners, l -> l.attack(player, level, hand, entity, hit)));
	public static final Event<AttackBlock> ATTACK_BLOCK = Event.create(listeners -> (player, level, hand, pos, direction) ->
			first(listeners, l -> l.attack(player, level, hand, pos, direction)));
	public static final Event<UseEntity> USE_ENTITY = Event.create(listeners -> (player, level, hand, entity, hit) ->
			first(listeners, l -> l.use(player, level, hand, entity, hit)));
	public static final Event<UseBlock> USE_BLOCK = Event.create(listeners -> (player, level, hand, hit) ->
			first(listeners, l -> l.use(player, level, hand, hit)));
	public static final Event<UseItem> USE_ITEM = Event.create(listeners -> (player, level, hand) ->
			first(listeners, l -> l.use(player, level, hand)));
	/** Un joueur va casser un bloc (côté serveur) : un seul refus l'en empêche. */
	public static final Event<BeforeBreak> BEFORE_BREAK = Event.create(listeners -> (level, player, pos, state, blockEntity) -> {
		for (BeforeBreak l : listeners) {
			if (!l.beforeBreak(level, player, pos, state, blockEntity)) {
				return false;
			}
		}
		return true;
	});
	/** Un joueur vient de casser un bloc (côté serveur). */
	public static final Event<AfterBreak> AFTER_BREAK = Event.create(listeners -> (level, player, pos, state, blockEntity) ->
			listeners.forEach(l -> l.afterBreak(level, player, pos, state, blockEntity)));

	private InteractionEvents() {
	}

	private static <T> InteractionResult first(List<T> listeners, java.util.function.Function<T, InteractionResult> call) {
		for (T l : listeners) {
			InteractionResult result = call.apply(l);
			if (result != InteractionResult.PASS) {
				return result;
			}
		}
		return InteractionResult.PASS;
	}

	@FunctionalInterface
	public interface AttackEntity {
		InteractionResult attack(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hit);
	}

	@FunctionalInterface
	public interface AttackBlock {
		InteractionResult attack(Player player, Level level, InteractionHand hand, BlockPos pos, Direction direction);
	}

	@FunctionalInterface
	public interface UseEntity {
		InteractionResult use(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hit);
	}

	@FunctionalInterface
	public interface UseBlock {
		InteractionResult use(Player player, Level level, InteractionHand hand, BlockHitResult hit);
	}

	@FunctionalInterface
	public interface UseItem {
		InteractionResult use(Player player, Level level, InteractionHand hand);
	}

	@FunctionalInterface
	public interface BeforeBreak {
		boolean beforeBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity);
	}

	@FunctionalInterface
	public interface AfterBreak {
		void afterBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity);
	}
}
