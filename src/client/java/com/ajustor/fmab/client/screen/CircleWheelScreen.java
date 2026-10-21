package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.client.render.CircleTextures;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.data.Notebooks;
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
		AlchemistData data = FmabAttachments.ALCHEMIST.get(mc.player);
		mc.gui.setScreen(new CircleWheelScreen(key, contents, data != null && data.rank().atLeast(Rank.GATE)));
	}

	// ---- Géométrie -----------------------------------------------------------------------------

	private int count() {
		return contents.pages().size();
	}

	private int radius() {
		return Math.clamp(Math.min(width, height) * 32 / 100, 56, 110);
	}

	/** Au-delà, les cercles se partagent deux anneaux : l'extérieur d'abord, puis l'intérieur. */
	private static final int ONE_RING = 16;
	/** Rayon de l'anneau intérieur, en part du rayon extérieur. */
	private static final double INNER = 0.55;

	private boolean twoRings() {
		return count() > ONE_RING;
	}

	/** Cercles de l'anneau extérieur : à case égale, l'intérieur en tient moins. */
	private int outerCount() {
		return twoRings() ? (int) Math.ceil(count() / (1 + INNER)) : count();
	}

	private boolean inner(int index) {
		return index >= outerCount();
	}

	private int ringCount(boolean inner) {
		return inner ? count() - outerCount() : outerCount();
	}

	private double ringRadius(boolean inner) {
		return inner ? radius() * INNER : radius();
	}

	/** Côté d'une case : assez petit pour que les cases ne se touchent pas, ni d'un anneau à l'autre. */
	private int slot() {
		double arc = 2 * Math.PI * radius() / Math.max(outerCount(), 3);
		int side = Math.clamp((int) (arc * 0.78), 20, 40);
		return twoRings() ? Math.min(side, Math.max(14, (int) (radius() * (1 - INNER)) - 10)) : side;
	}

	private double angle(int index) {
		boolean in = inner(index);
		int rank = in ? index - outerCount() : index;
		return -Math.PI / 2 + 2 * Math.PI * rank / ringCount(in);
	}

	private int slotX(int index) {
		return width / 2 + (int) Math.round(Math.cos(angle(index)) * ringRadius(inner(index)));
	}

	private int slotY(int index) {
		return height / 2 + (int) Math.round(Math.sin(angle(index)) * ringRadius(inner(index)));
	}

	/** La case pointée par la souris : sur son anneau, celle dont la direction est la plus proche. */
	private int pointed(double mouseX, double mouseY) {
		double dx = mouseX - width / 2.0, dy = mouseY - height / 2.0;
		double distance = Math.hypot(dx, dy);
		if (distance < DEAD_ZONE) {
			return -1;
		}
		boolean in = twoRings() && distance < (ringRadius(true) + ringRadius(false)) / 2;
		int n = ringCount(in);
		double turn = (Math.atan2(dy, dx) + Math.PI / 2) / (2 * Math.PI) * n;
		return (in ? outerCount() : 0) + Math.floorMod((int) Math.round(turn), n);
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
		for (boolean in : twoRings() ? new boolean[]{false, true} : new boolean[]{false}) {
			double ring = ringRadius(in);
			for (int k = 0; k < 96; k++) {
				double t = 2 * Math.PI * k / 96;
				int x = cx + (int) Math.round(Math.cos(t) * ring), y = cy + (int) Math.round(Math.sin(t) * ring);
				graphics.fill(x, y, x + 1, y + 1, RING);
			}
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
		// Au centre de deux anneaux, le nom tient entre les cases de l'anneau intérieur.
		int shown = hovered >= 0 ? hovered : contents.selected();
		String name = Notebooks.pageName(contents.pages().get(shown)).getString();
		if (twoRings()) {
			name = font.plainSubstrByWidth(name, Math.max(20, 2 * ((int) ringRadius(true) - s / 2) - 6));
		}
		graphics.centeredText(font, Component.literal(name), cx, cy - 4, hovered >= 0 ? TEXT : DIM);
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
		if (event.key() >= InputConstants.KEY_1 && event.key() <= InputConstants.KEY_9) {
			int index = event.key() - InputConstants.KEY_1;
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
		choose(index, gate && event.button() == InputConstants.MOUSE_BUTTON_LEFT);
		return true;
	}

	/** Ferme la roue ; une case choisie devient la sélection (et l'Initié joint les mains). */
	private void choose(int index, boolean clap) {
		onClose();
		if (index >= 0 && index < count()) {
			ClientPlatform.get().send(new SelectCirclePayload(index, clap));
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
