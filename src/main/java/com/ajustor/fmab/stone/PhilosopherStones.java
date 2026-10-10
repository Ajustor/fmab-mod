package com.ajustor.fmab.stone;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.entity.HomunculusEntity;
import com.ajustor.fmab.item.PhilosopherStoneItem;
import com.ajustor.fmab.registry.FmabItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Ce que fait la Pierre philosophale dans les mains d'un alchimiste : elle amplifie ses
 * transmutations (portée et dégâts ×1,5, aucune concentration, aucun rebond d'instabilité) au prix
 * de quelques âmes ; et elle se voit de loin : les monstres sentent sa présence et viennent la
 * chercher.
 */
public final class PhilosopherStones {
	/** Ce qu'ajoute la Pierre à une transmutation. */
	public static final double AMPLIFICATION = 1.5;
	/** Les problèmes que la Pierre efface : elle tient le cercle et comprend pour vous. */
	private static final Set<CircleIssue.Kind> STEADIED = EnumSet.of(CircleIssue.Kind.UNSTABLE,
			CircleIssue.Kind.GLYPH_NOT_LEARNED, CircleIssue.Kind.RANK_TOO_LOW);
	/** Distance à laquelle les monstres sentent une Pierre. */
	private static final double LURE = 24;

	private PhilosopherStones() {
	}

	/**
	 * La Pierre tenue en main (l'une ou l'autre), s'il lui reste des âmes : une vraie Pierre d'abord,
	 * sinon un éclat de pierre rouge impure.
	 */
	public static Optional<ItemStack> held(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.is(FmabItems.PHILOSOPHER_STONE) && PhilosopherStoneItem.souls(stack) > 0) {
				return Optional.of(stack);
			}
		}
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.is(FmabItems.RED_STONE_SHARD) && PhilosopherStoneItem.souls(stack) > 0) {
				return Optional.of(stack);
			}
		}
		return Optional.empty();
	}

	/** Une Pierre en main, ou une Pierre vivante : la transmutation est amplifiée et ne coûte rien. */
	public static boolean amplifies(ServerPlayer player) {
		return held(player).isPresent() || LivingStone.souls(player) > 0;
	}

	/** Une vraie Pierre, et non un éclat de pierre rouge impure. */
	public static boolean pure(ItemStack stone) {
		return stone.is(FmabItems.PHILOSOPHER_STONE);
	}

	/** La Pierre empêche-t-elle ce cercle de rebondir ? Seulement si ce qui cloche, c'est sa tenue. */
	public static boolean steadies(Analysis analysis) {
		return analysis.issues().stream().allMatch(i -> STEADIED.contains(i.kind()));
	}

	/** Les âmes que la Pierre dépense pour une transmutation : une, plus une par étage. */
	public static int cost(Analysis analysis) {
		return 1 + analysis.parsed().stages().size();
	}

	/** Puise des âmes dans la Pierre ; vide, elle tombe en poussière. */
	public static void drain(ServerPlayer player, ItemStack stone, int souls) {
		if (player.isCreative()) {
			return;
		}
		stone.hurtAndBreak(souls, player.level(), player, broken -> player.sendSystemMessage(
				Component.translatable("item.fmab.philosopher_stone.spent")));
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (carries(player) || LivingStone.souls(player) > 0) {
					lure(player);
				}
			}
		});
	}

	private static boolean carries(ServerPlayer player) {
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(FmabItems.PHILOSOPHER_STONE)) {
				return true;
			}
		}
		return false;
	}

	/** Une Pierre se voit de loin : le porteur luit, et les monstres alentour accourent. */
	private static void lure(ServerPlayer player) {
		if (player.isCreative() || player.isSpectator()) {
			return;
		}
		player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false, false));
		for (Monster monster : player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(LURE),
				m -> m.getTarget() == null && !(m instanceof HomunculusEntity))) {
			monster.setTarget(player);
		}
	}
}
