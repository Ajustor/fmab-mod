package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.network.ExamActionPayload;
import com.ajustor.fmab.network.OpenExamPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * L'examen d'Alchimiste d'État : les objets demandés (ce que le candidat porte déjà), la remise,
 * puis l'épreuve en arène.
 */
public class ExamScreen extends Screen {
	private static final int WIDTH = 300;
	private static final int HEIGHT = 190;
	private static final int PAPER = 0xFFEDE3C8;
	private static final int EDGE = 0xFF34466B;
	private static final int INK = 0xFF2B2B40;
	private static final int FADED = 0xFF8A7A60;
	private static final int DONE = 0xFF2E7D32;
	private static final int MISSING = 0xFFB03A2E;

	private final OpenExamPayload state;
	private int left;
	private int top;

	public ExamScreen(OpenExamPayload state) {
		super(Component.translatable("entity.fmab.state_examiner"));
		this.state = state;
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		if (!state.eligible() || state.passed()) {
			return;
		}
		if (!state.itemsGiven()) {
			boolean ready = state.rows().stream().allMatch(r -> r.held() >= r.count());
			Button give = addRenderableWidget(Button.builder(Component.translatable("exam.fmab.give"),
					b -> ClientPlayNetworking.send(new ExamActionPayload(state.entityId(), ExamActionPayload.GIVE)))
					.bounds(left + WIDTH / 2 - 75, top + HEIGHT - 26, 150, 18).build());
			give.active = ready;
		} else {
			addRenderableWidget(Button.builder(Component.translatable("exam.fmab.fight"), b -> {
				ClientPlayNetworking.send(new ExamActionPayload(state.entityId(), ExamActionPayload.FIGHT));
				onClose();
			}).bounds(left + WIDTH / 2 - 75, top + HEIGHT - 26, 150, 18).build());
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 3, top - 3, left + WIDTH + 3, top + HEIGHT + 3, EDGE);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, PAPER);
		graphics.centeredText(font, title, left + WIDTH / 2, top + 6, INK);
		String intro = !state.eligible() ? "exam.fmab.not_eligible"
				: state.passed() ? "exam.fmab.already_passed"
				: state.itemsGiven() ? "exam.fmab.ready_to_fight"
				: "exam.fmab.intro";
		int y = top + 22;
		for (FormattedCharSequence line : font.split(Component.translatable(intro), WIDTH - 20)) {
			graphics.text(font, line, left + 10, y, FADED, false);
			y += 10;
		}
		if (state.eligible() && !state.itemsGiven() && !state.passed()) {
			y += 6;
			for (OpenExamPayload.Row row : state.rows()) {
				boolean ok = row.held() >= row.count();
				Component line = Component.literal(ok ? "✔ " : "◻ ")
						.append(Component.translatable(row.key()))
						.append(" " + Math.min(row.held(), row.count()) + " / " + row.count());
				graphics.text(font, line, left + 10, y, ok ? DONE : MISSING, false);
				y += 14;
			}
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
