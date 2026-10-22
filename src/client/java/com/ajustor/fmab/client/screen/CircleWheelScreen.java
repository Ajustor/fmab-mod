package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.client.render.CircleTextures;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.network.SaveNotebookPayload;
import com.ajustor.fmab.network.SelectCirclePayload;
import com.ajustor.fmab.registry.FmabAttachments;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import com.ajustor.fmab.platform.client.ClientPlatform;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * La roue des cercles : neuf places en couronne autour du viseur, et autant de roues qu'on veut. On
 * tient la touche, on pointe un cercle, on relâche : il devient la sélection. Un clic le choisit
 * aussi, et pour qui a vu la Porte, le clic gauche joint aussitôt les mains. Les chiffres 1 à 9
 * choisissent sans viser. La molette passe d'une roue à l'autre ; après la dernière, une roue vide
 * attend qu'on y range un cercle. Le clic droit tenu emporte un cercle : on le pose sur une autre
 * place (il échange avec celui qui s'y trouvait), sur cette roue ou sur une autre, molette en main.
 * Le jeu ne s'arrête pas pendant qu'on choisit.
 */
public class CircleWheelScreen extends Screen {
	private static final int INK = 0xFFF0E6C8;
	private static final int SLOT_BG = 0xB0101018;
	private static final int EMPTY_BG = 0x50101018;
	private static final int HOVER_BG = 0xD02A4A8A;
	private static final int SELECTED_EDGE = 0xFFE0B040;
	private static final int HOVER_EDGE = 0xFF9CC8FF;
	private static final int RING = 0x60F0E6C8;
	private static final int TEXT = 0xFFE0E0E0;
	private static final int DIM = 0xFFA8A8A8;
	/** En deçà de ce rayon autour du centre, rien n'est pointé : relâcher annule. */
	private static final int DEAD_ZONE = 16;
	/** Un clic droit qui bouge moins que ça choisit, au lieu d'emporter le cercle. */
	private static final double DRAG_START = 4;

	private final KeyMapping key;
	private final boolean gate;
	private NotebookContents contents;
	/** La roue affichée. */
	private int wheel;
	/** La place pointée sur la roue affichée (0 à 8), ou −1. */
	private int hovered = -1;
	/** La page emportée au clic droit, ou −1. */
	private int dragged = -1;
	private boolean dragMoved;
	private double dragFromX;
	private double dragFromY;

	private CircleWheelScreen(KeyMapping key, NotebookContents contents, boolean gate) {
		super(Component.translatable("key.fmab.circle_wheel"));
		this.key = key;
		this.contents = contents;
		this.gate = gate;
		this.wheel = contents.slot(contents.selected()) / NotebookContents.WHEEL;
	}

	/** Ouvre la roue, si le carnet a au moins un cercle ; sur celle du cercle sélectionné. */
	public static void open(KeyMapping key) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		NotebookContents contents = Notebooks.of(mc.player);
		if (contents.pages().isEmpty()) {
			mc.player.sendOverlayMessage(Component.translatable("wheel.fmab.empty", Component.keybind("key.fmab.notebook")));
			return;
		}
		AlchemistData data = FmabAttachments.ALCHEMIST.get(mc.player);
		mc.gui.setScreen(new CircleWheelScreen(key, contents, data != null && data.rank().atLeast(Rank.GATE)));
	}

	// ---- Roues ---------------------------------------------------------------------------------

	/** Les roues qu'on peut parcourir : celles du carnet, et une vide au bout pour en commencer une autre. */
	private int wheelCount() {
		return Math.min(contents.wheels() + 1, NotebookContents.MAX_SLOTS / NotebookContents.WHEEL);
	}

	/** La page rangée à cette place de la roue affichée, ou −1. */
	private int pageAt(int place) {
		return place < 0 ? -1 : contents.at(wheel * NotebookContents.WHEEL + place);
	}

	// ---- Géométrie -----------------------------------------------------------------------------

	private int radius() {
		return Math.clamp(Math.min(width, height) * 32 / 100, 56, 110);
	}

	/** Côté d'une case : assez petit pour que les cases ne se touchent pas. */
	private int slot() {
		double arc = 2 * Math.PI * radius() / NotebookContents.WHEEL;
		return Math.clamp((int) (arc * 0.78), 20, 40);
	}

	private static double angle(int place) {
		return -Math.PI / 2 + 2 * Math.PI * place / NotebookContents.WHEEL;
	}

	private int slotX(int place) {
		return width / 2 + (int) Math.round(Math.cos(angle(place)) * radius());
	}

	private int slotY(int place) {
		return height / 2 + (int) Math.round(Math.sin(angle(place)) * radius());
	}

	/** La place pointée par la souris : celle dont la direction est la plus proche. */
	private int pointed(double mouseX, double mouseY) {
		double dx = mouseX - width / 2.0, dy = mouseY - height / 2.0;
		if (Math.hypot(dx, dy) < DEAD_ZONE) {
			return -1;
		}
		double turn = (Math.atan2(dy, dx) + Math.PI / 2) / (2 * Math.PI) * NotebookContents.WHEEL;
		return Math.floorMod((int) Math.round(turn), NotebookContents.WHEEL);
	}

	private boolean dragging() {
		return dragged >= 0 && dragMoved;
	}

	// ---- Rendu ---------------------------------------------------------------------------------

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		// Pas de flou : on choisit en plein combat, le monde doit rester lisible.
		graphics.fill(0, 0, width, height, 0x28000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		hovered = pointed(mouseX, mouseY);
		int cx = width / 2, cy = height / 2, r = radius();
		// L'anneau de la roue, en pointillés.
		for (int k = 0; k < 96; k++) {
			double t = 2 * Math.PI * k / 96;
			int x = cx + (int) Math.round(Math.cos(t) * r), y = cy + (int) Math.round(Math.sin(t) * r);
			graphics.fill(x, y, x + 1, y + 1, RING);
		}
		int s = slot();
		for (int place = 0; place < NotebookContents.WHEEL; place++) {
			int x = slotX(place) - s / 2, y = slotY(place) - s / 2;
			int page = pageAt(place);
			boolean over = place == hovered;
			graphics.fill(x - 2, y - 2, x + s + 2, y + s + 2, over ? HOVER_BG : page < 0 ? EMPTY_BG : SLOT_BG);
			if (page >= 0 && page == contents.selected()) {
				graphics.outline(x - 3, y - 3, s + 6, s + 6, SELECTED_EDGE);
			} else if (over) {
				graphics.outline(x - 3, y - 3, s + 6, s + 6, HOVER_EDGE);
			}
			// Le cercle emporté n'est plus à sa place : il suit la souris.
			if (page >= 0 && !(dragging() && page == dragged)) {
				circle(graphics, page, x, y, s);
			}
			graphics.text(font, String.valueOf(place + 1), x - 1, y - 1, DIM, true);
		}
		if (dragging()) {
			circle(graphics, dragged, mouseX - s / 2, mouseY - s / 2, s);
		}
		// Au centre, le nom du cercle pointé (ou emporté, ou sélectionné), et la roue affichée.
		int shown = dragging() ? dragged : pageAt(hovered);
		boolean bright = shown >= 0;
		if (shown < 0 && contents.slot(contents.selected()) / NotebookContents.WHEEL == wheel) {
			shown = contents.selected();
		}
		if (shown >= 0) {
			graphics.centeredText(font, Notebooks.pageName(contents.pages().get(shown)), cx, cy - 10, bright ? TEXT : DIM);
		}
		graphics.centeredText(font, Component.translatable("wheel.fmab.page", wheel + 1, wheelCount()), cx, cy + 2, DIM);
		graphics.centeredText(font, Component.translatable(gate ? "wheel.fmab.hint.gate" : "wheel.fmab.hint"),
				cx, Math.min(height - 24, cy + r + s / 2 + 10), DIM);
		graphics.centeredText(font, Component.translatable("wheel.fmab.hint.arrange"),
				cx, Math.min(height - 12, cy + r + s / 2 + 22), DIM);
	}

	private void circle(GuiGraphicsExtractor graphics, int page, int x, int y, int s) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, CircleTextures.get(contents.pages().get(page).drawing(), INK),
				x, y, 0, 0, s, s, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE);
	}

	// ---- Saisie --------------------------------------------------------------------------------

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (key.matches(event)) {
			if (dragging()) {
				// On lâche la touche le cercle en main : il est posé, rien n'est choisi.
				drop(hovered);
				onClose();
			} else {
				choose(pageAt(hovered), false);
			}
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() >= InputConstants.KEY_1 && event.key() <= InputConstants.KEY_9) {
			int page = pageAt(event.key() - InputConstants.KEY_1);
			if (page >= 0) {
				choose(page, false);
			}
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (scrollY != 0) {
			wheel = Math.clamp(wheel + (scrollY < 0 ? 1 : -1), 0, wheelCount() - 1);
		}
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int place = pointed(event.x(), event.y());
		if (place < 0) {
			onClose();
			return true;
		}
		int page = pageAt(place);
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			// Peut-être un choix, peut-être un rangement : on le saura au relâcher.
			dragged = page;
			dragMoved = false;
			dragFromX = event.x();
			dragFromY = event.y();
			return true;
		}
		if (page >= 0) {
			choose(page, gate && event.button() == InputConstants.MOUSE_BUTTON_LEFT);
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragged >= 0 && Math.hypot(event.x() - dragFromX, event.y() - dragFromY) >= DRAG_START) {
			dragMoved = true;
		}
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() != InputConstants.MOUSE_BUTTON_RIGHT || dragged < 0) {
			return super.mouseReleased(event);
		}
		if (dragMoved) {
			drop(pointed(event.x(), event.y()));
		} else {
			choose(dragged, false);
		}
		dragged = -1;
		return true;
	}

	/** Pose le cercle emporté à cette place de la roue affichée, et enregistre le carnet. */
	private void drop(int place) {
		int page = dragged;
		dragged = -1;
		if (page < 0 || place < 0) {
			return;
		}
		NotebookContents arranged = contents.placed(page, wheel * NotebookContents.WHEEL + place);
		if (arranged != contents) {
			contents = arranged;
			ClientPlatform.get().send(new SaveNotebookPayload(contents));
		}
	}

	/** Ferme la roue ; la page choisie devient la sélection (et l'Initié joint les mains). */
	private void choose(int page, boolean clap) {
		onClose();
		if (page >= 0 && page < contents.pages().size()) {
			ClientPlatform.get().send(new SelectCirclePayload(page, clap));
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
