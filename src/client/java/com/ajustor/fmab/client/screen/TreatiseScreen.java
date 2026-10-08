package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.Treatise;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.DrawingCode;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.client.render.CircleTextures;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.network.AddToNotebookPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Le Traité d'alchimie : sommaire, chapitres de texte, catalogue des glyphes (on y étudie chaque
 * glyphe pour le comprendre) et cercles d'exemple à recopier.
 */
public class TreatiseScreen extends Screen {
	private static final int BOOK_WIDTH = 320;
	private static final int BOOK_HEIGHT = 200;
	private static final int PAPER = 0xFFEDE3C8;
	private static final int EDGE = 0xFF6B4F2A;
	private static final int INK = 0xFF2B2B40;
	private static final int FADED = 0xFF7A6A50;
	private static final int IMAGE = 96;

	/** Une page affichée : son chapitre et ce qu'elle montre. */
	private record Entry(int chapter, Treatise.Page page, Glyph glyph) {
	}

	private final List<Entry> entries = new ArrayList<>();
	private Treatise treatise = new Treatise(List.of());
	/** −1 : sommaire. */
	private int index = -1;
	private int left;
	private int top;
	private Button actionButton;
	/** Second bouton : ajouter le cercle (de l'exemple, ou un cercle simple du glyphe) au carnet. */
	private Button notebookButton;
	private final List<Button> contentsButtons = new ArrayList<>();

	public TreatiseScreen() {
		super(Component.translatable("item.fmab.alchemy_treatise"));
	}

	@Override
	protected void init() {
		load();
		left = (width - BOOK_WIDTH) / 2;
		top = (height - BOOK_HEIGHT) / 2;
		addRenderableWidget(Button.builder(Component.literal("<"), b -> go(index - 1))
				.bounds(left + 6, top + BOOK_HEIGHT - 24, 20, 18).build());
		addRenderableWidget(Button.builder(Component.literal(">"), b -> go(index + 1))
				.bounds(left + BOOK_WIDTH - 26, top + BOOK_HEIGHT - 24, 20, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("treatise.fmab.contents"), b -> go(-1))
				.bounds(left + BOOK_WIDTH / 2 - 40, top + BOOK_HEIGHT - 24, 80, 18).build());
		actionButton = addRenderableWidget(Button.builder(Component.empty(), b -> action())
				.bounds(left + BOOK_WIDTH - 12 - IMAGE, top + 30 + IMAGE + 6, IMAGE, 18).build());
		notebookButton = addRenderableWidget(Button.builder(Component.translatable("treatise.fmab.add_to_notebook"),
				b -> addToNotebook()).bounds(left + BOOK_WIDTH - 12 - IMAGE, top + 30 + IMAGE + 26, IMAGE, 18).build());
		contentsButtons.clear();
		contentsButtons.add(addRenderableWidget(Button.builder(Component.translatable("knowledge.fmab.title"),
				b -> minecraft.gui.setScreen(new KnowledgeScreen(this)))
				.bounds(left + BOOK_WIDTH - 90, top + 6, 80, 16).build()));
		for (int c = 0; c < treatise.chapters().size(); c++) {
			int chapter = c;
			Button b = addRenderableWidget(Button.builder(Component.translatable(treatise.chapters().get(c).titleKey()),
					btn -> go(firstPageOf(chapter))).bounds(left + 30, top + 26 + c * 19, BOOK_WIDTH - 60, 17).build());
			contentsButtons.add(b);
		}
		go(index);
	}

	private void load() {
		entries.clear();
		try (Reader reader = Minecraft.getInstance().getResourceManager()
				.openAsReader(Fmab.id("treatise/treatise.json"))) {
			treatise = Treatise.parse(JsonParser.parseReader(reader).getAsJsonObject());
		} catch (Exception e) {
			Fmab.LOGGER.error("Traité d'alchimie illisible", e);
			treatise = new Treatise(List.of());
		}
		List<Glyph> glyphs = minecraft != null && minecraft.level != null
				? new ArrayList<>(AlchemyRules.of(minecraft.level.registryAccess()).glyphs())
				: new ArrayList<>();
		glyphs.sort(Comparator.comparing(Glyph::rank).thenComparing(Glyph::layer).thenComparing(Glyph::id));
		for (int c = 0; c < treatise.chapters().size(); c++) {
			for (Treatise.Page page : treatise.chapters().get(c).pages()) {
				if (page instanceof Treatise.GlyphCatalogue) {
					for (Glyph g : glyphs) {
						entries.add(new Entry(c, page, g));
					}
				} else {
					entries.add(new Entry(c, page, null));
				}
			}
		}
	}

	private int firstPageOf(int chapter) {
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).chapter() == chapter) {
				return i;
			}
		}
		return -1;
	}

	private void go(int target) {
		index = Math.clamp(target, -1, entries.size() - 1);
		contentsButtons.forEach(b -> b.visible = index < 0);
		Entry e = current();
		actionButton.visible = false;
		notebookButton.visible = false;
		notebookButton.active = true;
		notebookButton.setMessage(Component.translatable("treatise.fmab.add_to_notebook"));
		if (e != null && e.glyph() != null) {
			notebookButton.visible = glyphExample(e.glyph()).isPresent();
		} else if (e != null && e.page() instanceof Treatise.ExamplePage) {
			actionButton.visible = true;
			actionButton.active = true;
			actionButton.setMessage(Component.translatable("treatise.fmab.copy"));
			notebookButton.visible = true;
		}
	}

	/** Le cercle simple qui montre ce glyphe à l'œuvre, avec le nom de son effet. */
	private Optional<Map.Entry<String, Drawing>> glyphExample(Glyph glyph) {
		if (minecraft == null || minecraft.level == null) {
			return Optional.empty();
		}
		AlchemyRules rules = AlchemyRules.of(minecraft.level.registryAccess());
		return rules.simpleCombinationWith(glyph).flatMap(c -> rules.simpleCircle(c)
				.map(d -> Map.entry("effect." + c.effect().replace(':', '.'), d)));
	}

	/** Recopie dans le carnet le cercle de la page : il n'y a plus qu'à le tracer à la craie. */
	private void addToNotebook() {
		Entry e = current();
		if (e == null) {
			return;
		}
		if (e.glyph() != null) {
			glyphExample(e.glyph()).ifPresent(ex ->
					ClientPlayNetworking.send(new AddToNotebookPayload("@" + ex.getKey(), ex.getValue())));
		} else if (e.page() instanceof Treatise.ExamplePage example) {
			String title = exampleTitle(Component.translatable(example.textKey()).getString());
			ClientPlayNetworking.send(new AddToNotebookPayload(title, example.drawing()));
		}
		notebookButton.active = false;
		notebookButton.setMessage(Component.translatable("treatise.fmab.added_short"));
	}

	/** « Lance de pierre (Alchimiste, …) : Terre et… » donne « Lance de pierre ». */
	static String exampleTitle(String text) {
		int end = text.length();
		for (String stop : new String[]{" :", ":", " (", "\n"}) {
			int i = text.indexOf(stop);
			if (i > 0) {
				end = Math.min(end, i);
			}
		}
		return text.substring(0, end).strip();
	}

	private void action() {
		Entry e = current();
		if (e == null) {
			return;
		}
		if (e.page() instanceof Treatise.ExamplePage example) {
			Minecraft.getInstance().keyboardHandler.setClipboard(DrawingCode.encode(example.drawing()));
			actionButton.setMessage(Component.translatable("treatise.fmab.copied"));
		}
	}

	private Entry current() {
		return index >= 0 && index < entries.size() ? entries.get(index) : null;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(left - 3, top - 3, left + BOOK_WIDTH + 3, top + BOOK_HEIGHT + 3, EDGE);
		graphics.fill(left, top, left + BOOK_WIDTH, top + BOOK_HEIGHT, PAPER);
		Entry e = current();
		if (e == null) {
			graphics.centeredText(font, title, left + BOOK_WIDTH / 2, top + 12, INK);
		} else {
			Component chapter = Component.translatable(treatise.chapters().get(e.chapter()).titleKey());
			graphics.centeredText(font, chapter, left + BOOK_WIDTH / 2, top + 10, FADED);
			graphics.text(font, Component.literal((index + 1) + " / " + entries.size()),
					left + BOOK_WIDTH / 2 - 10, top + BOOK_HEIGHT - 36, FADED, false);
			if (e.glyph() != null) {
				drawGlyph(graphics, e.glyph());
			} else if (e.page() instanceof Treatise.ExamplePage example) {
				drawImage(graphics, example.drawing());
				drawText(graphics, Component.translatable(example.textKey()), BOOK_WIDTH - IMAGE - 36);
			} else if (e.page() instanceof Treatise.TextPage text) {
				drawText(graphics, Component.translatable(text.textKey()), BOOK_WIDTH - 24);
			}
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	private void drawGlyph(GuiGraphicsExtractor graphics, Glyph g) {
		// Le glyphe est défini dans le carré −1..1 : on le pose au centre d'une page de carnet.
		Drawing d = new Drawing(g.primitives().stream()
				.map(p -> p.map(v -> v.scale(12).add(new Vec2(Drawing.CENTER, Drawing.CENTER))))
				.toList());
		drawImage(graphics, d);
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable(g.nameKey()).withStyle(s -> s.withBold(true)));
		lines.add(Component.translatable("treatise.fmab.layer." + g.layer().name().toLowerCase(Locale.ROOT)));
		lines.add(Component.translatable("treatise.fmab.cost", g.complexity(), g.concentration()));
		lines.add(Component.translatable("treatise.fmab.rank", Component.translatable(g.rank().translationKey())));
		lines.add(understanding(g));
		lines.add(Component.empty());
		lines.add(Component.translatable(g.descriptionKey()));
		glyphExample(g).ifPresent(ex -> {
			lines.add(Component.empty());
			lines.add(Component.translatable("treatise.fmab.example_line", Component.translatable(ex.getKey())));
		});
		int y = top + 26;
		for (Component line : lines) {
			for (FormattedCharSequence seq : font.split(line, BOOK_WIDTH - IMAGE - 36)) {
				graphics.text(font, seq, left + 12, y, INK, false);
				y += 10;
			}
		}
	}

	/**
	 * Le Traité décrit, il n'enseigne pas : on comprend un glyphe grâce à un tome, ou à force de le
	 * tracer.
	 */
	private Component understanding(Glyph g) {
		AlchemistData me = alchemist();
		if (me.known().contains(g.id())) {
			return Component.translatable("treatise.fmab.understood").withStyle(s -> s.withColor(0x2E7D32));
		}
		return Component.translatable("treatise.fmab.not_understood", me.uses(g.id()), AlchemistData.USES_TO_LEARN)
				.withStyle(s -> s.withColor(0xA8321E));
	}

	private void drawText(GuiGraphicsExtractor graphics, Component text, int width) {
		int y = top + 26;
		for (FormattedCharSequence seq : font.split(text, width)) {
			if (y > top + BOOK_HEIGHT - 40) {
				break;
			}
			graphics.text(font, seq, left + 12, y, INK, false);
			y += 10;
		}
	}

	private void drawImage(GuiGraphicsExtractor graphics, Drawing d) {
		int x = left + BOOK_WIDTH - 12 - IMAGE;
		int y = top + 26;
		graphics.outline(x - 1, y - 1, IMAGE + 2, IMAGE + 2, FADED);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CircleTextures.get(d, INK), x, y, 0, 0, IMAGE, IMAGE,
				CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE);
	}

	private AlchemistData alchemist() {
		AlchemistData data = minecraft == null || minecraft.player == null ? null
				: minecraft.player.getAttached(FmabAttachments.ALCHEMIST);
		return data == null ? AlchemistData.NEW : data;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
