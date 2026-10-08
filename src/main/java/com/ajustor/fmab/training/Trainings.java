package com.ajustor.fmab.training;

import com.ajustor.fmab.alchemy.knowledge.KnowledgeNode;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Training;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.CircleFrame;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Collection;
import java.util.Map;

/** Constater les épreuves réussies et les récompenser. */
public final class Trainings {
	private Trainings() {
	}

	/** Une transmutation a réussi : quelles épreuves vient-elle de remplir ? */
	public static void onTransmutation(ServerPlayer player, Analysis analysis, CircleFrame frame, boolean inscribed,
			Collection<String> effects) {
		if (effects.contains("fmab:wall")) {
			achieve(player, Trial.FIRST_WALL);
		}
		if (!frame.onFloor()) {
			achieve(player, Trial.SURFACE);
		}
		if (analysis.parsed().stages().size() >= 2) {
			achieve(player, Trial.TWO_STAGES);
		}
		if (!inscribed) {
			achieve(player, Trial.GLOVES);
		}
	}

	/** Une épreuve est réussie ; elle ne compte qu'une fois le joueur présenté à Izumi. */
	public static void achieve(ServerPlayer player, Trial trial) {
		Training training = player.getAttachedOrCreate(FmabAttachments.TRAINING);
		if (!training.met() || training.achieved().contains(trial.id())) {
			return;
		}
		player.setAttached(FmabAttachments.TRAINING, training.achieve(trial.id()));
		player.sendSystemMessage(Component.translatable("trial.fmab.achieved", Component.translatable(trial.translationKey())));
	}

	/** Rendre compte d'une épreuve réussie à Izumi : la récompense est accordée une fois. */
	public static boolean claim(ServerPlayer player, Trial trial) {
		Training training = player.getAttachedOrCreate(FmabAttachments.TRAINING);
		if (!training.achieved().contains(trial.id()) || training.rewarded().contains(trial.id())) {
			return false;
		}
		AlchemistData data = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
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
		player.setAttached(FmabAttachments.ALCHEMIST, data);
		player.setAttached(FmabAttachments.TRAINING, training.reward(trial.id()));
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1, 1.2f);
		return true;
	}
}
