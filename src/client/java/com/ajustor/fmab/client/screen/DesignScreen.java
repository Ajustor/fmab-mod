package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.network.ChooseDesignPayload;
import com.ajustor.fmab.network.OpenDesignsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * La page de conception : l'alchimiste choisit l'objet auquel il pense en recomposant, parmi ceux
 * dont il a compris la structure. Sans modèle posé sur le cercle, c'est cet objet qu'un cercle
 * Recomposer crée, contre la matière qu'il contient.
 */
public class DesignScreen extends Screen {
	private static final int WIDTH = 300;
	private static final int HEIGHT = 206;
	private static final int COLUMNS = 8;
	private static final int ROWS = 6;
	private static final int SLOT = 18;
	private static final int PAPER = 0xFFEDE3C8;
	private static final int EDGE = 0xFF6B4F2A;
	private static final int INK = 0xFF2B2B40;
	private static final int FADED = 0xFF8A7A60;
	private static final int SLOT_BG = 0x30000000;
	private static final int CHOSEN = 0xFF3D7AB8;

	private record Design(ItemStack stack, String id, Map<String, Integer> cost) {
	}

	private final List<Design> all = new ArrayList<>();
	private final List<Design> shown = new ArrayList<>();
	private String current;
	private int left;
	private int top;
	private int scroll;
	private EditBox search;
	/** Sous la phrase d'introduction, quelle que soit sa longueur (elle dépend de la langue). */
	private int searchY;

	public DesignScreen(OpenDesignsPayload payload) {
		super(Component.translatable("design.fmab.title"));
		for (OpenDesignsPayload.Entry entry : payload.entries()) {
			BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(entry.item())).ifPresent(item ->
					all.add(new Design(new ItemStack(item), entry.item(), entry.cost())));
		}
		all.sort((a, b) -> a.stack.getHoverName().getString().compareToIgnoreCase(b.stack.getHoverName().getString()));
		current = payload.current();
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		searchY = top + 18 + font.split(Component.translatable("design.fmab.intro"), WIDTH - 20).size() * 9 + 4;
		search = addRenderableWidget(new EditBox(font, left + 10, searchY, COLUMNS * SLOT, 14,
				Component.translatable("design.fmab.search")));
		search.setHint(Component.translatable("design.fmab.search"));
		search.setResponder(text -> filter());
		addRenderableWidget(Button.builder(Component.translatable("design.fmab.forget"), b -> choose(""))
				.bounds(left + WIDTH - 110, top + HEIGHT - 26, 100, 18).build());
		filter();
	}

	private void filter() {
		String text = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
		shown.clear();
		for (Design d : all) {
			if (text.isEmpty() || d.stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(text)) {
				shown.add(d);
			}
		}
		scroll = 0;
	}

	private void choose(String id) {
		current = id;
		ClientPlayNetworking.send(new ChooseDesignPayload(id));
	}

	private int gridX() {
		return left + 10;
	}

	private int gridY() {
		return searchY + 18;
	}

	/** L'objet sous la souris dans la grille, ou -1. */
	private int pointed(double x, double y) {
		int col = (int) Math.floor((x - gridX()) / SLOT);
		int row = (int) Math.floor((y - gridY()) / SLOT);
		if (x < gridX() || y < gridY() || col >= COLUMNS || row >= ROWS) {
			return -1;
		}
		int index = (scroll + row) * COLUMNS + col;
		return index < shown.size() ? index : -1;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 3, top - 3, left + WIDTH + 3, top + HEIGHT + 3, EDGE);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, PAPER);
		graphics.centeredText(font, title, left + WIDTH / 2, top + 6, INK);
		int y = top + 18;
		for (FormattedCharSequence line : font.split(Component.translatable("design.fmab.intro"), WIDTH - 20)) {
			graphics.text(font, line, left + 10, y, FADED, false);
			y += 9;
		}

		if (all.isEmpty()) {
			int ty = gridY() + 10;
			for (FormattedCharSequence line : font.split(Component.translatable("design.fmab.empty"), COLUMNS * SLOT)) {
				graphics.text(font, line, gridX(), ty, FADED, false);
				ty += 10;
			}
		}
		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < COLUMNS; col++) {
				int x = gridX() + col * SLOT;
				int sy = gridY() + row * SLOT;
				graphics.fill(x, sy, x + SLOT - 1, sy + SLOT - 1, SLOT_BG);
				int index = (scroll + row) * COLUMNS + col;
				if (index < shown.size()) {
					Design d = shown.get(index);
					if (d.id.equals(current)) {
						graphics.fill(x - 1, sy - 1, x + SLOT, sy + SLOT, CHOSEN);
						graphics.fill(x, sy, x + SLOT - 1, sy + SLOT - 1, PAPER);
					}
					graphics.item(d.stack, x + 1, sy + 1);
				}
			}
		}

		// À droite : l'objet auquel on pense, et ce qu'il coûte.
		int px = gridX() + COLUMNS * SLOT + 12;
		int py = gridY();
		graphics.text(font, Component.translatable("design.fmab.current"), px, py, INK, false);
		Design chosen = all.stream().filter(d -> d.id.equals(current)).findFirst().orElse(null);
		if (chosen == null) {
			graphics.text(font, Component.translatable("design.fmab.nothing"), px, py + 14, FADED, false);
		} else {
			graphics.item(chosen.stack, px, py + 12);
			int ty = py + 14;
			for (FormattedCharSequence line : font.split(chosen.stack.getHoverName(), WIDTH - (px - left) - 30)) {
				graphics.text(font, line, px + 20, ty, INK, false);
				ty += 10;
			}
			ty = Math.max(ty, py + 32) + 4;
			for (var entry : chosen.cost.entrySet()) {
				graphics.text(font, Component.translatable("design.fmab.cost_line",
						Component.translatable("material.fmab." + entry.getKey()), entry.getValue()), px, ty, FADED, false);
				ty += 10;
			}
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);

		int hovered = pointed(mouseX, mouseY);
		if (hovered >= 0) {
			Design d = shown.get(hovered);
			MutableComponent cost = Component.empty();
			d.cost.forEach((element, mass) -> {
				if (!cost.getSiblings().isEmpty()) {
					cost.append(", ");
				}
				cost.append(Component.translatable("design.fmab.cost_line",
						Component.translatable("material.fmab." + element), mass));
			});
			graphics.setTooltipForNextFrame(List.of(d.stack.getHoverName().getVisualOrderText(),
					cost.getVisualOrderText()), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		int index = pointed(event.x(), event.y());
		if (index >= 0) {
			String id = shown.get(index).id;
			choose(id.equals(current) ? "" : id);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		int rows = (shown.size() + COLUMNS - 1) / COLUMNS;
		scroll = Math.max(0, Math.min(Math.max(0, rows - ROWS), scroll - (int) Math.signum(scrollY)));
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
