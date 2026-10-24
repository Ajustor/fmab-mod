package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.item.Notes;
import com.ajustor.fmab.network.ConsignPayload;
import com.ajustor.fmab.platform.client.ClientPlatform;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Consigner son savoir : on coche les glyphes qu'on comprend et qu'on veut transmettre, et un livre
 * vierge du sac devient des notes à son nom (voir {@link Notes}).
 */
public class ConsignScreen extends Screen {
	private static final int WIDTH = 330;
	private static final int HEIGHT = 220;
	private static final int COLUMNS = 3;
	private static final int ROWS = 7;
	private static final int PAPER = 0xFFEDE3C8;
	private static final int EDGE = 0xFF6B5A3A;
	private static final int INK = 0xFF2B2B40;
	private static final int FADED = 0xFF6B6050;

	private final Screen parent;
	private final List<Glyph> known = new ArrayList<>();
	private final Set<String> chosen = new LinkedHashSet<>();
	private int page;
	private int left;
	private int top;

	public ConsignScreen(Screen parent) {
		super(Component.translatable("tome.fmab.consign.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		if (known.isEmpty() && minecraft != null && minecraft.player != null && minecraft.level != null) {
			AlchemistData me = FmabAttachments.ALCHEMIST.get(minecraft.player);
			for (Glyph g : AlchemyRules.of(minecraft.level.registryAccess()).glyphs()) {
				if (me != null && me.known().contains(g.id())) {
					known.add(g);
				}
			}
		}
		int perPage = COLUMNS * ROWS;
		int cell = (WIDTH - 20) / COLUMNS;
		for (int i = page * perPage; i < Math.min(known.size(), (page + 1) * perPage); i++) {
			Glyph g = known.get(i);
			int slot = i - page * perPage;
			addRenderableWidget(Button.builder(label(g), b -> {
				if (!chosen.remove(g.id()) && chosen.size() < Notes.MAX_GLYPHS) {
					chosen.add(g.id());
				}
				rebuildWidgets();
			}).bounds(left + 10 + (slot % COLUMNS) * cell, top + 44 + (slot / COLUMNS) * 20, cell - 4, 18).build());
		}
		int y = top + HEIGHT - 26;
		if (known.size() > perPage) {
			addRenderableWidget(Button.builder(Component.literal("<"), b -> {
				page = Math.max(0, page - 1);
				rebuildWidgets();
			}).bounds(left + 10, y, 20, 18).build());
			addRenderableWidget(Button.builder(Component.literal(">"), b -> {
				page = Math.min((known.size() - 1) / perPage, page + 1);
				rebuildWidgets();
			}).bounds(left + 34, y, 20, 18).build());
		}
		Button consign = addRenderableWidget(Button.builder(Component.translatable("tome.fmab.consign.write", chosen.size()), b -> {
			ClientPlatform.get().send(new ConsignPayload(List.copyOf(chosen)));
			onClose();
		}).bounds(left + WIDTH - 200, y, 110, 18).build());
		consign.active = !chosen.isEmpty();
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
				.bounds(left + WIDTH - 84, y, 74, 18).build());
	}

	private Component label(Glyph g) {
		Component name = Component.translatable(g.nameKey());
		return chosen.contains(g.id()) ? Component.literal("✔ ").append(name) : name;
	}

	@Override
	public void onClose() {
		if (minecraft != null) {
			minecraft.gui.setScreen(parent);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, EDGE);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, PAPER);
		graphics.centeredText(font, title, left + WIDTH / 2, top + 8, INK);
		int y = top + 22;
		Component hint = known.isEmpty() ? Component.translatable("tome.fmab.consign.empty")
				: Component.translatable("tome.fmab.consign.hint", Notes.MAX_GLYPHS);
		for (FormattedCharSequence line : font.split(hint, WIDTH - 20)) {
			graphics.text(font, line, left + 10, y, FADED, false);
			y += 10;
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
