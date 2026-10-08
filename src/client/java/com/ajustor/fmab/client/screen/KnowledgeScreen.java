package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.knowledge.Knowledge;
import com.ajustor.fmab.alchemy.knowledge.KnowledgeNode;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arbre de savoir : la maîtrise de chaque école et les nœuds qu'elle débloque. On progresse en
 * pratiquant ; cet écran ne fait que montrer où l'on en est.
 */
public class KnowledgeScreen extends Screen {
	private static final int PAPER = 0xFFEDE3C8;
	private static final int EDGE = 0xFF6B4F2A;
	private static final int INK = 0xFF2B2B40;
	private static final int FADED = 0xFF8A7A60;
	private static final int DONE = 0xFF2E7D32;
	private static final int COLUMN = 150;
	private static final int ROW = 12;

	private final Screen parent;
	private final List<Hover> hovers = new ArrayList<>();

	/** Zone d'un nœud à l'écran, pour afficher sa description au survol. */
	private record Hover(int x0, int y0, int x1, int y1, KnowledgeNode node) {
	}

	public KnowledgeScreen(Screen parent) {
		super(Component.translatable("knowledge.fmab.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
				.bounds(width / 2 - 40, height - 28, 80, 18).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		AlchemistData me = alchemist();
		AlchemyRules rules = minecraft == null || minecraft.level == null ? null
				: AlchemyRules.of(minecraft.level.registryAccess());
		Knowledge knowledge = rules == null ? Knowledge.NONE : rules.knowledge(me);

		Map<String, List<KnowledgeNode>> schools = new LinkedHashMap<>();
		knowledge.nodes().stream()
				.sorted(Comparator.comparing(KnowledgeNode::school).thenComparingInt(KnowledgeNode::mastery))
				.forEach(n -> schools.computeIfAbsent(n.school(), s -> new ArrayList<>()).add(n));

		int bookWidth = Math.max(2, schools.size()) * COLUMN + 20;
		int left = (width - bookWidth) / 2;
		int top = 20;
		int bottom = height - 36;
		graphics.fill(left - 3, top - 3, left + bookWidth + 3, bottom + 3, EDGE);
		graphics.fill(left, top, left + bookWidth, bottom, PAPER);

		graphics.centeredText(font, title, width / 2, top + 6, INK);
		Component rank = Component.translatable(me.rank().translationKey());
		Component status = me.rank() == Rank.APPRENTICE
				? Component.translatable("knowledge.fmab.toward_alchemist", rank, knowledge.totalMastery(),
						Knowledge.ALCHEMIST_MASTERY)
				: Component.translatable("knowledge.fmab.rank", rank, knowledge.totalMastery());
		graphics.centeredText(font, status, width / 2, top + 18, FADED);

		hovers.clear();
		int x = left + 10;
		for (Map.Entry<String, List<KnowledgeNode>> school : schools.entrySet()) {
			int y = top + 40;
			graphics.text(font, Component.translatable("knowledge.fmab.school." + school.getKey())
					.withStyle(s -> s.withBold(true)), x, y, INK, false);
			y += ROW;
			graphics.text(font, Component.translatable("knowledge.fmab.mastery", knowledge.mastery(school.getKey())),
					x, y, FADED, false);
			y += ROW + 4;
			for (KnowledgeNode node : school.getValue()) {
				boolean has = knowledge.has(node.id());
				Component line = Component.literal(has ? "✔ " : "◻ ")
						.append(Component.translatable(node.nameKey()))
						.append(has ? "" : " (" + node.mastery() + ")");
				graphics.text(font, line, x + (node.parent().isPresent() ? 8 : 0), y, has ? DONE : INK, false);
				hovers.add(new Hover(x, y - 1, x + COLUMN - 10, y + ROW - 2, node));
				y += ROW;
			}
			x += COLUMN;
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);

		for (Hover h : hovers) {
			if (mouseX >= h.x0 && mouseX < h.x1 && mouseY >= h.y0 && mouseY < h.y1) {
				List<FormattedCharSequence> lines = font.split(Component.translatable(h.node.descriptionKey()), 200);
				graphics.setTooltipForNextFrame(lines, mouseX, mouseY);
			}
		}
	}

	private AlchemistData alchemist() {
		AlchemistData data = minecraft == null || minecraft.player == null ? null
				: minecraft.player.getAttached(FmabAttachments.ALCHEMIST);
		return data == null ? AlchemistData.NEW : data;
	}

	@Override
	public void onClose() {
		if (minecraft != null) {
			minecraft.gui.setScreen(parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
