package com.ajustor.fmab.progress;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.Training;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.stone.LivingStone;
import com.ajustor.fmab.training.Trial;
import com.ajustor.fmab.xing.Alkahestry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/**
 * Les étapes du chemin de l'alchimiste, qui débloquent les progrès ({@code data/fmab/advancement}).
 * La plupart se lisent dans l'état du joueur, vérifié de temps en temps (rang, armure, automail,
 * homonculus vus tomber…) ; les événements ponctuels (sceau ouvert, point de sang scellé) passent
 * par {@link #reach}.
 */
public final class Milestones {
	public static final MilestoneTrigger TRIGGER = Registry.register(BuiltInRegistries.TRIGGER_TYPES,
			Fmab.id("milestone"), new MilestoneTrigger());
	/** Les sept homonculus, par identifiant de type d'entité. */
	public static final List<String> SINS = List.of("lust", "gluttony", "envy", "greed", "sloth", "wrath", "pride");
	private static final int PERIOD = 40;

	private Milestones() {
	}

	/** Le joueur franchit une étape. Sans effet s'il l'a déjà franchie. */
	public static void reach(ServerPlayer player, String milestone) {
		TRIGGER.trigger(player, milestone);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % PERIOD != 0) {
				return;
			}
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				check(p);
			}
		});
	}

	/** Les étapes qui se lisent dans l'état du joueur. */
	private static void check(ServerPlayer p) {
		AlchemistData alchemist = p.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		if (alchemist.mastery().values().stream().mapToInt(Integer::intValue).sum() > 0) {
			reach(p, "transmuted");
		}
		Rank rank = alchemist.rank();
		if (rank.atLeast(Rank.ALCHEMIST)) {
			reach(p, "rank_alchemist");
		}
		if (rank.atLeast(Rank.STATE)) {
			reach(p, "rank_state");
		}
		if (rank.atLeast(Rank.GATE)) {
			reach(p, "rank_gate");
		}
		Training training = p.getAttachedOrCreate(FmabAttachments.TRAINING);
		if (training.met()) {
			reach(p, "izumi_met");
		}
		if (training.rewarded().size() >= Trial.values().length) {
			reach(p, "izumi_done");
		}
		GateState gate = p.getAttachedOrCreate(FmabAttachments.GATE);
		if (gate.inArmor()) {
			reach(p, "soul_armor");
		}
		if (!p.getAttachedOrCreate(FmabAttachments.AUTOMAIL).limbs().isEmpty()) {
			reach(p, "automail");
		}
		if (LivingStone.souls(p) > 0) {
			reach(p, "living_stone");
		}
		if (Alkahestry.knows(p)) {
			reach(p, "alkahestry");
		}
		if (Boolean.TRUE.equals(p.getAttached(FmabAttachments.BRIGGS_ALLY))) {
			reach(p, "briggs_ally");
		}
		Set<String> slain = p.getAttachedOrCreate(FmabAttachments.SLAIN);
		for (String sin : slain) {
			reach(p, "slain_" + sin);
		}
		if (slain.containsAll(SINS)) {
			reach(p, "seven_sins");
		}
	}
}
