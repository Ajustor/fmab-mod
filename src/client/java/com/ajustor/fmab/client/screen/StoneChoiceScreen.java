package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.gate.BeingKind;
import com.ajustor.fmab.network.StoneChoicePayload;
import com.ajustor.fmab.network.StoneChosenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Une transmutation humaine, Pierre philosophale en main : ouvrir sa Porte, ou donner une âme de la
 * Pierre à l'être qui naît du cercle, dont on choisit ensuite la forme. Fermer l'écran laisse le
 * cercle s'éteindre.
 */
public class StoneChoiceScreen extends Screen {
	private static final int WIDTH = 300;
	private static final int HEIGHT = 168;
	private static final int PAPER = 0xFF1E1012;
	private static final int EDGE = 0xFFB0201A;
	private static final int INK = 0xFFF0D8D8;
	private static final int FADED = 0xFFB89090;

	private final StoneChoicePayload state;
	private boolean answered;
	/** Second temps : la forme de l'être. */
	private boolean choosingForm;
	private int left;
	private int top;

	public StoneChoiceScreen(StoneChoicePayload state) {
		super(Component.translatable("gate.fmab.choice.title"));
		this.state = state;
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		int y = top + HEIGHT - 28;
		if (!choosingForm) {
			addRenderableWidget(Button.builder(Component.translatable("gate.fmab.choice.gate"), b -> answer("gate"))
					.bounds(left + 10, y, 135, 20).build());
			Button being = addRenderableWidget(Button.builder(Component.translatable("gate.fmab.choice.being"),
					b -> chooseForm()).bounds(left + WIDTH - 145, y, 135, 20).build());
			being.active = state.souls() > 0;
			return;
		}
		// La forme du corps : deux colonnes de boutons, et de quoi revenir en arrière.
		BeingKind[] kinds = BeingKind.values();
		for (int i = 0; i < kinds.length; i++) {
			BeingKind kind = kinds[i];
			int bx = left + 10 + (i % 2) * 145;
			int by = top + 64 + (i / 2) * 22;
			addRenderableWidget(Button.builder(Component.translatable(kind.translationKey()),
					b -> answer("being:" + kind.id())).bounds(bx, by, 135, 20).build());
		}
		addRenderableWidget(Button.builder(Component.translatable("gate.fmab.choice.back"), b -> {
			choosingForm = false;
			rebuildWidgets();
		}).bounds(left + WIDTH / 2 - 50, y + 4, 100, 20).build());
	}

	/** Passe au choix de la forme, comme le bouton « Créer un être ». */
	public void chooseForm() {
		choosingForm = true;
		rebuildWidgets();
	}

	private void answer(String choice) {
		answered = true;
		ClientPlayNetworking.send(new StoneChosenPayload(choice));
		onClose();
	}

	@Override
	public void removed() {
		if (!answered) {
			ClientPlayNetworking.send(new StoneChosenPayload(""));
		}
		super.removed();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, EDGE);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, PAPER);
		graphics.centeredText(font, title, left + WIDTH / 2, top + 8, INK);
		int y = top + 26;
		Component text = choosingForm ? Component.translatable("gate.fmab.choice.form")
				: Component.translatable("gate.fmab.choice.text", state.souls());
		for (FormattedCharSequence line : font.split(text, WIDTH - 24)) {
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
