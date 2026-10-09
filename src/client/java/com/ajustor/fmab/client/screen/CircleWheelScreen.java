package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.client.render.CircleTextures;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.network.SelectCirclePayload;
import com.ajustor.fmab.registry.FmabAttachments;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * La roue des cercles : les pages du carnet en couronne autour du viseur, dans l'ordre du carnet. On
 * tient la touche, on pointe un cercle, on relâche : il devient la sélection. Un clic le choisit
 * aussi, et pour qui a vu la Porte, le clic gauche joint aussitôt les mains. Les chiffres 1 à 9
 * choisissent sans viser. Le jeu ne s'arrête pas pendant qu'on choisit.
 */
public class CircleWheelScreen extends Screen {
	private static final int INK = 0xFFF0E6C8;
	private static final int SLOT_BG = 0xB0101018;
	private static final int HOVER_BG = 0xD02A4A8A;
	private static final int SELECTED_EDGE = 0xFFE0B040;
	private static final int HOVER_EDGE = 0xFF9CC8FF;
	private static final int RING = 0x60F0E6C8;
	private static final int TEXT = 0xFFE0E0E0;
	private static final int DIM = 0xFFA8A8A8;
	/** En deçà de ce rayon autour du centre, rien n'est pointé : relâcher annule. */
	private static final int DEAD_ZONE = 16;

	private final KeyMapping key;
	private final NotebookContents contents;
	private final boolean gate;
	private int hovered = -1;

	private CircleWheelScreen(KeyMapping key, NotebookContents contents, boolean gate) {
		super(Component.translatable("key.fmab.circle_wheel"));
		this.key = key;
		this.contents = contents;
		this.gate = gate;
	}

	/** Ouvre la roue, si le carnet a au moins un cercle. */
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
		AlchemistData data = mc.player.getAttached(FmabAttachments.ALCHEMIST);
		mc.gui.setScreen(new CircleWheelScreen(key, contents, data != null && data.rank().atLeast(Rank.GATE)));
	}

	// ---- Géométrie -----------------------------------------------------------------------------

	private int count() {
		return contents.pages().size();
	}

	private int radius() {
		return Math.clamp(Math.min(width, height) * 32 / 100, 56, 110);
	}

	/** Côté d'une case : assez petit pour que les cases ne se touchent pas. */
	private int slot() {
		double arc = 2 * Math.PI * radius() / Math.max(count(), 3);
		return Math.clamp((int) (arc * 0.78), 20, 40);
	}

	private double angle(int index) {
		return -Math.PI / 2 + 2 * Math.PI * index / count();
	}

	private int slotX(int index) {
		return width / 2 + (int) Math.round(Math.cos(angle(index)) * radius());
	}

	private int slotY(int index) {
		return height / 2 + (int) Math.round(Math.sin(angle(index)) * radius());
	}

	/** La case pointée par la souris : celle dont la direction est la plus proche. */
	private int pointed(double mouseX, double mouseY) {
		double dx = mouseX - width / 2.0, dy = mouseY - height / 2.0;
		if (Math.hypot(dx, dy) < DEAD_ZONE) {
			return -1;
		}
		double turn = (Math.atan2(dy, dx) + Math.PI / 2) / (2 * Math.PI) * count();
		return Math.floorMod((int) Math.round(turn), count());
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
		for (int i = 0; i < count(); i++) {
			int x = slotX(i) - s / 2, y = slotY(i) - s / 2;
			boolean over = i == hovered;
			graphics.fill(x - 2, y - 2, x + s + 2, y + s + 2, over ? HOVER_BG : SLOT_BG);
			if (i == contents.selected()) {
				graphics.outline(x - 3, y - 3, s + 6, s + 6, SELECTED_EDGE);
			} else if (over) {
				graphics.outline(x - 3, y - 3, s + 6, s + 6, HOVER_EDGE);
			}
			graphics.blit(RenderPipelines.GUI_TEXTURED, CircleTextures.get(contents.pages().get(i).drawing(), INK),
					x, y, 0, 0, s, s, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE);
			if (i < 9) {
				graphics.text(font, String.valueOf(i + 1), x - 1, y - 1, DIM, true);
			}
		}
		// Au centre, le nom du cercle pointé (ou du cercle sélectionné).
		int shown = hovered >= 0 ? hovered : contents.selected();
		graphics.centeredText(font, Notebooks.pageName(contents.pages().get(shown)), cx, cy - 4,
				hovered >= 0 ? TEXT : DIM);
		graphics.centeredText(font, Component.translatable(gate ? "wheel.fmab.hint.gate" : "wheel.fmab.hint"),
				cx, Math.min(height - 14, cy + r + s / 2 + 10), DIM);
	}

	// ---- Saisie --------------------------------------------------------------------------------

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (key.matches(event)) {
			choose(hovered, false);
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() >= GLFW.GLFW_KEY_1 && event.key() <= GLFW.GLFW_KEY_9) {
			int index = event.key() - GLFW.GLFW_KEY_1;
			if (index < count()) {
				choose(index, false);
			}
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int index = pointed(event.x(), event.y());
		if (index < 0) {
			onClose();
			return true;
		}
		choose(index, gate && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT);
		return true;
	}

	/** Ferme la roue ; une case choisie devient la sélection (et l'Initié joint les mains). */
	private void choose(int index, boolean clap) {
		onClose();
		if (index >= 0 && index < count()) {
			ClientPlayNetworking.send(new SelectCirclePayload(index, clap));
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
