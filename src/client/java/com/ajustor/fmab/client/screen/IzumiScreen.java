package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.network.IzumiActionPayload;
import com.ajustor.fmab.network.OpenIzumiPayload;
import com.ajustor.fmab.training.Trial;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.ajustor.fmab.platform.client.ClientPlatform;

/**
 * Dialogue avec Izumi : ses épreuves (à faire, réussies, récompensées) et le combat
 * d'entraînement. Survoler une épreuve dit comment la réussir.
 */
public class IzumiScreen extends Screen {
	private static final int WIDTH = 300;
	private static final int ROW = 18;
	private static final int PAPER = 0xFFEDE3C8;
	private static final int EDGE = 0xFF6B4F2A;
	private static final int INK = 0xFF2B2B40;
	private static final int FADED = 0xFF8A7A60;
	private static final int DONE = 0xFF2E7D32;
	/** Le fond d'une épreuve survolée. */
	private static final int HOVER = 0x336B4F2A;
	/** La ligne, sous les épreuves, qui invite à les survoler. */
	private static final int HINT_HEIGHT = 12;

	private final OpenIzumiPayload state;
	private int left;
	private int top;
	private int panelHeight;

	public IzumiScreen(OpenIzumiPayload state) {
		super(Component.translatable("entity.fmab.izumi"));
		this.state = state;
	}

	@Override
	protected void init() {
		panelHeight = 70 + Trial.values().length * ROW + HINT_HEIGHT + 30;
		left = (width - WIDTH) / 2;
		top = (height - panelHeight) / 2;
		int y = top + 62;
		for (Trial trial : Trial.values()) {
			boolean achieved = state.achieved().contains(trial.id());
			boolean rewarded = state.rewarded().contains(trial.id());
			if (achieved && !rewarded) {
				addRenderableWidget(Button.builder(Component.translatable("entity.fmab.izumi.report"),
						b -> ClientPlatform.get().send(new IzumiActionPayload(state.entityId(), trial.id())))
						.bounds(left + WIDTH - 90, y - 4, 80, 16).build());
			}
			y += ROW;
		}
		addRenderableWidget(Button.builder(Component.translatable("entity.fmab.izumi.spar"), b -> {
			ClientPlatform.get().send(new IzumiActionPayload(state.entityId(), IzumiActionPayload.SPAR));
			onClose();
		}).bounds(left + WIDTH / 2 - 75, top + panelHeight - 26, 150, 18).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 3, top - 3, left + WIDTH + 3, top + panelHeight + 3, EDGE);
		graphics.fill(left, top, left + WIDTH, top + panelHeight, PAPER);
		graphics.centeredText(font, title, left + WIDTH / 2, top + 6, INK);
		int y = top + 20;
		for (FormattedCharSequence line : font.split(Component.translatable("entity.fmab.izumi.greeting"), WIDTH - 20)) {
			graphics.text(font, line, left + 10, y, FADED, false);
			y += 10;
		}
		y = top + 62;
		Trial hovered = null;
		for (Trial trial : Trial.values()) {
			boolean achieved = state.achieved().contains(trial.id());
			boolean rewarded = state.rewarded().contains(trial.id());
			if (mouseX >= left + 4 && mouseX < left + WIDTH - 4 && mouseY >= y - 5 && mouseY < y + ROW - 5) {
				hovered = trial;
				graphics.fill(left + 4, y - 5, left + WIDTH - 4, y + ROW - 5, HOVER);
			}
			String mark = rewarded ? "★ " : achieved ? "✔ " : "◻ ";
			graphics.text(font, Component.literal(mark).append(Component.translatable(trial.translationKey())),
					left + 10, y, rewarded || achieved ? DONE : INK, false);
			y += ROW;
		}
		graphics.text(font, Component.translatable("entity.fmab.izumi.hover"), left + 10, y, FADED, false);
		super.extractRenderState(graphics, mouseX, mouseY, a);
		if (hovered != null) {
			// Ce qu'Izumi dit de l'épreuve survolée : comment la réussir.
			graphics.setTooltipForNextFrame(font.split(Component.translatable(hovered.hintKey()), 220), mouseX, mouseY);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
