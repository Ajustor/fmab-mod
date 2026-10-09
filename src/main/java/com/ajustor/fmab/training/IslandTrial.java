package com.ajustor.fmab.training;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.registry.FmabAttachments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * L'épreuve de l'île de Yock : « un est tout, tout est un ». Izumi y abandonne ses élèves un mois
 * durant, sans rien. Ici : passer un jour entier sur l'île sans la quitter. On compte le temps passé
 * sur l'île ; la quitter remet le compte à zéro.
 */
public final class IslandTrial {
	public static final ResourceKey<Structure> YOCK = ResourceKey.create(Registries.STRUCTURE, Fmab.id("yock_island"));
	/** Un jour de Minecraft, en ticks. */
	public static final int DAY = 24000;
	private static final int PERIOD = 100;

	private IslandTrial() {
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

	private static void check(ServerPlayer player) {
		var training = player.getAttachedOrCreate(FmabAttachments.TRAINING);
		if (!training.met() || training.achieved().contains(Trial.ISLAND.id())) {
			return;
		}
		ServerLevel level = player.level();
		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(YOCK);
		boolean onIsland = structure != null
				&& level.structureManager().getStructureWithPieceAt(player.blockPosition(), structure).isValid();
		Integer before = player.getAttached(FmabAttachments.ISLAND_TIME);
		int time = before == null ? 0 : before;
		if (!onIsland) {
			if (time > 0) {
				player.removeAttached(FmabAttachments.ISLAND_TIME);
				player.sendOverlayMessage(Component.translatable("trial.fmab.island.left"));
			}
			return;
		}
		if (time == 0) {
			player.sendSystemMessage(Component.translatable("trial.fmab.island.arrived"));
		}
		time += PERIOD;
		player.setAttached(FmabAttachments.ISLAND_TIME, time);
		if (time >= DAY) {
			Trainings.achieve(player, Trial.ISLAND);
		}
	}
}
