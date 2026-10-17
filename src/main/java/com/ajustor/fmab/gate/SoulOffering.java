package com.ajustor.fmab.gate;

import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.SoulOffer;
import com.ajustor.fmab.network.FmabNetwork;
import com.ajustor.fmab.network.SoulOfferAnswerPayload;
import com.ajustor.fmab.network.SoulOfferPayload;
import com.ajustor.fmab.platform.event.PlayerEvents;
import com.ajustor.fmab.platform.event.ServerEvents;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import com.ajustor.fmab.util.PlayerMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Le geste d'Edward : un alchimiste qui a déjà vu sa Porte trace une transmutation humaine sous une
 * armure, et paie d'un morceau de lui-même pour y fixer l'âme d'un autre. Le membre paie le rituel :
 * il ne va pas à l'âme, ne lui rend rien.
 *
 * <p>Le donneur choisit ce qu'il offre ; les âmes qui errent devant la Porte sont sollicitées de la
 * plus ancienne à la plus récente, chacune voit qui lui offre quoi et accepte ou refuse ; quand l'une
 * accepte, le donneur la voit et confirme une seconde fois. Tout est revérifié au moment de payer, et
 * le paiement n'a lieu qu'une fois ({@link SoulOffer}). Rien n'est gardé au-delà d'une minute par
 * étape, ni d'un redémarrage : un rituel interrompu ne coûte rien.
 */
public final class SoulOffering {
	/** Chaque étape attend une minute au plus. */
	private static final int PATIENCE = 20 * 60;
	/** Le donneur reste près de son cercle. */
	private static final double REACH = 8;

	/**
	 * Un rituel en cours.
	 *
	 * @param souls les âmes qui erraient quand le cercle s'est éveillé, de la plus ancienne à la plus
	 *              récente
	 * @param since le moment où l'étape en cours a commencé
	 */
	private record Pending(SoulOffer offer, ResourceKey<Level> dimension, BlockPos circle, List<ServerPlayer> souls,
			long since) {
		Pending with(SoulOffer value, long now) {
			return new Pending(value, dimension, circle, souls, now);
		}

		@Nullable ServerPlayer recipient() {
			return offer.recipient().flatMap(id -> souls.stream().filter(s -> s.getUUID().equals(id)).findFirst())
					.orElse(null);
		}
	}

	/** Les rituels, par donneur. */
	private static final Map<UUID, Pending> PENDING = PlayerMaps.track(new HashMap<>());

	private SoulOffering() {
	}

	public static void register() {
		ServerEvents.END_TICK.register(SoulOffering::tick);
		// L'âme sollicitée qui s'en va refuse ; le donneur qui s'en va abandonne (PlayerMaps l'oublie).
		PlayerEvents.DISCONNECT.register((player, server) -> {
			for (Map.Entry<UUID, Pending> entry : List.copyOf(PENDING.entrySet())) {
				if (player == entry.getValue().recipient()) {
					refused(server, entry.getKey(), player);
				}
			}
		});
	}

	/**
	 * La transmutation humaine s'éveille : est-ce ce rituel-là ? Il faut avoir déjà ouvert sa Porte et
	 * avoir posé une armure sur le cercle.
	 */
	public static boolean tryRitual(ServerPlayer caster, BlockPos circle) {
		if (FmabAttachments.GATE.getOrCreate(caster).openings() == 0
				|| SoulBinding.armorOn(caster.level(), circle).isEmpty()) {
			return false;
		}
		start(caster, circle, SoulBinding.waiting(caster.level().getServer()));
		return true;
	}

	/** Le rituel commence : le donneur choisit ce qu'il offre, à ces âmes-là. */
	public static void start(ServerPlayer donor, BlockPos circle, List<ServerPlayer> souls) {
		List<BodyPart> offerable = SoulOffer.offerable(FmabAttachments.GATE.getOrCreate(donor));
		if (offerable.isEmpty()) {
			donor.sendOverlayMessage(Component.translatable("gate.fmab.offer.nothing_left"));
			return;
		}
		if (souls.isEmpty()) {
			donor.sendOverlayMessage(Component.translatable("gate.fmab.soul.nobody"));
			return;
		}
		ServerLevel level = donor.level();
		PENDING.put(donor.getUUID(), new Pending(SoulOffer.start(), level.dimension(), circle.immutable(),
				List.copyOf(souls), level.getServer().overworld().getGameTime()));
		FmabNetwork.send(donor, new SoulOfferPayload(SoulOfferPayload.CHOOSE, "", "",
				offerable.stream().map(SoulOffering::id).toList()));
	}

	/** Ce qu'un joueur répond, donneur ou âme sollicitée. */
	public static void answer(ServerPlayer player, SoulOfferAnswerPayload answer) {
		MinecraftServer server = player.level().getServer();
		Pending own = PENDING.get(player.getUUID());
		if (own != null && answer.step() == SoulOfferPayload.CHOOSE) {
			choose(server, player, own, answer);
			return;
		}
		if (own != null && answer.step() == SoulOfferPayload.CONFIRM) {
			if (answer.accept()) {
				pay(server, player, own);
			} else {
				cancel(player, "gate.fmab.offer.withdrawn");
			}
			return;
		}
		if (answer.step() == SoulOfferPayload.PROPOSE) {
			for (Map.Entry<UUID, Pending> entry : List.copyOf(PENDING.entrySet())) {
				if (entry.getValue().recipient() == player) {
					if (answer.accept()) {
						accepted(server, entry.getKey(), player);
					} else {
						refused(server, entry.getKey(), player);
					}
				}
			}
		}
	}

	/** Le donneur a-t-il un rituel en cours ? */
	public static Optional<SoulOffer> pending(ServerPlayer donor) {
		return Optional.ofNullable(PENDING.get(donor.getUUID())).map(Pending::offer);
	}

	private static void choose(MinecraftServer server, ServerPlayer donor, Pending pending, SoulOfferAnswerPayload answer) {
		Optional<BodyPart> part = byId(answer.part());
		if (!answer.accept() || part.isEmpty()) {
			cancel(donor, "gate.fmab.offer.withdrawn");
			return;
		}
		SoulOffer chosen = pending.offer().choose(part.get(), FmabAttachments.GATE.getOrCreate(donor));
		if (chosen == pending.offer()) {
			return;
		}
		PENDING.put(donor.getUUID(), pending.with(chosen, server.overworld().getGameTime()));
		askNext(server, donor.getUUID());
	}

	/** Sollicite la prochaine âme qui erre toujours ; s'il n'en reste pas, le rituel s'éteint. */
	private static void askNext(MinecraftServer server, UUID donorId) {
		Pending pending = PENDING.get(donorId);
		ServerPlayer donor = server.getPlayerList().getPlayer(donorId);
		if (pending == null || donor == null) {
			return;
		}
		List<UUID> waiting = pending.souls().stream()
				.filter(s -> !s.isRemoved() && SoulBinding.adrift(s))
				.map(ServerPlayer::getUUID)
				.toList();
		Optional<UUID> next = pending.offer().next(waiting);
		if (next.isEmpty()) {
			cancel(donor, "gate.fmab.offer.nobody_accepted");
			return;
		}
		Pending asking = pending.with(pending.offer().ask(next.get()), server.overworld().getGameTime());
		PENDING.put(donorId, asking);
		ServerPlayer soul = asking.recipient();
		FmabNetwork.send(soul, new SoulOfferPayload(SoulOfferPayload.PROPOSE, donor.getName().getString(),
				id(asking.offer().part().orElseThrow()), List.of()));
		donor.sendOverlayMessage(Component.translatable("gate.fmab.offer.asking"));
	}

	private static void accepted(MinecraftServer server, UUID donorId, ServerPlayer soul) {
		Pending pending = PENDING.get(donorId);
		ServerPlayer donor = server.getPlayerList().getPlayer(donorId);
		if (pending == null || donor == null) {
			return;
		}
		PENDING.put(donorId, pending.with(pending.offer().answer(soul.getUUID(), true), server.overworld().getGameTime()));
		soul.sendOverlayMessage(Component.translatable("gate.fmab.offer.waiting_donor"));
		FmabNetwork.send(donor, new SoulOfferPayload(SoulOfferPayload.CONFIRM, soul.getName().getString(),
				id(pending.offer().part().orElseThrow()), List.of()));
	}

	private static void refused(MinecraftServer server, UUID donorId, ServerPlayer soul) {
		Pending pending = PENDING.get(donorId);
		if (pending == null) {
			return;
		}
		PENDING.put(donorId, pending.with(pending.offer().answer(soul.getUUID(), false), server.overworld().getGameTime()));
		askNext(server, donorId);
	}

	/**
	 * La seconde confirmation : tout est revérifié, puis le membre est pris et l'âme fixée, dans le même
	 * tick. Le rituel est retiré avant de payer : il ne paie qu'une fois.
	 */
	private static void pay(MinecraftServer server, ServerPlayer donor, Pending pending) {
		SoulOffer done = pending.offer().confirm();
		if (done.stage() != SoulOffer.Stage.DONE) {
			return;
		}
		PENDING.remove(donor.getUUID());
		ServerLevel level = donor.level();
		BodyPart part = done.part().orElseThrow();
		ServerPlayer soul = pending.recipient();
		Optional<SoulBinding.Armor> armor = SoulBinding.armorOn(level, pending.circle());
		GateState gate = FmabAttachments.GATE.getOrCreate(donor);
		if (!donor.isAlive() || level.dimension() != pending.dimension()
				|| donor.position().distanceTo(Vec3.atCenterOf(pending.circle())) > REACH
				|| !SoulOffer.offerable(gate).contains(part)
				|| armor.isEmpty() || !armor.get().present()
				|| soul == null || soul.isRemoved() || !SoulBinding.adrift(soul)) {
			donor.sendOverlayMessage(Component.translatable("gate.fmab.offer.failed"));
			if (soul != null) {
				soul.sendOverlayMessage(Component.translatable("gate.fmab.offer.failed"));
			}
			return;
		}
		FmabAttachments.GATE.set(donor, gate.pay(part));
		SoulBinding.bind(soul, level, pending.circle(), armor.get());
		Vec3 at = Vec3.atCenterOf(pending.circle());
		level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y, at.z, 60, 1.2, 0.3, 1.2, 0.05);
		TransmutationLightning.discharge(level, pending.circle(), 1.5, 2);
		level.playSound(null, pending.circle(), SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.2f, 0.7f);
		donor.sendSystemMessage(Component.translatable("gate.fmab.offer.paid",
				Component.translatable("gate.fmab.offer.part." + id(part)), soul.getDisplayName()));
		soul.sendSystemMessage(Component.translatable("gate.fmab.offer.bound", donor.getDisplayName()));
	}

	private static void cancel(ServerPlayer donor, String reason) {
		Pending pending = PENDING.remove(donor.getUUID());
		if (pending == null) {
			return;
		}
		donor.sendOverlayMessage(Component.translatable(reason));
		ServerPlayer soul = pending.recipient();
		if (soul != null) {
			soul.sendOverlayMessage(Component.translatable("gate.fmab.offer.withdrawn_soul"));
		}
	}

	/** Une étape qui a trop attendu : la sollicitation passe à l'âme suivante, le reste s'éteint. */
	private static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		for (Map.Entry<UUID, Pending> entry : new ArrayList<>(PENDING.entrySet())) {
			Pending pending = entry.getValue();
			if (now - pending.since() <= PATIENCE) {
				continue;
			}
			ServerPlayer soul = pending.recipient();
			if (pending.offer().stage() == SoulOffer.Stage.ASKING && soul != null) {
				refused(server, entry.getKey(), soul);
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

	public static String id(BodyPart part) {
		return part.name().toLowerCase(Locale.ROOT);
	}

	public static Optional<BodyPart> byId(String id) {
		return SoulOffer.OFFERABLE.stream().filter(p -> id(p).equals(id)).findFirst();
	}
}
