package com.ajustor.fmab.client;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.client.render.CircleTextures;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.item.InscriptionItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.item.ItemStack;

/**
 * Le cercle sélectionné dans le carnet, en haut à gauche de l'écran (miniature et nom), quand on
 * peut s'en servir : après avoir vu la Porte (les mains jointes le lancent), ou un outil d'inscription
 * en main (craie, peinture, burin, fil ou encre alchimiques).
 */
final class AlchemyHud {
	private static final int MARGIN = 4;
	private static final int THUMB = 22;
	private static final int BACKDROP = 0x90000000;
	private static final int INK = 0xFFF0E6C8;
	/** Largeur du nom du cercle, au-delà on le coupe. */
	private static final int NAME_WIDTH = 120;

	private AlchemyHud() {
	}

	static void draw(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || !usable(player)) {
			return;
		}
		NotebookContents notebook = Notebooks.of(player);
		if (notebook.pages().isEmpty()) {
			return;
		}
		NotebookContents.Page page = notebook.pages().get(notebook.selected());
		graphics.fill(MARGIN - 2, MARGIN - 2, MARGIN + THUMB + 2, MARGIN + THUMB + 2, BACKDROP);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CircleTextures.get(page.drawing(), INK), MARGIN, MARGIN, 0, 0, THUMB,
				THUMB, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE);
		String name = mc.font.plainSubstrByWidth(Notebooks.pageName(page).getString(), NAME_WIDTH);
		graphics.text(mc.font, name, MARGIN + THUMB + 6, MARGIN + (THUMB - 8) / 2, INK, true);
	}

	/** Le cercle sélectionné sert-il en ce moment ? */
	private static boolean usable(LocalPlayer player) {
		AlchemistData data = player.getAttached(FmabAttachments.ALCHEMIST);
		return data != null && data.rank().atLeast(Rank.GATE) || inscribes(player.getMainHandItem())
				|| inscribes(player.getOffhandItem());
	}

	private static boolean inscribes(ItemStack stack) {
		return stack.getItem() instanceof InscriptionItem || stack.is(FmabItems.ALCHEMICAL_THREAD)
				|| stack.is(FmabItems.ALCHEMICAL_INK);
	}
}
