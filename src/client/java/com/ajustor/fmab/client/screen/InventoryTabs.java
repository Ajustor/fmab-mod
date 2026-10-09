package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.client.mixin.AbstractContainerScreenAccessor;
import com.ajustor.fmab.network.OpenBodyPayload;
import com.ajustor.fmab.registry.FmabItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Les onglets au-dessus de l'inventaire, à la manière de ceux du mode créatif : l'inventaire, puis
 * la page du corps. Ils suivent l'inventaire quand le livre de recettes le décale.
 */
public final class InventoryTabs {
	private static final int WIDTH = 26;
	private static final int HEIGHT = 32;
	/** Les onglets dépassent du haut de la fenêtre de tant de pixels. */
	private static final int RISE = 28;

	private InventoryTabs() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			if (screen instanceof InventoryScreen inventory) {
				Tab[] tabs = add(inventory, true);
				ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, a) -> place(inventory, tabs));
			}
		});
	}

	/**
	 * Ajoute les deux onglets à un écran d'inventaire ou à la page du corps.
	 *
	 * @param inventoryOpen l'onglet de l'inventaire est-il celui qui est ouvert ?
	 */
	public static Tab[] add(AbstractContainerScreen<?> screen, boolean inventoryOpen) {
		Tab inventory = new Tab(0, new ItemStack(Items.CHEST), Component.translatable("body_page.fmab.tab.inventory"),
				inventoryOpen, InventoryTabs::openInventory);
		Tab body = new Tab(1, new ItemStack(FmabItems.IRON_AUTOMAIL_ARM), Component.translatable("body_page.fmab.tab.body"),
				!inventoryOpen, () -> ClientPlayNetworking.send(OpenBodyPayload.INSTANCE));
		Tab[] tabs = {inventory, body};
		for (Tab tab : tabs) {
			Screens.getWidgets(screen).add(tab);
		}
		place(screen, tabs);
		return tabs;
	}

	private static void place(AbstractContainerScreen<?> screen, Tab[] tabs) {
		AbstractContainerScreenAccessor at = (AbstractContainerScreenAccessor) screen;
		for (Tab tab : tabs) {
			tab.setPosition(at.fmab$leftPos() + tab.column * (WIDTH + 1), at.fmab$topPos() - RISE);
			tab.panelTop = at.fmab$topPos();
		}
	}

	/** Revenir à l'inventaire : on ferme la page du corps, puis on ouvre l'inventaire. */
	private static void openInventory() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gui.screen() instanceof InventoryScreen) {
			return;
		}
		minecraft.player.closeContainer();
		minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
	}

	/** Un onglet : sa colonne, son icône, et ce qu'il ouvre. */
	public static final class Tab extends AbstractButton {
		private final int column;
		private final ItemStack icon;
		private final boolean selected;
		private final Runnable action;
		/** Le haut de la fenêtre : un onglet fermé passe dessous. */
		private int panelTop;

		Tab(int column, ItemStack icon, Component name, boolean selected, Runnable action) {
			super(0, 0, WIDTH, HEIGHT, name);
			this.column = column;
			this.icon = icon;
			this.selected = selected;
			this.action = action;
			setTooltip(Tooltip.create(name));
		}

		@Override
		public void onPress(InputWithModifiers input) {
			if (!selected) {
				action.run();
			}
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			int x = getX(), y = getY();
			Identifier sprite = Identifier.withDefaultNamespace("container/creative_inventory/tab_top_"
					+ (selected ? "selected_" : "unselected_") + (column + 1));
			if (selected) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, WIDTH, HEIGHT);
			} else {
				// Comme au mode créatif, l'onglet fermé passe sous la fenêtre.
				graphics.enableScissor(x, y, x + WIDTH, panelTop);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, WIDTH, HEIGHT);
				graphics.disableScissor();
			}
			graphics.item(icon, x + 5, y + 9);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}
}
