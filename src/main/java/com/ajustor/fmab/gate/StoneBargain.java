package com.ajustor.fmab.gate;

import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.item.PhilosopherStoneItem;
import com.ajustor.fmab.network.BargainPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Marchander avec la Vérité : devant sa Porte, l'alchimiste qui porte une Pierre philosophale
 * rachète ce qu'elle lui a pris, partie par partie, avec les âmes de la Pierre (au prix de
 * {@link Restoration#cost}). La visite attend qu'il ait fini, deux minutes au plus.
 */
public final class StoneBargain {
	/** La Vérité ne marchande pas plus de deux minutes. */
	private static final int PATIENCE = 20 * 60 * 2;

	private static final Map<UUID, Long> BARGAINING = new HashMap<>();

	private StoneBargain() {
	}

	/**
	 * La Vérité propose le marché, s'il y a quelque chose à racheter et une Pierre pour payer.
	 *
	 * @return vrai si le marché est ouvert
	 */
	public static boolean offer(ServerPlayer player) {
		GateState gate = player.getAttachedOrCreate(FmabAttachments.GATE);
		if (gate.lost().isEmpty() || souls(player) <= 0) {
			return false;
		}
		BARGAINING.putIfAbsent(player.getUUID(), player.level().getGameTime());
		send(player, gate);
		return true;
	}

	/** Le marché tient-il encore la visite ? */
	public static boolean holds(ServerPlayer player) {
		Long since = BARGAINING.get(player.getUUID());
		if (since == null) {
			return false;
		}
		if (player.level().getGameTime() - since > PATIENCE) {
			BARGAINING.remove(player.getUUID());
			return false;
		}
		return true;
	}

	/** L'alchimiste rachète une partie, ou (partie vide) s'en va. */
	public static void answer(ServerPlayer player, String partName) {
		if (!holds(player)) {
			return;
		}
		Optional<BodyPart> part = part(partName);
		GateState gate = player.getAttachedOrCreate(FmabAttachments.GATE);
		if (part.isEmpty() || !gate.lost().contains(part.get())) {
			BARGAINING.remove(player.getUUID());
			return;
		}
		int cost = Restoration.cost(part.get());
		if (!player.isCreative() && souls(player) < cost) {
			player.sendOverlayMessage(Component.translatable("truth.fmab.bargain.too_poor", cost));
			send(player, gate);
			return;
		}
		if (!player.isCreative()) {
			pay(player, cost);
		}
		PhilosopherStoneItem.restore(player, gate, new Restoration.Plan(EnumSet.of(part.get()), cost));
		GateState after = player.getAttachedOrCreate(FmabAttachments.GATE);
		if (after.lost().isEmpty() || souls(player) <= 0) {
			BARGAINING.remove(player.getUUID());
			player.sendSystemMessage(Component.translatable("truth.fmab.bargain.done")
					.withStyle(s -> s.withItalic(true).withColor(0x9A9A9A)));
			ServerPlayNetworking.send(player, BargainPayload.CLOSE);
		} else {
			send(player, after);
		}
	}

	private static Optional<BodyPart> part(String name) {
		try {
			return name.isEmpty() ? Optional.empty() : Optional.of(BodyPart.fromSerializedName(name));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private static void send(ServerPlayer player, GateState gate) {
		List<String> parts = new ArrayList<>();
		List<Integer> costs = new ArrayList<>();
		gate.lost().stream().sorted(Comparator.comparingInt(Restoration::cost).reversed())
				.forEach(p -> {
					parts.add(p.serializedName());
					costs.add(Restoration.cost(p));
				});
		ServerPlayNetworking.send(player, new BargainPayload(parts, costs, souls(player)));
	}

	/** Les âmes de toutes les Pierres que l'alchimiste porte. */
	static int souls(ServerPlayer player) {
		int souls = 0;
		for (ItemStack stack : stones(player)) {
			souls += PhilosopherStoneItem.souls(stack);
		}
		return souls;
	}

	/** On paie avec les Pierres les moins pleines d'abord : une Pierre vide tombe en poussière. */
	private static void pay(ServerPlayer player, int cost) {
		List<ItemStack> stones = stones(player);
		stones.sort(Comparator.comparingInt(PhilosopherStoneItem::souls));
		int left = cost;
		for (ItemStack stone : stones) {
			int taken = Math.min(left, PhilosopherStoneItem.souls(stone));
			stone.hurtAndBreak(taken, player.level(), player, broken -> player.sendSystemMessage(
					Component.translatable("item.fmab.philosopher_stone.spent")));
			left -= taken;
			if (left <= 0) {
				return;
			}
		}
	}

	private static List<ItemStack> stones(ServerPlayer player) {
		List<ItemStack> out = new ArrayList<>();
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(FmabItems.PHILOSOPHER_STONE) && PhilosopherStoneItem.souls(stack) > 0) {
				out.add(stack);
			}
		}
		return out;
	}
}
