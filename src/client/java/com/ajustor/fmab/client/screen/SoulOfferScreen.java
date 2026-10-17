package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.network.SoulOfferAnswerPayload;
import com.ajustor.fmab.network.SoulOfferPayload;
import com.ajustor.fmab.platform.client.ClientPlatform;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Le sacrifice d'un membre pour lier une âme à une armure, à l'écran de chacun : le donneur choisit ce
 * qu'il offre, l'âme errante accepte ou refuse, puis le donneur confirme une seconde fois. Fermer
 * l'écran vaut refus : rien n'est payé.
 */
public class SoulOfferScreen extends Screen {
	private static final int WIDTH = 300;
	private static final int HEIGHT = 200;
	private static final int PAPER = 0xFF140C10;
	private static final int EDGE = 0xFF6A1A22;
	private static final int INK = 0xFFF0E0D8;
	private static final int FADED = 0xFFB89A92;
	private static final int WARNING = 0xFFE05040;

	private final SoulOfferPayload state;
	private boolean answered;
	private int left;
	private int top;

	public SoulOfferScreen(SoulOfferPayload state) {
		super(Component.translatable(switch (state.step()) {
			case SoulOfferPayload.PROPOSE -> "gate.fmab.offer.propose.title";
			case SoulOfferPayload.CONFIRM -> "gate.fmab.offer.confirm.title";
			default -> "gate.fmab.offer.choose.title";
		}));
		this.state = state;
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		int y = top + HEIGHT - 28;
		switch (state.step()) {
			case SoulOfferPayload.CHOOSE -> {
				// Une colonne par moitié : ce que le donneur a encore de chair.
				for (int i = 0; i < state.parts().size(); i++) {
					String part = state.parts().get(i);
					int bx = left + 10 + (i % 2) * 145;
					int by = top + 94 + (i / 2) * 22;
					addRenderableWidget(Button.builder(Component.translatable("gate.fmab.offer.part." + part),
							b -> answer(part, true)).bounds(bx, by, 135, 20).build());
				}
				addRenderableWidget(Button.builder(Component.translatable("gate.fmab.offer.renounce"), b -> answer("", false))
						.bounds(left + WIDTH / 2 - 60, y, 120, 20).build());
			}
			case SoulOfferPayload.PROPOSE -> {
				addRenderableWidget(Button.builder(Component.translatable("gate.fmab.offer.accept"), b -> answer("", true))
						.bounds(left + 10, y, 135, 20).build());
				addRenderableWidget(Button.builder(Component.translatable("gate.fmab.offer.refuse"), b -> answer("", false))
						.bounds(left + WIDTH - 145, y, 135, 20).build());
			}
			default -> {
				addRenderableWidget(Button.builder(Component.translatable("gate.fmab.offer.sacrifice"), b -> answer("", true))
						.bounds(left + 10, y, 135, 20).build());
				addRenderableWidget(Button.builder(Component.translatable("gate.fmab.offer.renounce"), b -> answer("", false))
						.bounds(left + WIDTH - 145, y, 135, 20).build());
			}
		}
	}

	/** Ce que répond le joueur, comme ses boutons : une partie offerte (au premier choix), oui ou non. */
	public void answer(String part, boolean accept) {
		answered = true;
		ClientPlatform.get().send(new SoulOfferAnswerPayload(state.step(), part, accept));
		onClose();
	}

	@Override
	public void removed() {
		if (!answered) {
			ClientPlatform.get().send(new SoulOfferAnswerPayload(state.step(), "", false));
		}
		super.removed();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, EDGE);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, PAPER);
		graphics.centeredText(font, title, left + WIDTH / 2, top + 8, INK);
		Component part = Component.translatable("gate.fmab.offer.part." + state.part());
		Component text = switch (state.step()) {
			case SoulOfferPayload.PROPOSE -> Component.translatable("gate.fmab.offer.propose.text", state.other(), part);
			case SoulOfferPayload.CONFIRM -> Component.translatable("gate.fmab.offer.confirm.text", state.other(), part);
			default -> Component.translatable("gate.fmab.offer.choose.text");
		};
		int y = top + 26;
		for (FormattedCharSequence line : font.split(text, WIDTH - 24)) {
			graphics.text(font, line, left + 12, y, FADED, false);
			y += 10;
		}
		if (state.step() == SoulOfferPayload.CONFIRM) {
			graphics.centeredText(font, Component.translatable("gate.fmab.offer.irreversible"), left + WIDTH / 2,
					top + HEIGHT - 44, WARNING);
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
