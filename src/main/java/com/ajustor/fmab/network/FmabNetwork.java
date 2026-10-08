package com.ajustor.fmab.network;

import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.AlchemyRules;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class FmabNetwork {
	private FmabNetwork() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(SaveNotebookPayload.TYPE, SaveNotebookPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(LearnGlyphPayload.TYPE, LearnGlyphPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SaveNotebookPayload.TYPE,
				(payload, context) -> saveNotebook(context.player(), payload.hand(), payload.contents()));
		ServerPlayNetworking.registerGlobalReceiver(LearnGlyphPayload.TYPE,
				(payload, context) -> learn(context.player(), payload.glyph()));
	}

	private static void saveNotebook(ServerPlayer player, InteractionHand hand, NotebookContents contents) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(FmabItems.CIRCLE_NOTEBOOK)) {
			stack.set(FmabComponents.NOTEBOOK, contents);
		}
	}

	/**
	 * On n'apprend un glyphe qu'avec le Traité en main, et seulement s'il est de son rang : le
	 * serveur reste juge de ce que le joueur sait.
	 */
	private static void learn(ServerPlayer player, String glyphId) {
		boolean holdsTreatise = player.getMainHandItem().is(FmabItems.ALCHEMY_TREATISE)
				|| player.getOffhandItem().is(FmabItems.ALCHEMY_TREATISE);
		if (!holdsTreatise) {
			return;
		}
		Optional<Glyph> glyph = AlchemyRules.of(player.level().registryAccess()).glyphs().stream()
				.filter(g -> g.id().equals(glyphId))
				.findFirst();
		AlchemistData data = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		if (glyph.isEmpty() || data.known().contains(glyphId)) {
			return;
		}
		if (!data.rank().atLeast(glyph.get().rank())) {
			player.sendOverlayMessage(Component.translatable("treatise.fmab.rank_too_low",
					Component.translatable(glyph.get().rank().translationKey())));
			return;
		}
		player.setAttached(FmabAttachments.ALCHEMIST, data.learn(glyphId));
		player.sendOverlayMessage(Component.translatable("treatise.fmab.learned",
				Component.translatable(glyph.get().nameKey())));
	}
}
