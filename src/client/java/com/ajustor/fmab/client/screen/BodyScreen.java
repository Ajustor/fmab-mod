package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.gate.BodyMenu;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.registry.FmabAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * La page du corps : le joueur au milieu, ses membres et ses gants autour (vus de face, la droite à
 * gauche), l'état de ses organes, de sa vue et de sa Porte en dessous, l'inventaire en bas. On y
 * arrive par l'onglet de l'inventaire.
 */
public class BodyScreen extends AbstractContainerScreen<BodyMenu> {
	private static final Identifier INVENTORY = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
	private static final int PANEL = 0xFFC6C6C6;
	private static final int LABEL = 0xFF404040;
	private static final int GOOD = 0xFF2E6B30;
	private static final int BAD = 0xFFB0201A;
	private static final int SOUL = 0xFF6A3D9A;
	private static final int SKIN = 0x80B88A60;
	private static final int LOST = 0xFFB0201A;
	/** La boîte du modèle du joueur. */
	private static final int MODEL_X0 = 61, MODEL_Y0 = 7, MODEL_X1 = 115, MODEL_Y1 = 64;
	/** La rangée d'état, sous le modèle. */
	private static final int STATUS_Y = 69;

	private float xMouse;
	private float yMouse;

	public BodyScreen(BodyMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		InventoryTabs.add(this, false);
	}

	private GateState gate() {
		GateState gate = minecraft.player == null ? null : minecraft.player.getAttached(FmabAttachments.GATE);
		return gate == null ? GateState.NONE : gate;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		xMouse = mouseX;
		yMouse = mouseY;
		List<Component> tooltip = tooltipAt(mouseX - leftPos, mouseY - topPos);
		if (!tooltip.isEmpty()) {
			graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int x = leftPos, y = topPos;
		// La fenêtre de l'inventaire, dont on vide le haut (armure, artisanat) pour y mettre le corps.
		graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
		graphics.fill(x + 4, y + 4, x + imageWidth - 4, y + 81, PANEL);
		frame(graphics, x + MODEL_X0, y + MODEL_Y0, MODEL_X1 - MODEL_X0, MODEL_Y1 - MODEL_Y0);
		graphics.fill(x + MODEL_X0, y + MODEL_Y0, x + MODEL_X1, y + MODEL_Y1, 0xFF000000);
		if (minecraft.player != null) {
			InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x + MODEL_X0, y + MODEL_Y0, x + MODEL_X1,
					y + MODEL_Y1, 25, 0.0625F, xMouse, yMouse, minecraft.player);
		}
		GateState gate = gate();
		for (int i = 0; i < BodyMenu.SLOTS; i++) {
			Slot slot = menu.slots.get(i);
			frame(graphics, x + slot.x - 1, y + slot.y - 1, 18, 18);
			if (!slot.hasItem()) {
				emptySlot(graphics, gate, i, x + slot.x, y + slot.y);
			}
		}
		statusRow(graphics, gate, x, y + STATUS_Y);
	}

	/** Un emplacement vide montre le membre de chair, ou le vide qu'a laissé la Porte. */
	private static void emptySlot(GuiGraphicsExtractor graphics, GateState gate, int index, int x, int y) {
		BodyPart part = BodyMenu.limb(index);
		if (part == null) {
			// Une main nue, en ombre.
			graphics.fill(x + 5, y + 4, x + 11, y + 12, 0x40000000);
			graphics.fill(x + 6, y + 12, x + 10, y + 14, 0x40000000);
			return;
		}
		int top = part.arm() ? y + 1 : y;
		int height = part.arm() ? 14 : 16;
		if (gate.soulBound()) {
			graphics.fill(x + 6, top, x + 10, top + height, 0x406A3D9A);
		} else if (gate.lost(part)) {
			graphics.outline(x + 5, top, 6, height, LOST);
		} else {
			graphics.fill(x + 6, top, x + 10, top + height, SKIN);
		}
	}

	/** Organes, vue, Porte : en mots, d'une couleur qui dit tout. */
	private void statusRow(GuiGraphicsExtractor graphics, GateState gate, int x, int y) {
		if (gate.soulBound()) {
			graphics.text(font, Component.translatable("body_page.fmab.soul"), x + 8, y, SOUL, false);
			return;
		}
		boolean organs = !gate.lost(BodyPart.ORGANS), sight = !gate.lost(BodyPart.SIGHT);
		graphics.text(font, Component.translatable("body_page.fmab.organs"), x + 8, y, organs ? GOOD : BAD, false);
		graphics.text(font, Component.translatable("body_page.fmab.sight"), x + 64, y, sight ? GOOD : BAD, false);
		Component gateLabel = Component.translatable("body_page.fmab.gate", gate.openings());
		graphics.text(font, gateLabel, x + imageWidth - 8 - font.width(gateLabel), y, LABEL, false);
	}

	/** Un cadre d'emplacement à la façon du jeu : ombre en haut à gauche, lumière en bas à droite. */
	private static void frame(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		graphics.fill(x, y, x + width, y + height, 0xFF8B8B8B);
		graphics.fill(x, y, x + width - 1, y + 1, 0xFF373737);
		graphics.fill(x, y, x + 1, y + height - 1, 0xFF373737);
		graphics.fill(x + 1, y + height - 1, x + width, y + height, 0xFFFFFFFF);
		graphics.fill(x + width - 1, y + 1, x + width, y + height, 0xFFFFFFFF);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		graphics.text(font, title, titleLabelX, titleLabelY, LABEL, false);
	}

	/** Ce que dit la page sous la souris (coordonnées dans la fenêtre). */
	private List<Component> tooltipAt(int x, int y) {
		List<Component> lines = new ArrayList<>();
		GateState gate = gate();
		if (hoveredSlot != null && hoveredSlot.index < BodyMenu.SLOTS && !hoveredSlot.hasItem()) {
			BodyPart part = BodyMenu.limb(hoveredSlot.index);
			if (part == null) {
				lines.add(Component.translatable(BodyMenu.leftGlove(hoveredSlot.index) ? "body_page.fmab.glove.left"
						: "body_page.fmab.glove.right"));
				lines.add(Component.translatable("body_page.fmab.glove.empty").withStyle(ChatFormatting.GRAY));
			} else {
				lines.add(Component.translatable("entity.fmab.winry.limb." + part.serializedName()));
				lines.add(Component.translatable(gate.soulBound() ? "body_page.fmab.limb.soul"
						: gate.lost(part) ? "body_page.fmab.limb.lost" : "body_page.fmab.limb.intact").withStyle(ChatFormatting.GRAY));
			}
			return lines;
		}
		if (y >= STATUS_Y - 1 && y < STATUS_Y + 9 && !gate.soulBound()) {
			if (x >= 8 && x < 60) {
				lines.add(Component.translatable(gate.lost(BodyPart.ORGANS) ? "body_page.fmab.organs.lost" : "body_page.fmab.organs.intact"));
			} else if (x >= 64 && x < 110) {
				lines.add(Component.translatable(gate.lost(BodyPart.SIGHT) ? "body_page.fmab.sight.lost" : "body_page.fmab.sight.intact"));
			} else if (x >= 110 && x < imageWidth - 8) {
				lines.add(Component.translatable("body_page.fmab.gate.tooltip"));
			}
		}
		return lines;
	}

	@Override
	protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
		List<Component> lines = super.getTooltipFromContainerItem(stack);
		if (hoveredSlot != null && BodyMenu.limb(hoveredSlot.index) != null) {
			lines = new ArrayList<>(lines);
			lines.add(Component.translatable("body_page.fmab.limb.automail").withStyle(ChatFormatting.GRAY));
		}
		return lines;
	}
}
