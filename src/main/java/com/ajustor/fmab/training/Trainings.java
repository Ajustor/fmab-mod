package com.ajustor.fmab.training;

import net.minecraft.world.item.ItemStack;
import com.ajustor.fmab.item.Tomes;
import com.ajustor.fmab.alchemy.knowledge.KnowledgeNode;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Training;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.CircleFrame;
import com.ajustor.fmab.state.StateExam;
import com.ajustor.fmab.transmutation.Transmutation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.ajustor.fmab.compat.EntityCompat;

/** Constater les épreuves réussies et les récompenser. */
public final class Trainings {
	/** Les épreuves déjà signalées comme ne comptant pas encore, par joueur (le temps d'une session). */
	private static final Set<String> UNCOUNTED = ConcurrentHashMap.newKeySet();

	private Trainings() {
	}

	/**
	 * Une transmutation a réussi : quelles épreuves vient-elle de remplir ? L'épreuve du gant se
	 * constate au gant lui-même ({@code GloveCasting}) : des mains jointes ou un tatouage transmutent
	 * eux aussi sans tracer, mais sans gant.
	 */
	public static void onTransmutation(ServerPlayer player, Analysis analysis, CircleFrame frame,
			Collection<String> effects) {
		if (effects.contains("fmab:wall")) {
			achieve(player, Trial.FIRST_WALL);
		}
		if (!frame.onFloor()) {
			achieve(player, Trial.SURFACE);
		}
		if (analysis.parsed().stages().size() >= 2) {
			StateExam.proveTwoStages(player);
		}
	}

	/** Une épreuve est réussie ; elle ne compte qu'une fois le joueur présenté à Izumi. */
	public static void achieve(ServerPlayer player, Trial trial) {
		Training training = FmabAttachments.TRAINING.getOrCreate(player);
		if (!training.met()) {
			// Qu'il sache pourquoi rien ne se passe : une fois par épreuve et par session.
			if (UNCOUNTED.add(player.getUUID() + "/" + trial.id())) {
				player.sendSystemMessage(Component.translatable("trial.fmab.not_student"));
			}
			return;
		}
		if (training.achieved().contains(trial.id())) {
			return;
		}
		FmabAttachments.TRAINING.set(player, training.achieve(trial.id()));
		player.sendSystemMessage(Component.translatable("trial.fmab.achieved", Component.translatable(trial.translationKey())));
	}

	/** Rendre compte d'une épreuve réussie à Izumi : la récompense est accordée une fois. */
	public static boolean claim(ServerPlayer player, Trial trial) {
		Training training = FmabAttachments.TRAINING.getOrCreate(player);
		if (!training.achieved().contains(trial.id()) || training.rewarded().contains(trial.id())) {
			return false;
		}
		AlchemistData data = FmabAttachments.ALCHEMIST.getOrCreate(player);
		for (Map.Entry<String, Integer> m : trial.mastery().entrySet()) {
			data = data.addMastery(m.getKey(), m.getValue());
		}
		AlchemyRules rules = AlchemyRules.of(player.level().registryAccess());
		for (String node : trial.nodes()) {
			data = data.grant(node);
			rules.nodes().stream().filter(n -> n.id().equals(node)).map(KnowledgeNode::nameKey).findFirst()
					.ifPresent(key -> player.sendSystemMessage(Component.translatable("knowledge.fmab.unlocked",
							Component.translatable(key))));
		}
		FmabAttachments.ALCHEMIST.set(player, Transmutation.promote(player, data));
		trial.tome().ifPresent(t -> {
			ItemStack tome = Tomes.stack(t);
			if (!player.getInventory().add(tome)) {
				EntityCompat.drop(player, tome);
			}
		});
		FmabAttachments.TRAINING.set(player, training.reward(trial.id()));
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1, 1.2f);
		return true;
	}
}
