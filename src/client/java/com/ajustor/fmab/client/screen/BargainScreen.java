package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.network.BargainAnswerPayload;
import com.ajustor.fmab.network.BargainPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Marchander avec la Vérité : chaque partie qu'elle a prise, et son prix en âmes. On rachète ce que
 * les Pierres permettent, puis on s'en va ; fermer l'écran, c'est s'en aller.
 */
public class BargainScreen extends Screen {
	private static final int WIDTH = 260;
	private static final int PAPER = 0xFFF6F6F2;
	private static final int EDGE = 0xFFB0B0AA;
	private static final int INK = 0xFF2A2A2E;
	private static final int FADED = 0xFF8A8A84;
	private static final int RED = 0xFFB0201A;

	private BargainPayload state;
	private boolean leaving;
	private int left;
	private int top;
	private int height2;

	public BargainScreen(BargainPayload state) {
		super(Component.translatable("truth.fmab.bargain.title"));
		this.state = state;
	}

	/** Le marché a changé (une partie rachetée) : on le redessine. */
	public void update(BargainPayload next) {
		state = next;
		rebuildWidgets();
	}

	@Override
	protected void init() {
		height2 = 70 + state.parts().size() * 22 + 28;
		left = (width - WIDTH) / 2;
		top = (height - height2) / 2;
		int y = top + 64;
		for (int i = 0; i < state.parts().size(); i++) {
			String part = state.parts().get(i);
			int cost = state.costs().get(i);
			Button b = addRenderableWidget(Button.builder(Component.translatable("truth.fmab.bargain.part",
							Component.translatable("body.fmab." + part), cost),
					x -> ClientPlayNetworking.send(new BargainAnswerPayload(part)))
					.bounds(left + 20, y, WIDTH - 40, 20).build());
			b.active = cost <= state.souls() || minecraft.player.isCreative();
			y += 22;
		}
		addRenderableWidget(Button.builder(Component.translatable("truth.fmab.bargain.leave"), x -> onClose())
				.bounds(left + WIDTH / 2 - 60, top + height2 - 26, 120, 20).build());
	}

	@Override
	public void removed() {
		if (!leaving) {
			leaving = true;
			ClientPlayNetworking.send(new BargainAnswerPayload(""));
		}
		super.removed();
	}

	/** La Vérité a clos le marché elle-même : rien à lui répondre. */
	public void closeSilently() {
		leaving = true;
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + height2 + 2, EDGE);
		graphics.fill(left, top, left + WIDTH, top + height2, PAPER);
		graphics.centeredText(font, title, left + WIDTH / 2, top + 8, INK);
		int y = top + 24;
		for (FormattedCharSequence line : font.split(Component.translatable("truth.fmab.bargain.text"), WIDTH - 24)) {
			graphics.text(font, line, left + 12, y, FADED, false);
			y += 10;
		}
		graphics.centeredText(font, Component.translatable("truth.fmab.bargain.souls", state.souls()),
				left + WIDTH / 2, top + 50, RED);
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
