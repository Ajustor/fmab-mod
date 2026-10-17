package com.ajustor.fmab.progress;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.Training;
import com.ajustor.fmab.promised.NationalCircle;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.stone.LivingStone;
import com.ajustor.fmab.training.Trial;
import com.ajustor.fmab.xing.Alkahestry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;
import com.ajustor.fmab.platform.Platform;
import com.ajustor.fmab.platform.event.ServerEvents;

/**
 * Les étapes du chemin de l'alchimiste, qui débloquent les progrès ({@code data/fmab/advancement}).
 * La plupart se lisent dans l'état du joueur, vérifié de temps en temps (rang, armure, automail,
 * homonculus vus tomber…) ; les événements ponctuels (sceau ouvert, point de sang scellé) passent
 * par {@link #reach}.
 */
public final class Milestones {
	public static final MilestoneTrigger TRIGGER = Platform.get().content().register(BuiltInRegistries.TRIGGER_TYPES,
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
		ServerEvents.END_TICK.register(server -> {
			if (server.getTickCount() % PERIOD != 0) {
				return;
			}
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				check(p);
			}
		});
	}

	/**
	 * Les sept péchés sont soumis : chacun vu tomber, sauf Greed, qu'un pacte suffit à soumettre (on
	 * n'a pas à trahir son allié pour ce progrès).
	 */
	public static boolean sevenSins(Set<String> slain, boolean greedPact) {
		return SINS.stream().allMatch(sin -> slain.contains(sin) || sin.equals("greed") && greedPact);
	}

	/** Les étapes qui se lisent dans l'état du joueur. */
	private static void check(ServerPlayer p) {
		AlchemistData alchemist = FmabAttachments.ALCHEMIST.getOrCreate(p);
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
		Training training = FmabAttachments.TRAINING.getOrCreate(p);
		if (training.met()) {
			reach(p, "izumi_met");
		}
		if (training.rewarded().size() >= Trial.values().length) {
			reach(p, "izumi_done");
		}
		GateState gate = FmabAttachments.GATE.getOrCreate(p);
		if (gate.inArmor()) {
			reach(p, "soul_armor");
		}
		if (!FmabAttachments.AUTOMAIL.getOrCreate(p).limbs().isEmpty()) {
			reach(p, "automail");
		}
		if (LivingStone.souls(p) > 0) {
			reach(p, "living_stone");
		}
		if (Alkahestry.knows(p)) {
			reach(p, "alkahestry");
		}
		if (Boolean.TRUE.equals(FmabAttachments.BRIGGS_ALLY.get(p))) {
			reach(p, "briggs_ally");
		}
		boolean greedPact = Boolean.TRUE.equals(FmabAttachments.GREED_PACT.get(p));
		if (greedPact) {
			reach(p, "greed_pact");
		}
		if (Boolean.TRUE.equals(FmabAttachments.SCAR_ARM.get(p))) {
			reach(p, "scar_tattoo");
		}
		Set<String> slain = FmabAttachments.SLAIN.getOrCreate(p);
		for (String sin : slain) {
			reach(p, "slain_" + sin);
		}
		if (sevenSins(slain, greedPact)) {
			reach(p, "seven_sins");
		}
		if (NationalCircle.get(p.level().getServer()).broken()) {
			reach(p, "circle_broken");
		}
	}
}
