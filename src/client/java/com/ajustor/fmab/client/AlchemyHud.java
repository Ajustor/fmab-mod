package com.ajustor.fmab.client;

import com.ajustor.fmab.client.render.CircleTextures;
import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.registry.FmabAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.item.ItemStack;

/**
 * Le panneau de l'alchimiste, en haut à gauche de l'écran : le cercle sélectionné dans le carnet
 * (miniature et nom), puis, dès que la Vérité a pris quelque chose, une silhouette du corps. Chaque
 * membre y montre son automail et son usure (la barre d'un objet), ou un vide cerclé de rouge s'il
 * manque ; le torse s'assombrit sans organes, un bandeau barre les yeux sans la vue.
 */
final class AlchemyHud {
	private static final int MARGIN = 4;
	private static final int THUMB = 22;
	private static final int BACKDROP = 0x90000000;
	private static final int INK = 0xFFF0E6C8;
	private static final int SKIN = 0xC0D8B890;
	private static final int WOUND = 0xC08A2A2A;
	private static final int LOST = 0xFFB0201A;
	private static final int BROKEN = 0x80FF2020;
	/** Largeur du nom du cercle, au-delà on le coupe. */
	private static final int NAME_WIDTH = 120;

	private AlchemyHud() {
	}

	static void draw(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		int y = MARGIN;
		NotebookContents notebook = Notebooks.of(mc.player);
		if (!notebook.pages().isEmpty()) {
			NotebookContents.Page page = notebook.pages().get(notebook.selected());
			graphics.fill(MARGIN - 2, y - 2, MARGIN + THUMB + 2, y + THUMB + 2, BACKDROP);
			graphics.blit(RenderPipelines.GUI_TEXTURED, CircleTextures.get(page.drawing(), INK), MARGIN, y, 0, 0, THUMB,
					THUMB, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE);
			String name = mc.font.plainSubstrByWidth(Notebooks.pageName(page).getString(), NAME_WIDTH);
			graphics.text(mc.font, name, MARGIN + THUMB + 6, y + (THUMB - 8) / 2, INK, true);
			y += THUMB + 8;
		}
		body(graphics, mc, MARGIN, y);
	}

	/** La silhouette, vue de face : le bras droit à gauche de l'écran, comme dans l'inventaire. */
	private static void body(GuiGraphicsExtractor graphics, Minecraft mc, int x, int y) {
		GateState gate = mc.player.getAttached(FmabAttachments.GATE);
		Automail automail = mc.player.getAttached(FmabAttachments.AUTOMAIL);
		gate = gate == null ? GateState.NONE : gate;
		automail = automail == null ? Automail.NONE : automail;
		// Une âme dans une armure n'a plus de corps à montrer ; un corps entier non plus.
		if (gate.soulBound() || gate.lost().isEmpty() && automail.limbs().isEmpty()) {
			return;
		}
		graphics.fill(x - 2, y - 2, x + 44, y + 43, BACKDROP);
		// La tête et le torse.
		graphics.fill(x + 17, y, x + 25, y + 8, SKIN);
		if (gate.lost(BodyPart.SIGHT)) {
			graphics.fill(x + 16, y + 3, x + 26, y + 5, 0xFF202020);
		}
		graphics.fill(x + 16, y + 9, x + 26, y + 24, gate.lost(BodyPart.ORGANS) ? WOUND : SKIN);
		limb(graphics, mc, gate, automail, BodyPart.RIGHT_ARM, x, y + 8);
		limb(graphics, mc, gate, automail, BodyPart.LEFT_ARM, x + 26, y + 8);
		limb(graphics, mc, gate, automail, BodyPart.RIGHT_LEG, x + 5, y + 25);
		limb(graphics, mc, gate, automail, BodyPart.LEFT_LEG, x + 21, y + 25);
	}

	/** Un membre dans sa case de 16 sur 16 : automail et usure, membre manquant, ou membre de chair. */
	private static void limb(GuiGraphicsExtractor graphics, Minecraft mc, GateState gate, Automail automail,
			BodyPart part, int x, int y) {
		ItemStack piece = automail.get(part);
		int top = part.arm() ? y + 1 : y;
		int height = part.arm() ? 14 : 16;
		if (!piece.isEmpty()) {
			graphics.item(piece, x, y);
			graphics.itemDecorations(mc.font, piece, x, y);
			if (Automails.broken(piece)) {
				graphics.fill(x, y, x + 16, y + 16, BROKEN);
			}
		} else if (gate.lost(part)) {
			graphics.outline(x + 5, top, 6, height, LOST);
		} else {
			graphics.fill(x + 6, top, x + 10, top + height, SKIN);
		}
	}
}
