package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.knowledge.Knowledge;
import com.ajustor.fmab.alchemy.knowledge.KnowledgeLayout;
import com.ajustor.fmab.alchemy.knowledge.KnowledgeNode;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arbre de savoir : la maîtrise de chaque école et les nœuds qu'elle débloque. On progresse en
 * pratiquant ; cet écran ne fait que montrer où l'on en est. Les écoles se rangent en grille à la
 * largeur de l'écran ; ce qui dépasse en hauteur défile à la molette.
 */
public class KnowledgeScreen extends Screen {
	private static final int PAPER = 0xFFEDE3C8;
	private static final int EDGE = 0xFF6B4F2A;
	private static final int INK = 0xFF2B2B40;
	private static final int FADED = 0xFF8A7A60;
	private static final int DONE = 0xFF2E7D32;
	private static final int ROW = KnowledgeLayout.ROW;
	/** Le contenu commence sous le titre, le rang et le karma. */
	private static final int HEADER = 40;

	private final Screen parent;
	private final List<Hover> hovers = new ArrayList<>();
	private int scroll;
	private int maxScroll;
	/** Faux si, au dernier rendu, le livre ou une école sortait de l'écran. */
	private boolean fits = true;

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

		KnowledgeLayout layout = KnowledgeLayout.of(width, schools.values().stream().map(List::size).toList());
		int left = layout.left();
		int bookWidth = layout.width();
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
		graphics.text(font, karmaLine(), left + 8, top + 6, FADED, false);

		// Ce qui ne tient pas en hauteur défile, coupé aux bords de la page.
		int viewTop = top + HEADER;
		int viewBottom = bottom - 4;
		maxScroll = layout.maxScroll(viewBottom - viewTop);
		scroll = Mth.clamp(scroll, 0, maxScroll);
		int columnWidth = layout.columnWidth();
		int textWidth = columnWidth - 10;
		fits = left - 3 >= 0 && left + bookWidth + 3 <= width;

		hovers.clear();
		graphics.enableScissor(left, viewTop, left + bookWidth, viewBottom);
		int index = 0;
		for (Map.Entry<String, List<KnowledgeNode>> school : schools.entrySet()) {
			KnowledgeLayout.Cell cell = layout.cells().get(index++);
			int x = cell.x();
			int y = viewTop + cell.y() - scroll;
			fits &= x >= left && x + columnWidth <= left + bookWidth;
			graphics.text(font, fit(Component.translatable("knowledge.fmab.school." + school.getKey())
					.withStyle(s -> s.withBold(true)), textWidth), x, y, INK, false);
			y += ROW;
			graphics.text(font, fit(Component.translatable("knowledge.fmab.mastery",
					knowledge.mastery(school.getKey())), textWidth), x, y, FADED, false);
			y += ROW + 4;
			for (KnowledgeNode node : school.getValue()) {
				boolean has = knowledge.has(node.id());
				Component line = Component.literal(has ? "✔ " : "◻ ")
						.append(Component.translatable(node.nameKey()))
						.append(has ? "" : " (" + node.mastery() + ")");
				int indent = node.parent().isPresent() ? 8 : 0;
				graphics.text(font, fit(line, textWidth - indent), x + indent, y, has ? DONE : INK, false);
				// Un nœud caché par le défilement ne répond pas au survol.
				if (y - 1 >= viewTop && y + ROW - 2 <= viewBottom) {
					hovers.add(new Hover(x, y - 1, x + textWidth, y + ROW - 2, node));
				}
				y += ROW;
			}
		}
		graphics.disableScissor();
		if (maxScroll > 0) {
			// Un ascenseur discret au bord droit de la page.
			int track = viewBottom - viewTop;
			int thumb = Math.max(10, track * track / (track + maxScroll));
			int thumbTop = viewTop + (track - thumb) * scroll / maxScroll;
			graphics.fill(left + bookWidth - 5, viewTop, left + bookWidth - 3, viewBottom, 0x30000000);
			graphics.fill(left + bookWidth - 5, thumbTop, left + bookWidth - 3, thumbTop + thumb, EDGE);
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);

		for (Hover h : hovers) {
			if (mouseX >= h.x0 && mouseX < h.x1 && mouseY >= h.y0 && mouseY < h.y1) {
				List<FormattedCharSequence> lines = font.split(Component.translatable(h.node.descriptionKey()), 200);
				graphics.setTooltipForNextFrame(lines, mouseX, mouseY);
			}
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (maxScroll > 0 && scrollY != 0) {
			scroll = Mth.clamp(scroll - (int) Math.round(scrollY * ROW * 2), 0, maxScroll);
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	/** Vrai si, au dernier rendu, le livre et toutes les écoles tenaient dans la largeur de l'écran. */
	public boolean fitsOnScreen() {
		return fits;
	}

	/** La première ligne du texte à cette largeur : le survol garde la description complète. */
	private FormattedCharSequence fit(Component text, int maxWidth) {
		List<FormattedCharSequence> lines = font.split(text, Math.max(1, maxWidth));
		return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
	}

	private AlchemistData alchemist() {
		AlchemistData data = minecraft == null || minecraft.player == null ? null
				: FmabAttachments.ALCHEMIST.get(minecraft.player);
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

	/** Le karma, en toutes lettres : il se sent plus qu'il ne se compte. */
	private Component karmaLine() {
		Integer karma = minecraft == null || minecraft.player == null ? null
				: FmabAttachments.KARMA.get(minecraft.player);
		int k = karma == null ? 0 : karma;
		String step = k <= -60 ? "damned" : k <= -25 ? "dark" : k < 25 ? "neutral" : k < 60 ? "good" : "noble";
		return Component.translatable("knowledge.fmab.karma", Component.translatable("karma.fmab." + step), k);
	}
}
