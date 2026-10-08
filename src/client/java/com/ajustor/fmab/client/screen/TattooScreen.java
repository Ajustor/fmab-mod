package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.client.render.CircleTextures;
import com.ajustor.fmab.data.Tattoos;
import com.ajustor.fmab.network.OpenTattooPayload;
import com.ajustor.fmab.network.TattooPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.tattoo.TattooSlot;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Le rituel du tatouage : choisir où le cercle du sol passera dans la peau, ou effacer un
 * tatouage existant.
 */
public class TattooScreen extends Screen {
	private static final int WIDTH = 320;
	private static final int ROW = 30;
	private static final int PAPER = 0xFFEDE3C8;
	private static final int EDGE = 0xFF6B2A2A;
	private static final int INK = 0xFF2B2B40;
	private static final int FADED = 0xFF8A7A60;
	private static final int THUMB = 24;

	private final OpenTattooPayload state;
	private int left;
	private int top;
	private int panelHeight;

	public TattooScreen(OpenTattooPayload state) {
		super(Component.translatable("tattoo.fmab.title"));
		this.state = state;
	}

	@Override
	protected void init() {
		panelHeight = 50 + TattooSlot.values().length * ROW;
		left = (width - WIDTH) / 2;
		top = (height - panelHeight) / 2;
		int y = top + 44;
		for (TattooSlot slot : TattooSlot.values()) {
			boolean taken = state.occupied().contains(slot.id());
			Button b = addRenderableWidget(Button.builder(
					Component.translatable(taken ? "tattoo.fmab.erase" : "tattoo.fmab.here"), btn -> {
						ClientPlayNetworking.send(new TattooPayload(state.circle(), slot.id(), taken));
						onClose();
					}).bounds(left + WIDTH - 96, y + 4, 86, 18).build());
			b.active = true;
			y += ROW;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 3, top - 3, left + WIDTH + 3, top + panelHeight + 3, EDGE);
		graphics.fill(left, top, left + WIDTH, top + panelHeight, PAPER);
		graphics.centeredText(font, title, left + WIDTH / 2, top + 6, INK);
		int y = top + 20;
		for (FormattedCharSequence line : font.split(Component.translatable("tattoo.fmab.intro"), WIDTH - 20)) {
			graphics.text(font, line, left + 10, y, FADED, false);
			y += 10;
		}
		Tattoos tattoos = minecraft == null || minecraft.player == null ? null
				: minecraft.player.getAttached(FmabAttachments.TATTOOS);
		y = top + 44;
		for (TattooSlot slot : TattooSlot.values()) {
			Drawing drawing = tattoos == null ? null : tattoos.get(slot).orElse(null);
			if (drawing != null) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, CircleTextures.get(drawing, INK), left + 10, y + 2, 0, 0,
						THUMB, THUMB, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE);
			} else {
				graphics.outline(left + 10, y + 2, THUMB, THUMB, FADED);
			}
			graphics.text(font, Component.translatable(slot.translationKey()), left + 42, y + 4, INK, false);
			graphics.text(font, Component.translatable(slot.active() ? "tattoo.fmab.active" : "tattoo.fmab.passive",
					slot.stages()), left + 42, y + 15, FADED, false);
			y += ROW;
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
