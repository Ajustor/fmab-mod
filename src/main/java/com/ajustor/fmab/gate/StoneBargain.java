package com.ajustor.fmab.gate;

import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.item.PhilosopherStoneItem;
import com.ajustor.fmab.network.BargainPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.util.PlayerMaps;
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
import com.ajustor.fmab.network.FmabNetwork;

/**
 * Marchander avec la Vérité : devant sa Porte, l'alchimiste qui porte une Pierre philosophale
 * rachète ce qu'elle lui a pris, partie par partie, avec les âmes de la Pierre (au prix de
 * {@link Restoration#cost}). La visite attend qu'il ait fini, deux minutes au plus.
 */
public final class StoneBargain {
	/** La Vérité ne marchande pas plus de deux minutes. */
	private static final int PATIENCE = 20 * 60 * 2;

	/** Un marché ouvert depuis {@code since}, ou réglé (fini, quitté, ou la Vérité s'est lassée). */
	private record Bargain(long since, boolean open) {
	}

	/** Oubliés à la déconnexion : revenu en pleine visite, l'alchimiste se voit reproposer le marché. */
	private static final Map<UUID, Bargain> BARGAINS = PlayerMaps.track(new HashMap<>());

	private StoneBargain() {
	}

	/**
	 * La Vérité propose le marché, s'il y a quelque chose à racheter et une Pierre pour payer.
	 *
	 * @return vrai si le marché est ouvert
	 */
	public static boolean offer(ServerPlayer player) {
		GateState gate = FmabAttachments.GATE.getOrCreate(player);
		if (gate.lost().isEmpty() || souls(player) <= 0) {
			return false;
		}
		BARGAINS.put(player.getUUID(), new Bargain(player.level().getGameTime(), true));
		send(player, gate);
		return true;
	}

	/** Le marché tient-il encore la visite ? Passé deux minutes, la Vérité le règle d'elle-même. */
	public static boolean holds(ServerPlayer player) {
		Bargain bargain = BARGAINS.get(player.getUUID());
		if (bargain == null || !bargain.open()) {
			return false;
		}
		if (player.level().getGameTime() - bargain.since() > PATIENCE) {
			settle(player);
			FmabNetwork.send(player, BargainPayload.CLOSE);
			return false;
		}
		return true;
	}

	/** Le marché a-t-il été réglé pendant cette visite ? Il ne se rouvre pas. */
	public static boolean settled(ServerPlayer player) {
		Bargain bargain = BARGAINS.get(player.getUUID());
		return bargain != null && !bargain.open();
	}

	/** La visite a passé le marché : on l'oublie, la prochaine pourra en proposer un autre. */
	public static void forget(ServerPlayer player) {
		BARGAINS.remove(player.getUUID());
	}

	private static void settle(ServerPlayer player) {
		BARGAINS.computeIfPresent(player.getUUID(), (id, bargain) -> new Bargain(bargain.since(), false));
	}

	/** L'alchimiste rachète une partie, ou (partie vide) s'en va. */
	public static void answer(ServerPlayer player, String partName) {
		if (!holds(player)) {
			return;
		}
		Optional<BodyPart> part = part(partName);
		GateState gate = FmabAttachments.GATE.getOrCreate(player);
		if (part.isEmpty() || !gate.lost().contains(part.get())) {
			settle(player);
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
		GateState after = FmabAttachments.GATE.getOrCreate(player);
		if (after.lost().isEmpty() || souls(player) <= 0) {
			settle(player);
			player.sendSystemMessage(Component.translatable("truth.fmab.bargain.done")
					.withStyle(s -> s.withItalic(true).withColor(0x9A9A9A)));
			FmabNetwork.send(player, BargainPayload.CLOSE);
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
		FmabNetwork.send(player, new BargainPayload(parts, costs, souls(player)));
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
