package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.network.StoneRadiusChosenPayload;
import com.ajustor.fmab.network.StoneRadiusPayload;
import com.ajustor.fmab.platform.client.ClientPlatform;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Le cercle de la Pierre philosophale s'éveille : l'alchimiste choisit jusqu'où il porte, de quelques
 * blocs à toute une ferme. Fermer l'écran laisse le cercle s'éteindre.
 */
public class StoneRadiusScreen extends Screen {
	private static final int WIDTH = 280;
	private static final int HEIGHT = 130;
	private static final int PAPER = 0xFF1E1012;
	private static final int EDGE = 0xFFB0201A;
	private static final int INK = 0xFFF0D8D8;
	private static final int FADED = 0xFFB89090;

	private final StoneRadiusPayload state;
	private int radius;
	private boolean answered;
	private int left;
	private int top;

	public StoneRadiusScreen(StoneRadiusPayload state) {
		super(Component.translatable("stone.fmab.radius.title"));
		this.state = state;
		this.radius = Math.clamp(state.suggested(), 1, state.max());
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		int max = Math.max(1, state.max());
		addRenderableWidget(new AbstractSliderButton(left + 20, top + 58, WIDTH - 40, 20, Component.empty(),
				(radius - 1) / (double) Math.max(1, max - 1)) {
			{
				updateMessage();
			}

			@Override
			protected void updateMessage() {
				setMessage(Component.translatable("stone.fmab.radius.value", radius));
			}

			@Override
			protected void applyValue() {
				radius = 1 + (int) Math.round(value * (max - 1));
			}
		});
		addRenderableWidget(Button.builder(Component.translatable("stone.fmab.radius.confirm"), b -> answer(radius))
				.bounds(left + 20, top + HEIGHT - 30, 115, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> answer(0))
				.bounds(left + WIDTH - 135, top + HEIGHT - 30, 115, 20).build());
	}

	private void answer(int chosen) {
		answered = true;
		ClientPlatform.get().send(new StoneRadiusChosenPayload(chosen));
		onClose();
	}

	@Override
	public void removed() {
		if (!answered) {
			ClientPlatform.get().send(new StoneRadiusChosenPayload(0));
		}
		super.removed();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, EDGE);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, PAPER);
		graphics.centeredText(font, title, left + WIDTH / 2, top + 8, INK);
		int y = top + 24;
		for (FormattedCharSequence line : font.split(Component.translatable("stone.fmab.radius.text"), WIDTH - 24)) {
			graphics.text(font, line, left + 12, y, FADED, false);
			y += 10;
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
