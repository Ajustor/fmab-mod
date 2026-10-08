package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.client.render.CircleTextures;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.network.RemoveGlovePayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Les deux emplacements de gants : ce qu'on porte à chaque main, le cercle qui y est brodé ou gravé,
 * et de quoi les retirer. On enfile un gant en l'utilisant.
 */
public class GlovesScreen extends Screen {
	private static final int PANEL_WIDTH = 260;
	private static final int PANEL_HEIGHT = 150;
	private static final int PAPER = 0xFFEDE3C8;
	private static final int EDGE = 0xFF6B4F2A;
	private static final int INK = 0xFF2B2B40;
	private static final int FADED = 0xFF8A7A60;
	private static final int IMAGE = 72;

	private int left;
	private int top;

	public GlovesScreen() {
		super(Component.translatable("gloves.fmab.title"));
	}

	@Override
	protected void init() {
		left = (width - PANEL_WIDTH) / 2;
		top = (height - PANEL_HEIGHT) / 2;
		for (boolean leftHand : new boolean[]{true, false}) {
			int x = column(leftHand);
			addRenderableWidget(Button.builder(Component.translatable("gloves.fmab.remove"),
					b -> ClientPlayNetworking.send(new RemoveGlovePayload(leftHand)))
					.bounds(x, top + PANEL_HEIGHT - 26, IMAGE + 20, 18).build());
		}
	}

	private int column(boolean leftHand) {
		return leftHand ? left + 20 : left + PANEL_WIDTH - 20 - IMAGE - 20;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 3, top - 3, left + PANEL_WIDTH + 3, top + PANEL_HEIGHT + 3, EDGE);
		graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, PAPER);
		graphics.centeredText(font, title, left + PANEL_WIDTH / 2, top + 6, INK);
		Gloves gloves = minecraft == null || minecraft.player == null ? null
				: minecraft.player.getAttached(FmabAttachments.GLOVES);
		if (gloves == null) {
			gloves = Gloves.NONE;
		}
		for (boolean leftHand : new boolean[]{true, false}) {
			int x = column(leftHand);
			int y = top + 20;
			graphics.text(font, Component.translatable(leftHand ? "gloves.fmab.left" : "gloves.fmab.right"), x, y,
					FADED, false);
			ItemStack glove = gloves.get(leftHand);
			if (glove.isEmpty()) {
				graphics.text(font, Component.translatable("gloves.fmab.empty"), x, y + 14, INK, false);
				continue;
			}
			graphics.item(glove, x, y + 10);
			graphics.text(font, glove.getHoverName(), x + 20, y + 14, INK, false);
			Drawing circle = glove.get(FmabComponents.GLOVE_CIRCLE);
			if (circle != null && !circle.isEmpty()) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, CircleTextures.get(circle, INK), x + 10, y + 28, 0, 0,
						IMAGE - 20, IMAGE - 20, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE,
						CircleTextures.SIZE);
			}
		}
		graphics.centeredText(font, Component.translatable("gloves.fmab.hint"), left + PANEL_WIDTH / 2,
				top + PANEL_HEIGHT - 40, FADED);
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
