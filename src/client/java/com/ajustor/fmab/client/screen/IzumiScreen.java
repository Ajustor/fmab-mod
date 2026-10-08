package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.network.IzumiActionPayload;
import com.ajustor.fmab.network.OpenIzumiPayload;
import com.ajustor.fmab.training.Trial;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Dialogue avec Izumi : ses épreuves (à faire, réussies, récompensées) et le combat
 * d'entraînement.
 */
public class IzumiScreen extends Screen {
	private static final int WIDTH = 300;
	private static final int ROW = 20;
	private static final int PAPER = 0xFFEDE3C8;
	private static final int EDGE = 0xFF6B4F2A;
	private static final int INK = 0xFF2B2B40;
	private static final int FADED = 0xFF8A7A60;
	private static final int DONE = 0xFF2E7D32;

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
		panelHeight = 70 + Trial.values().length * ROW + 30;
		left = (width - WIDTH) / 2;
		top = (height - panelHeight) / 2;
		int y = top + 62;
		for (Trial trial : Trial.values()) {
			boolean achieved = state.achieved().contains(trial.id());
			boolean rewarded = state.rewarded().contains(trial.id());
			if (achieved && !rewarded) {
				addRenderableWidget(Button.builder(Component.translatable("entity.fmab.izumi.report"),
						b -> ClientPlayNetworking.send(new IzumiActionPayload(state.entityId(), trial.id())))
						.bounds(left + WIDTH - 90, y - 4, 80, 16).build());
			}
			y += ROW;
		}
		addRenderableWidget(Button.builder(Component.translatable("entity.fmab.izumi.spar"), b -> {
			ClientPlayNetworking.send(new IzumiActionPayload(state.entityId(), IzumiActionPayload.SPAR));
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
		for (Trial trial : Trial.values()) {
			boolean achieved = state.achieved().contains(trial.id());
			boolean rewarded = state.rewarded().contains(trial.id());
			String mark = rewarded ? "★ " : achieved ? "✔ " : "◻ ";
			graphics.text(font, Component.literal(mark).append(Component.translatable(trial.translationKey())),
					left + 10, y, rewarded || achieved ? DONE : INK, false);
			y += ROW;
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
