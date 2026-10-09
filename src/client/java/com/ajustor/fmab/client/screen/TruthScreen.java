package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.network.OpenTruthPayload;
import com.ajustor.fmab.network.TruthChoicePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * La Vérité face à une âme sans corps : elle s'en moque, puis propose deux issues. Être rappelée
 * dans un de ses sceaux, ou repartir de zéro (et l'écran dit ce que coûte ce choix sur ce serveur).
 * Repartir de zéro demande une confirmation.
 */
public class TruthScreen extends Screen {
	private static final int WIDTH = 300;
	private static final int HEIGHT = 150;
	private static final int PAPER = 0xFFF6F6F2;
	private static final int EDGE = 0xFFB0B0AA;
	private static final int INK = 0xFF2A2A2E;
	private static final int FADED = 0xFF8A8A84;
	private static final int WARN = 0xFFA8321E;

	private final OpenTruthPayload state;
	private boolean confirming;
	private int left;
	private int top;

	public TruthScreen(OpenTruthPayload state) {
		super(Component.translatable("entity.fmab.truth"));
		this.state = state;
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		int y = top + HEIGHT - 28;
		if (!confirming) {
			addRenderableWidget(Button.builder(Component.translatable("truth.fmab.choice.recall"), b -> {
				ClientPlayNetworking.send(new TruthChoicePayload(state.entityId(), false));
				onClose();
			}).bounds(left + 10, y, 135, 20).build());
			addRenderableWidget(Button.builder(Component.translatable("truth.fmab.choice.restart"), b -> {
				confirming = true;
				rebuildWidgets();
			}).bounds(left + WIDTH - 145, y, 135, 20).build());
		} else {
			addRenderableWidget(Button.builder(Component.translatable("truth.fmab.choice.confirm"), b -> {
				ClientPlayNetworking.send(new TruthChoicePayload(state.entityId(), true));
				onClose();
			}).bounds(left + 10, y, 135, 20).build());
			addRenderableWidget(Button.builder(Component.translatable("truth.fmab.choice.back"), b -> {
				confirming = false;
				rebuildWidgets();
			}).bounds(left + WIDTH - 145, y, 135, 20).build());
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, EDGE);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, PAPER);
		graphics.centeredText(font, title, left + WIDTH / 2, top + 8, INK);
		Component text = confirming
				? Component.translatable(state.wipes() ? "truth.fmab.restart_warning_wipe" : "truth.fmab.restart_warning_keep")
				: Component.translatable("truth.fmab.offer");
		int y = top + 26;
		for (FormattedCharSequence line : font.split(text, WIDTH - 24)) {
			graphics.text(font, line, left + 12, y, confirming ? WARN : FADED, false);
			y += 10;
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
