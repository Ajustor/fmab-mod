package com.ajustor.fmab.entity.character;

import com.ajustor.fmab.network.CinematicPayload;
import com.ajustor.fmab.progress.Milestones;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shou Tucker, démasqué. Le BRIEF veut que Nina passe par le récit, pas par une mécanique : à sa
 * dernière réplique, une cinématique dit ce qu'il a fait de sa fille et de son chien, l'armée vient
 * l'arrêter, et il n'y a plus de Tucker à la bibliothèque.
 */
final class Tucker {
	private Tucker() {
	}

	static void unmasked(StoryCharacterEntity tucker, ServerPlayer by) {
		if (!(tucker.level() instanceof ServerLevel level)) {
			return;
		}
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, tucker.getBoundingBox().inflate(24))) {
			CinematicPayload.play(p, CinematicPayload.NINA, CinematicPayload.NINA_TICKS);
			Milestones.reach(p, "nina");
			p.sendSystemMessage(Component.translatable("npc.fmab.tucker.arrested").withStyle(ChatFormatting.DARK_RED));
		}
		tucker.discard();
	}
}
