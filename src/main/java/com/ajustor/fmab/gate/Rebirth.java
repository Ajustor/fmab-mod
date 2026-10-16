package com.ajustor.fmab.gate;

import com.ajustor.fmab.FmabConfig;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.ExamProgress;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.Tattoos;
import com.ajustor.fmab.data.Training;
import com.ajustor.fmab.registry.FmabAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Repartir de zéro : une âme lasse d'errer demande à la Vérité de tout effacer. Elle retrouve un
 * corps et rentre au monde. Selon le réglage du serveur, elle perd aussi toute sa progression
 * d'alchimiste (par défaut : oui, pour qu'on ne reste pas coincé indéfiniment devant la Porte en
 * gardant tout).
 */
public final class Rebirth {
	private Rebirth() {
	}

	public static void restart(ServerPlayer player) {
		boolean wipe = FmabConfig.get().restartWipesProgress();
		player.setAttached(FmabAttachments.GATE, GateState.NONE);
		player.removeAttached(FmabAttachments.ADRIFT_SINCE);
		player.setAttached(FmabAttachments.AUTOMAIL, Automail.NONE);
		player.removeAttached(FmabAttachments.LIVING_STONE);
		if (wipe) {
			player.setAttached(FmabAttachments.ALCHEMIST, AlchemistData.NEW);
			player.setAttached(FmabAttachments.TRAINING, Training.NONE);
			player.setAttached(FmabAttachments.EXAM, ExamProgress.NONE);
			player.setAttached(FmabAttachments.TATTOOS, Tattoos.NONE);
			player.setAttached(FmabAttachments.KARMA, 0);
		}
		GateOfTruth.leaveTruth(player);
		ServerLevel home = player.level().getServer().overworld();
		player.teleport(new TeleportTransition(home, Vec3.atBottomCenterOf(home.getRespawnData().pos()), Vec3.ZERO,
				0, 0, TeleportTransition.DO_NOTHING));
		player.setHealth(player.getMaxHealth());
		player.sendSystemMessage(Component.translatable(wipe ? "truth.fmab.reborn_wiped" : "truth.fmab.reborn_kept"));
	}
}
