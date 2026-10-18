package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.data.SoulGift;
import com.ajustor.fmab.gate.Restoration;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.network.SoulGiftAnswerPayload;
import com.ajustor.fmab.network.SoulGiftPayload;
import com.ajustor.fmab.platform.client.ClientPlatform;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Offrir une âme pour rendre à un allié ce que la Porte lui a pris, à l'écran de chacun : le donneur
 * choisit ce qu'il rend et ce qu'il paie (son âme, ou des âmes de sa Pierre), l'allié accepte ou
 * refuse, puis le donneur qui donne son âme confirme une seconde fois. Fermer l'écran vaut refus : rien
 * n'est payé.
 */
public class SoulGiftScreen extends Screen {
	private static final int WIDTH = 320;
	private static final int ROW = 22;
	private static final int PAPER = 0xFF140C10;
	private static final int EDGE = 0xFF6A1A22;
	private static final int INK = 0xFFF0E0D8;
	private static final int FADED = 0xFFB89A92;
	private static final int WARNING = 0xFFE05040;
	private static final int TEXT_TOP = 26;
	private static final int ROWS_TOP = 84;

	private final SoulGiftPayload state;
	private boolean answered;
	private int left;
	private int top;
	private int panelHeight;

	public SoulGiftScreen(SoulGiftPayload state) {
		super(Component.translatable(switch (state.step()) {
			case SoulGiftPayload.PROPOSE -> "gate.fmab.gift.propose.title";
			case SoulGiftPayload.CONFIRM -> "gate".equals(state.payment())
					? "gate.fmab.gift.confirm_gate.title" : "gate.fmab.gift.confirm.title";
			default -> "gate.fmab.gift.choose.title";
		}));
		this.state = state;
	}

	@Override
	protected void init() {
		panelHeight = state.step() == SoulGiftPayload.CHOOSE ? ROWS_TOP + state.parts().size() * ROW + 36 : 176;
		left = (width - WIDTH) / 2;
		top = (height - panelHeight) / 2;
		int y = top + panelHeight - 28;
		switch (state.step()) {
			case SoulGiftPayload.CHOOSE -> {
				// Une ligne par partie perdue : la rendre au prix de son âme, des âmes de la Pierre, ou, pour un
				// corps entier, de sa Porte.
				for (int i = 0; i < state.parts().size(); i++) {
					String part = state.parts().get(i);
					int ry = top + ROWS_TOP + i * ROW;
					if (SoulGift.GATE_ID.equals(part)) {
						// Sa Porte : seule la Pierre la rend, au plus haut prix.
						Button stone = addRenderableWidget(Button.builder(
								Component.translatable("gate.fmab.gift.pay.stone", state.gateCost()),
								b -> answer(part, "stone", true)).bounds(left + 248, ry, 64, 20).build());
						stone.active = state.stoneSouls() >= state.gateCost();
						continue;
					}
					Button own = addRenderableWidget(Button.builder(Component.translatable("gate.fmab.gift.pay.own_soul"),
							b -> answer(part, "own_soul", true)).bounds(left + 104, ry, 68, 20).build());
					own.active = state.ownSoul();
					if ("body".equals(part)) {
						Button gate = addRenderableWidget(Button.builder(Component.translatable("gate.fmab.gift.pay.gate"),
								b -> answer(part, "gate", true)).bounds(left + 176, ry, 68, 20).build());
						gate.active = state.gate();
					}
					int cost = BodyPart.byId(part).map(Restoration::cost).orElse(Integer.MAX_VALUE);
					if (state.stoneSouls() > 0) {
						Button stone = addRenderableWidget(Button.builder(
								Component.translatable("gate.fmab.gift.pay.stone", cost),
								b -> answer(part, "stone", true)).bounds(left + 248, ry, 64, 20).build());
						stone.active = state.stoneSouls() >= cost;
					}
				}
				addRenderableWidget(Button.builder(Component.translatable("gate.fmab.offer.renounce"),
						b -> answer("", "", false)).bounds(left + WIDTH / 2 - 60, y, 120, 20).build());
			}
			case SoulGiftPayload.PROPOSE -> {
				addRenderableWidget(Button.builder(Component.translatable("gate.fmab.offer.accept"),
						b -> answer("", "", true)).bounds(left + 10, y, 145, 20).build());
				addRenderableWidget(Button.builder(Component.translatable("gate.fmab.offer.refuse"),
						b -> answer("", "", false)).bounds(left + WIDTH - 155, y, 145, 20).build());
			}
			default -> {
				addRenderableWidget(Button.builder(Component.translatable("gate".equals(state.payment())
								? "gate.fmab.gift.give_gate" : "gate.fmab.gift.give_soul"),
						b -> answer("", "", true)).bounds(left + 10, y, 145, 20).build());
				addRenderableWidget(Button.builder(Component.translatable("gate.fmab.offer.renounce"),
						b -> answer("", "", false)).bounds(left + WIDTH - 155, y, 145, 20).build());
			}
		}
	}

	/** Ce que l'écran propose de rendre. */
	public List<String> offers() {
		return state.parts();
	}

	/** Ce que répond le joueur, comme ses boutons : une partie et un paiement (au premier choix), oui ou non. */
	public void answer(String part, String payment, boolean accept) {
		answered = true;
		ClientPlatform.get().send(new SoulGiftAnswerPayload(state.step(), part, payment, accept));
		onClose();
	}

	@Override
	public void removed() {
		if (!answered) {
			ClientPlatform.get().send(new SoulGiftAnswerPayload(state.step(), "", "", false));
		}
		super.removed();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + panelHeight + 2, EDGE);
		graphics.fill(left, top, left + WIDTH, top + panelHeight, PAPER);
		graphics.centeredText(font, title, left + WIDTH / 2, top + 8, INK);
		Component part = Component.translatable("gate.fmab.offer.part." + state.part());
		Component price = switch (state.payment()) {
			case "stone" -> Component.translatable("gate.fmab.gift.price.stone", SoulGift.GATE_ID.equals(state.part())
					? state.gateCost() : BodyPart.byId(state.part()).map(Restoration::cost).orElse(0));
			case "gate" -> Component.translatable("gate.fmab.gift.price.gate");
			default -> Component.translatable("gate.fmab.gift.price.own_soul");
		};
		Component text = switch (state.step()) {
			case SoulGiftPayload.PROPOSE -> Component.translatable("gate.fmab.gift.propose.text", state.other(), price, part);
			case SoulGiftPayload.CONFIRM -> Component.translatable("gate".equals(state.payment())
					? "gate.fmab.gift.confirm_gate.text" : "gate.fmab.gift.confirm.text", state.other(), part);
			default -> Component.translatable("gate.fmab.gift.choose.text", state.other(), state.stoneSouls());
		};
		int y = top + TEXT_TOP;
		for (FormattedCharSequence line : font.split(text, WIDTH - 24)) {
			graphics.text(font, line, left + 12, y, FADED, false);
			y += 10;
		}
		if (state.step() == SoulGiftPayload.CHOOSE) {
			for (int i = 0; i < state.parts().size(); i++) {
				graphics.text(font, Component.translatable("gate.fmab.offer.part." + state.parts().get(i)), left + 12,
						top + ROWS_TOP + i * ROW + 6, INK, false);
			}
		}
		if (state.step() == SoulGiftPayload.CONFIRM) {
			graphics.centeredText(font, Component.translatable("gate.fmab.offer.irreversible"), left + WIDTH / 2,
					top + panelHeight - 44, WARNING);
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
