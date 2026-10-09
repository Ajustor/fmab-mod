package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.network.WinryActionPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * L'atelier de Winry : l'état de chaque membre (intact, perdu, automail et son usure), la réparation
 * contre des lingots de fer et le retrait d'une pièce. Pour poser un automail, on le lui tend.
 */
public class WinryScreen extends Screen {
	private static final List<BodyPart> LIMBS = List.of(BodyPart.RIGHT_ARM, BodyPart.LEFT_ARM, BodyPart.RIGHT_LEG,
			BodyPart.LEFT_LEG);
	private static final int WIDTH = 320;
	/** Une ligne par membre : son nom et les boutons, puis son état en dessous (le nom d'une pièce est long). */
	private static final int ROW = 32;
	private static final int PAPER = 0xFFEDE3C8;
	private static final int EDGE = 0xFF4A5560;
	private static final int INK = 0xFF2B2B40;
	private static final int FADED = 0xFF7A7060;
	private static final int GOOD = 0xFF2E7D32;
	private static final int BAD = 0xFFB0201A;

	private final int entityId;
	private int left;
	private int top;
	private int panelHeight;
	/** Les automails tels qu'affichés : quand le serveur les change, on refait les boutons. */
	private Automail shown;

	public WinryScreen(int entityId) {
		super(Component.translatable("entity.fmab.winry"));
		this.entityId = entityId;
	}

	@Override
	protected void init() {
		shown = automail();
		panelHeight = 64 + LIMBS.size() * ROW + 12;
		left = (width - WIDTH) / 2;
		top = (height - panelHeight) / 2;
		int y = top + 60;
		for (BodyPart part : LIMBS) {
			ItemStack stack = shown.get(part);
			if (!stack.isEmpty()) {
				int cost = Automails.repairCost(stack);
				Button repair = addRenderableWidget(Button.builder(cost == 0
								? Component.translatable("entity.fmab.winry.repaired")
								: Component.translatable("entity.fmab.winry.repair", cost),
						b -> ClientPlayNetworking.send(new WinryActionPayload(entityId, part.serializedName(), true)))
						.bounds(left + WIDTH - 156, y - 4, 86, 18).build());
				repair.active = cost > 0;
				addRenderableWidget(Button.builder(Component.translatable("entity.fmab.winry.remove"),
						b -> ClientPlayNetworking.send(new WinryActionPayload(entityId, part.serializedName(), false)))
						.bounds(left + WIDTH - 66, y - 4, 58, 18).build());
			}
			y += ROW;
		}
	}

	@Override
	public void tick() {
		// ItemStack n'a pas d'égalité de valeur : on compare ce que l'écran montre.
		if (!signature(automail()).equals(signature(shown))) {
			rebuildWidgets();
		}
	}

	private static String signature(Automail automail) {
		StringBuilder out = new StringBuilder();
		for (BodyPart part : LIMBS) {
			ItemStack stack = automail.get(part);
			out.append(part.ordinal()).append(':').append(stack.getItem()).append('/').append(stack.getDamageValue())
					.append(';');
		}
		return out.toString();
	}

	private Automail automail() {
		Automail a = minecraft == null || minecraft.player == null ? null
				: minecraft.player.getAttached(FmabAttachments.AUTOMAIL);
		return a == null ? Automail.NONE : a;
	}

	private GateState gate() {
		GateState g = minecraft == null || minecraft.player == null ? null
				: minecraft.player.getAttached(FmabAttachments.GATE);
		return g == null ? GateState.NONE : g;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 3, top - 3, left + WIDTH + 3, top + panelHeight + 3, EDGE);
		graphics.fill(left, top, left + WIDTH, top + panelHeight, PAPER);
		graphics.centeredText(font, title, left + WIDTH / 2, top + 6, INK);
		int y = top + 20;
		for (FormattedCharSequence line : font.split(Component.translatable("entity.fmab.winry.greeting"), WIDTH - 20)) {
			graphics.text(font, line, left + 10, y, FADED, false);
			y += 10;
		}
		y = top + 60;
		GateState gate = gate();
		for (BodyPart part : LIMBS) {
			Component name = Component.translatable("entity.fmab.winry.limb." + part.serializedName());
			graphics.text(font, name, left + 10, y, INK, false);
			ItemStack stack = shown.get(part);
			Component status;
			int color;
			if (!stack.isEmpty()) {
				int health = 100 - Math.round(100f * stack.getDamageValue() / stack.getMaxDamage());
				boolean broken = Automails.broken(stack);
				status = Component.translatable(broken ? "entity.fmab.winry.broken" : "entity.fmab.winry.automail",
						stack.getHoverName(), health);
				color = broken ? BAD : GOOD;
			} else if (gate.soulBound()) {
				status = Component.translatable("entity.fmab.winry.armor");
				color = FADED;
			} else if (gate.lost(part)) {
				status = Component.translatable("entity.fmab.winry.lost");
				color = BAD;
			} else {
				status = Component.translatable("entity.fmab.winry.intact");
				color = FADED;
			}
			graphics.text(font, status, left + 18, y + 16, color, false);
			y += ROW;
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
