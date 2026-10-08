package com.ajustor.fmab.client;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.client.render.TransmutationCircleRenderer;
import com.ajustor.fmab.client.screen.NotebookScreen;
import com.ajustor.fmab.client.screen.TreatiseScreen;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class FmabClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Fmab.setClientHooks((kind, hand) -> {
			Minecraft mc = Minecraft.getInstance();
			switch (kind) {
				case NOTEBOOK -> mc.gui.setScreen(new NotebookScreen(hand, mc.player.getItemInHand(hand)));
				case TREATISE -> mc.gui.setScreen(new TreatiseScreen());
			}
		});
		BlockEntityRendererRegistry.register(FmabBlockEntities.TRANSMUTATION_CIRCLE, TransmutationCircleRenderer::new);
		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, Fmab.id("concentration"),
				(graphics, delta) -> concentrationBar(graphics));
	}

	/**
	 * Barre de concentration au-dessus de la faim. Elle ne s'affiche qu'en dessous du maximum :
	 * un alchimiste reposé n'a pas besoin de la voir.
	 */
	private static void concentrationBar(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		AlchemistData data = mc.player.getAttached(FmabAttachments.ALCHEMIST);
		if (data == null || data.concentration() >= AlchemistData.MAX_CONCENTRATION) {
			return;
		}
		int width = 81;
		int x = graphics.guiWidth() / 2 + 10;
		int y = graphics.guiHeight() - 39 - 10 - 4;
		int filled = Math.round(width * data.concentration() / AlchemistData.MAX_CONCENTRATION);
		graphics.fill(x - 1, y - 1, x + width + 1, y + 4, 0xC0000000);
		graphics.fill(x, y, x + filled, y + 3, 0xFF4FA3FF);
	}
}
