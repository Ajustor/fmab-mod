package com.ajustor.fmab.gate;

import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.SoulGift;
import com.ajustor.fmab.item.PhilosopherStoneItem;
import com.ajustor.fmab.network.FmabNetwork;
import com.ajustor.fmab.network.SoulGiftAnswerPayload;
import com.ajustor.fmab.network.SoulGiftPayload;
import com.ajustor.fmab.platform.event.PlayerEvents;
import com.ajustor.fmab.platform.event.ServerEvents;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.stone.PhilosopherStones;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import com.ajustor.fmab.util.PlayerMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Rendre à un allié ce que la Porte lui a pris, en payant d'une âme : un alchimiste qui a déjà vu sa
 * Porte trace une transmutation humaine, l'allié debout dans le cercle. Il choisit ce qu'il rend et ce
 * qu'il paie : sa propre âme (la Vérité prend tout son corps ; il se réveille dans une armure, comme
 * Alphonse), ou des âmes de la Pierre qu'il tient, au prix de {@link Restoration#cost}. L'allié
 * accepte ; le donneur qui donne son âme confirme une seconde fois.
 *
 * <p>Tout est revérifié au moment de payer, et le paiement n'a lieu qu'une fois ({@link SoulGift}). Rien
 * n'est gardé au-delà d'une minute par étape, ni d'un redémarrage : un rituel interrompu ne coûte rien.
 */
public final class SoulGifts {
	/** Chaque étape attend une minute au plus. */
	private static final int PATIENCE = 20 * 60;
	/** Le donneur reste près de son cercle. */
	private static final double REACH = 8;
	/** L'allié se tient dans le cercle. */
	private static final double INSIDE = 2.5;

	private record Pending(SoulGift gift, ServerPlayer ally, ResourceKey<Level> dimension, BlockPos circle, long since) {
		Pending with(SoulGift value, long now) {
			return new Pending(value, ally, dimension, circle, now);
		}
	}

	/** Les rituels, par donneur. */
	private static final Map<UUID, Pending> PENDING = PlayerMaps.track(new HashMap<>());

	private SoulGifts() {
	}

	public static void register() {
		ServerEvents.END_TICK.register(SoulGifts::tick);
		// L'allié qui s'en va refuse ; le donneur qui s'en va abandonne (PlayerMaps l'oublie).
		PlayerEvents.DISCONNECT.register((player, server) -> {
			for (Map.Entry<UUID, Pending> entry : List.copyOf(PENDING.entrySet())) {
				if (entry.getValue().ally() == player) {
					ServerPlayer donor = server.getPlayerList().getPlayer(entry.getKey());
					if (donor != null) {
						cancel(donor, "gate.fmab.gift.refused");
					}
				}
			}
		});
	}

	/**
	 * La transmutation humaine s'éveille : est-ce ce rituel-là ? Il faut avoir déjà ouvert sa Porte, et
	 * qu'un autre joueur, à qui la Porte a pris quelque chose, se tienne dans le cercle.
	 */
	public static boolean tryRitual(ServerPlayer caster, BlockPos circle) {
		if (FmabAttachments.GATE.getOrCreate(caster).openings() == 0) {
			return false;
		}
		AABB area = new AABB(circle).inflate(INSIDE, 1, INSIDE);
		Optional<ServerPlayer> ally = caster.level().getEntitiesOfClass(ServerPlayer.class, area,
				p -> p != caster && p.isAlive() && !SoulGift.restorable(FmabAttachments.GATE.getOrCreate(p)).isEmpty())
				.stream().findFirst();
		if (ally.isEmpty()) {
			return false;
		}
		start(caster, circle, ally.get());
		return true;
	}

	/** Le rituel commence : le donneur choisit ce qu'il rend à cet allié, et ce qu'il paie. */
	public static void start(ServerPlayer donor, BlockPos circle, ServerPlayer ally) {
		List<BodyPart> restorable = SoulGift.restorable(FmabAttachments.GATE.getOrCreate(ally));
		if (restorable.isEmpty()) {
			donor.sendOverlayMessage(Component.translatable("gate.fmab.gift.nothing_lost"));
			return;
		}
		ServerLevel level = donor.level();
		PENDING.put(donor.getUUID(), new Pending(SoulGift.start(), ally, level.dimension(), circle.immutable(),
				now(level.getServer())));
		FmabNetwork.send(donor, new SoulGiftPayload(SoulGiftPayload.CHOOSE, ally.getName().getString(), "", "",
				restorable.stream().map(SoulOffering::id).toList(), stoneSouls(donor),
				!FmabAttachments.GATE.getOrCreate(donor).soulBound()));
	}

	/** Ce qu'un joueur répond, donneur ou allié. */
	public static void answer(ServerPlayer player, SoulGiftAnswerPayload answer) {
		MinecraftServer server = player.level().getServer();
		Pending own = PENDING.get(player.getUUID());
		if (own != null && answer.step() == SoulGiftPayload.CHOOSE) {
			choose(server, player, own, answer);
			return;
		}
		if (own != null && answer.step() == SoulGiftPayload.CONFIRM) {
			if (answer.accept()) {
				settle(server, player, own.with(own.gift().confirm(), now(server)));
			} else {
				cancel(player, "gate.fmab.gift.withdrawn");
			}
			return;
		}
		if (answer.step() == SoulGiftPayload.PROPOSE) {
			for (Map.Entry<UUID, Pending> entry : List.copyOf(PENDING.entrySet())) {
				Pending pending = entry.getValue();
				ServerPlayer donor = server.getPlayerList().getPlayer(entry.getKey());
				if (pending.ally() != player || donor == null) {
					continue;
				}
				SoulGift answered = pending.gift().answer(answer.accept());
				if (answered.stage() == SoulGift.Stage.CANCELLED) {
					cancel(donor, "gate.fmab.gift.refused");
				} else if (answered.stage() == SoulGift.Stage.CONFIRMING) {
					PENDING.put(donor.getUUID(), pending.with(answered, now(server)));
					player.sendOverlayMessage(Component.translatable("gate.fmab.offer.waiting_donor"));
					FmabNetwork.send(donor, new SoulGiftPayload(SoulGiftPayload.CONFIRM, player.getName().getString(),
							SoulOffering.id(answered.part().orElseThrow()), id(answered.payment().orElseThrow()), List.of(),
							0, true));
				} else {
					settle(server, donor, pending.with(answered, now(server)));
				}
			}
		}
	}

	/** Le donneur a-t-il un rituel en cours ? */
	public static Optional<SoulGift> pending(ServerPlayer donor) {
		return Optional.ofNullable(PENDING.get(donor.getUUID())).map(Pending::gift);
	}

	private static void choose(MinecraftServer server, ServerPlayer donor, Pending pending, SoulGiftAnswerPayload answer) {
		Optional<BodyPart> part = SoulOffering.byId(answer.part());
		Optional<SoulGift.Payment> payment = payment(answer.payment());
		if (!answer.accept() || part.isEmpty() || payment.isEmpty()) {
			cancel(donor, "gate.fmab.gift.withdrawn");
			return;
		}
		SoulGift chosen = pending.gift().choose(part.get(), payment.get(), FmabAttachments.GATE.getOrCreate(pending.ally()),
				FmabAttachments.GATE.getOrCreate(donor), stoneSouls(donor));
		if (chosen == pending.gift()) {
			return;
		}
		PENDING.put(donor.getUUID(), pending.with(chosen, now(server)));
		FmabNetwork.send(pending.ally(), new SoulGiftPayload(SoulGiftPayload.PROPOSE, donor.getName().getString(),
				SoulOffering.id(part.get()), id(payment.get()), List.of(), 0, true));
		donor.sendOverlayMessage(Component.translatable("gate.fmab.gift.asking", pending.ally().getDisplayName()));
	}

	/**
	 * Le paiement : tout est revérifié, puis l'allié retrouve ce qu'il avait perdu et le donneur paie,
	 * dans le même tick. Le rituel est retiré avant de payer : il ne paie qu'une fois.
	 */
	private static void settle(MinecraftServer server, ServerPlayer donor, Pending pending) {
		SoulGift gift = pending.gift();
		if (gift.stage() != SoulGift.Stage.DONE) {
			return;
		}
		PENDING.remove(donor.getUUID());
		ServerLevel level = donor.level();
		ServerPlayer ally = pending.ally();
		BodyPart part = gift.part().orElseThrow();
		SoulGift.Payment payment = gift.payment().orElseThrow();
		GateState donorGate = FmabAttachments.GATE.getOrCreate(donor);
		GateState allyGate = FmabAttachments.GATE.getOrCreate(ally);
		Optional<ItemStack> stone = PhilosopherStones.held(donor);
		Vec3 circle = Vec3.atCenterOf(pending.circle());
		if (!donor.isAlive() || !ally.isAlive() || ally.isRemoved()
				|| level.dimension() != pending.dimension() || ally.level().dimension() != pending.dimension()
				|| donor.position().distanceTo(circle) > REACH || ally.position().distanceTo(circle) > REACH
				|| !SoulGift.restorable(allyGate).contains(part)
				|| !SoulGift.affordable(payment, part, donorGate, stoneSouls(donor))) {
			donor.sendOverlayMessage(Component.translatable("gate.fmab.offer.failed"));
			ally.sendOverlayMessage(Component.translatable("gate.fmab.offer.failed"));
			return;
		}
		if (payment == SoulGift.Payment.STONE) {
			PhilosopherStones.drain(donor, stone.orElseThrow(), Restoration.cost(part));
		}
		PhilosopherStoneItem.restore(ally, allyGate, new Restoration.Plan(EnumSet.of(part), Restoration.cost(part)), false);
		Component what = Component.translatable("gate.fmab.offer.part." + SoulOffering.id(part));
		ally.sendSystemMessage(Component.translatable(payment == SoulGift.Payment.STONE
				? "gate.fmab.gift.restored_stone" : "gate.fmab.gift.restored_soul", donor.getDisplayName(), what));
		TransmutationLightning.discharge(level, pending.circle(), 1.5, 2);
		if (payment == SoulGift.Payment.OWN_SOUL) {
			// Comme Alphonse : la Vérité prend tout le corps, l'âme se réveille dans une armure de fer.
			FmabAttachments.GATE.set(donor, donorGate.pay(BodyPart.BODY));
			SoulArmor.bindFirstTime(donor);
			level.playSound(null, pending.circle(), SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.2f, 0.6f);
			donor.sendSystemMessage(Component.translatable("gate.fmab.gift.gave_soul", ally.getDisplayName(), what));
			donor.sendSystemMessage(Component.translatable("gate.fmab.soul.first_armor"));
			SoulArmor.giveNotes(donor);
		} else {
			donor.sendSystemMessage(Component.translatable("gate.fmab.gift.gave_stone", ally.getDisplayName(), what,
					Restoration.cost(part)));
		}
	}

	private static void cancel(ServerPlayer donor, String reason) {
		Pending pending = PENDING.remove(donor.getUUID());
		if (pending == null) {
			return;
		}
		donor.sendOverlayMessage(Component.translatable(reason));
		if (!"gate.fmab.gift.refused".equals(reason)) {
			pending.ally().sendOverlayMessage(Component.translatable("gate.fmab.offer.withdrawn_soul"));
		}
	}

	/** Une étape qui a trop attendu : le rituel s'éteint, sans rien prendre. */
	private static void tick(MinecraftServer server) {
		long now = now(server);
		for (Map.Entry<UUID, Pending> entry : new ArrayList<>(PENDING.entrySet())) {
			if (now - entry.getValue().since() <= PATIENCE) {
				continue;
			}
			ServerPlayer donor = server.getPlayerList().getPlayer(entry.getKey());
			if (donor != null) {
				cancel(donor, "gate.fmab.offer.too_late");
			} else {
				PENDING.remove(entry.getKey());
			}
		}
	}

	private static long now(MinecraftServer server) {
		return server.overworld().getGameTime();
	}

	private static int stoneSouls(ServerPlayer donor) {
		return PhilosopherStones.held(donor).map(PhilosopherStoneItem::souls).orElse(0);
	}

	public static String id(SoulGift.Payment payment) {
		return payment.name().toLowerCase(Locale.ROOT);
	}

	public static Optional<SoulGift.Payment> payment(String id) {
		for (SoulGift.Payment p : SoulGift.Payment.values()) {
			if (id(p).equals(id)) {
				return Optional.of(p);
			}
		}
		return Optional.empty();
	}
}
