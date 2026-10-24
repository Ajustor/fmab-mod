package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.Treatise;
import com.ajustor.fmab.item.Tomes;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.DrawingCode;
import com.ajustor.fmab.alchemy.drawing.SoulSeal;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.client.render.CircleTextures;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.network.AddToNotebookPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import com.ajustor.fmab.platform.client.ClientPlatform;

/**
 * Le Traité d'alchimie : sommaire, chapitres de texte, catalogue des glyphes (on y étudie chaque
 * glyphe pour le comprendre) et cercles d'exemple à recopier. Un chapitre encore scellé paraît au
 * sommaire sans titre, avec ce qu'il faut pour le lire ; ses pages n'y sont pas.
 */
public class TreatiseScreen extends Screen {
	private static final int BOOK_WIDTH = 320;
	/** Assez haut pour un sommaire de vingt titres sur deux lignes, assez bas pour la plus petite interface (240). */
	private static final int BOOK_HEIGHT = 232;
	private static final int PAPER = 0xFFEDE3C8;
	private static final int EDGE = 0xFF6B4F2A;
	private static final int INK = 0xFF2B2B40;
	private static final int FADED = 0xFF7A6A50;
	private static final int IMAGE = 96;
	/** Le fond d'un titre du sommaire survolé. */
	private static final int HOVER = 0x336B4F2A;

	/**
	 * Un titre du sommaire : sa place, et ses lignes une fois coupé à la largeur de la colonne ; un
	 * chapitre scellé ne s'ouvre pas.
	 */
	private record Link(int chapter, boolean open, int x, int y, int width, int height,
			List<FormattedCharSequence> lines) {
		boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
	}

	/** La hauteur d'un titre du sommaire : de quoi tenir sur deux lignes. */
	private static final int LINK_HEIGHT = 20;
	private final List<Link> links = new ArrayList<>();

	/**
	 * Une page affichée : son chapitre, ce qu'elle montre, et les lignes de son texte une fois coupé
	 * à sa largeur ({@code null} pour un glyphe). Un texte trop long se poursuit sur les pages
	 * suivantes, en pleine largeur.
	 */
	private record Entry(int chapter, Treatise.Page page, Glyph glyph, List<FormattedCharSequence> lines) {
	}

	/** Les lignes qui tiennent sous le titre du chapitre, au-dessus du numéro de page. */
	private static final int LINES = (BOOK_HEIGHT - 66) / 10;

	private final List<Entry> entries = new ArrayList<>();
	private Treatise treatise = new Treatise(List.of());
	/** −1 : sommaire. */
	private int index = -1;
	private boolean opened;
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
		// Un nouveau venu, qui n'a encore rien transmuté, ouvre le Traité à sa première page : les premiers pas.
		if (!opened) {
			opened = true;
			if (totalMastery(alchemist()) == 0 && !entries.isEmpty()) {
				index = 0;
			}
		}
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
		// Consigner son savoir dans un livre, pour un autre alchimiste.
		contentsButtons.add(addRenderableWidget(Button.builder(Component.translatable("tome.fmab.consign.open"),
				b -> minecraft.gui.setScreen(new ConsignScreen(this)))
				.bounds(left + 10, top + 6, 80, 16).build()));
		// Le sommaire, comme dans un livre : deux colonnes de titres qui passent à la ligne plutôt que d'être
		// coupés, et qu'on clique.
		links.clear();
		int rows = (treatise.chapters().size() + 1) / 2;
		int column = (BOOK_WIDTH - 24) / 2;
		int row = rows == 0 ? LINK_HEIGHT : Math.min(LINK_HEIGHT, (BOOK_HEIGHT - 52) / rows);
		for (int c = 0; c < treatise.chapters().size(); c++) {
			int x = left + 10 + (c / rows) * (column + 4);
			Treatise.Chapter chapter = treatise.chapters().get(c);
			boolean open = unlocked(chapter);
			List<FormattedCharSequence> lines = font.split(numbered(c, open ? Component.translatable(chapter.titleKey())
					: Component.literal("???")), column - 6);
			links.add(new Link(c, open, x, top + 26 + (c % rows) * row, column, row, lines));
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
			if (!unlocked(treatise.chapters().get(c))) {
				continue;
			}
			int chapter = c;
			for (Treatise.Page page : treatise.chapters().get(c).pages()) {
				switch (page) {
					case Treatise.GlyphCatalogue catalogue -> glyphs.forEach(g -> entries.add(new Entry(chapter, page, g, null)));
					case Treatise.TextPage text -> addText(chapter, page, text.textKey(), false);
					case Treatise.ExamplePage example -> addText(chapter, page, example.textKey(), true);
					case Treatise.SealPage seal -> addText(chapter, page, seal.textKey(), true);
				}
			}
		}
	}

	/**
	 * Coupe le texte d'une page en lignes, à côté de l'image s'il y en a une. Ce qui ne tient pas passe,
	 * paragraphe par paragraphe, sur des pages de texte qui suivent, en pleine largeur.
	 */
	private void addText(int chapter, Treatise.Page page, String textKey, boolean image) {
		Treatise.Page shown = page;
		int width = image ? BOOK_WIDTH - IMAGE - 36 : BOOK_WIDTH - 24;
		List<FormattedCharSequence> lines = new ArrayList<>();
		for (String paragraph : Component.translatable(textKey).getString().split("\n", -1)) {
			if (paragraph.isEmpty() && lines.isEmpty() && shown != page) {
				continue;
			}
			List<FormattedCharSequence> split = split(paragraph, width);
			if (!lines.isEmpty() && lines.size() + split.size() > LINES) {
				entries.add(new Entry(chapter, shown, null, lines));
				lines = new ArrayList<>();
				shown = new Treatise.TextPage(textKey);
				width = BOOK_WIDTH - 24;
				if (paragraph.isEmpty()) {
					continue;
				}
				split = split(paragraph, width);
			}
			for (FormattedCharSequence line : split) {
				if (lines.size() == LINES) {
					entries.add(new Entry(chapter, shown, null, lines));
					lines = new ArrayList<>();
					shown = new Treatise.TextPage(textKey);
				}
				lines.add(line);
			}
		}
		entries.add(new Entry(chapter, shown, null, lines));
	}

	private List<FormattedCharSequence> split(String paragraph, int width) {
		return paragraph.isEmpty() ? List.of(FormattedCharSequence.EMPTY) : font.split(Component.literal(paragraph), width);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (index < 0 && event.button() == 0) {
			for (Link link : links) {
				if (link.open() && link.contains(event.x(), event.y())) {
					minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
					go(firstPageOf(link.chapter()));
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private int firstPageOf(int chapter) {
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).chapter() == chapter) {
				return i;
			}
		}
		return -1;
	}

	/** Ouvre le Traité à la page de ce glyphe, dans le catalogue. */
	public void goToGlyph(String glyph) {
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).glyph() != null && entries.get(i).glyph().id().equals(glyph)) {
				go(i);
				return;
			}
		}
	}

	/** Tourne la page, comme la flèche du bas. */
	public void nextPage() {
		go(index + 1);
	}

	/** Ouvre le Traité au sommaire. */
	public void showContents() {
		go(-1);
	}

	/** Ouvre le Traité à la première page de ce texte ; rend faux s'il n'y est pas (chapitre scellé). */
	public boolean goToPage(String textKey) {
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).page() instanceof Treatise.TextPage t && t.textKey().equals(textKey)
					|| entries.get(i).page() instanceof Treatise.ExamplePage x && x.textKey().equals(textKey)) {
				go(i);
				return true;
			}
		}
		return false;
	}

	/**
	 * Le texte de la page ouverte, et si c'en est la suite ; {@code null} au sommaire ou sur un glyphe.
	 */
	public String currentText() {
		Entry e = current();
		if (e == null || e.glyph() != null) {
			return null;
		}
		String key = textOf(e);
		boolean continued = index > 0 && entries.get(index - 1).glyph() == null && key.equals(textOf(entries.get(index - 1)));
		return continued ? key + " (suite)" : key;
	}

	private static String textOf(Entry e) {
		return switch (e.page()) {
			case Treatise.TextPage t -> t.textKey();
			case Treatise.ExamplePage x -> x.textKey();
			case Treatise.SealPage seal -> seal.textKey();
			case Treatise.GlyphCatalogue g -> "";
		};
	}

	/** Ce chapitre est-il ouvert au lecteur (ses pages sont-elles dans le Traité) ? */
	public boolean opens(String titleKey) {
		return entries.stream().anyMatch(e -> treatise.chapters().get(e.chapter()).titleKey().equals(titleKey));
	}

	/** La page ouverte est-elle celle d'un glyphe que le lecteur voit en entier ? */
	public boolean showsRevealedGlyph() {
		Entry e = current();
		return e != null && e.glyph() != null && revealed(e.glyph());
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
			notebookButton.visible = revealed(e.glyph()) && glyphExample(e.glyph()).isPresent();
		} else if (e != null && (e.page() instanceof Treatise.ExamplePage || e.page() instanceof Treatise.SealPage)) {
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

	/** Appuie sur « Ajouter au carnet » de la page ouverte ; rend faux si la page n'a pas ce bouton. */
	public boolean copyToNotebook() {
		if (!notebookButton.visible || !notebookButton.active) {
			return false;
		}
		addToNotebook();
		return true;
	}

	/** Recopie dans le carnet le cercle de la page : il n'y a plus qu'à le tracer à la craie. */
	private void addToNotebook() {
		Entry e = current();
		if (e == null) {
			return;
		}
		if (e.glyph() != null) {
			glyphExample(e.glyph()).ifPresent(ex ->
					ClientPlatform.get().send(new AddToNotebookPayload("@" + ex.getKey(), ex.getValue())));
		} else if (e.page() instanceof Treatise.ExamplePage example) {
			String title = exampleTitle(Component.translatable(example.textKey()).getString());
			ClientPlatform.get().send(new AddToNotebookPayload(title, example.drawing()));
		} else if (e.page() instanceof Treatise.SealPage && minecraft != null && minecraft.player != null) {
			ClientPlatform.get().send(new AddToNotebookPayload(Component.translatable("gate.fmab.page.seal",
					minecraft.player.getName().getString()).getString(), mySeal()));
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
		} else if (e.page() instanceof Treatise.SealPage) {
			Minecraft.getInstance().keyboardHandler.setClipboard(DrawingCode.encode(mySeal()));
			actionButton.setMessage(Component.translatable("treatise.fmab.copied"));
		}
	}

	/** Le sceau de sang de qui lit le Traité. */
	private Drawing mySeal() {
		return minecraft == null || minecraft.player == null ? Drawing.EMPTY : SoulSeal.of(minecraft.player.getUUID());
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
			for (Link link : links) {
				boolean hovered = link.open() && link.contains(mouseX, mouseY);
				if (!link.open() && link.contains(mouseX, mouseY)) {
					// Ce qui manque pour lire un chapitre scellé, au pied du sommaire.
					graphics.centeredText(font, sealedHint(treatise.chapters().get(link.chapter())), left + BOOK_WIDTH / 2,
							top + BOOK_HEIGHT - 36, FADED);
				}
				if (hovered) {
					graphics.fill(link.x() - 2, link.y() - 1, link.x() + link.width(), link.y() + link.height() - 1, HOVER);
				}
				int lineY = link.y() + (link.height() - Math.min(2, link.lines().size()) * 9) / 2;
				for (FormattedCharSequence line : link.lines().subList(0, Math.min(2, link.lines().size()))) {
					graphics.text(font, line, link.x() + 2, lineY, !link.open() ? FADED : hovered ? EDGE : INK, false);
					lineY += 9;
				}
			}
		} else {
			Component chapter = numbered(e.chapter(),
					Component.translatable(treatise.chapters().get(e.chapter()).titleKey()));
			graphics.centeredText(font, chapter, left + BOOK_WIDTH / 2, top + 10, FADED);
			graphics.text(font, Component.literal((index + 1) + " / " + entries.size()),
					left + BOOK_WIDTH / 2 - 10, top + BOOK_HEIGHT - 36, FADED, false);
			if (e.glyph() != null) {
				drawGlyph(graphics, e.glyph());
			} else {
				if (e.page() instanceof Treatise.ExamplePage example) {
					drawImage(graphics, example.drawing());
				} else if (e.page() instanceof Treatise.SealPage) {
					drawImage(graphics, mySeal());
				}
				int y = top + 26;
				for (FormattedCharSequence line : e.lines()) {
					graphics.text(font, line, left + 12, y, INK, false);
					y += 10;
				}
			}
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	private void drawGlyph(GuiGraphicsExtractor graphics, Glyph g) {
		if (!revealed(g)) {
			drawUnknown(graphics, g);
			return;
		}
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

	/** « IV. Composer un cercle » : le numéro suit la place du chapitre dans le plan. */
	private static Component numbered(int chapter, Component title) {
		return Component.literal(Treatise.numeral(chapter + 1) + ". ").append(title);
	}

	/** Le lecteur a-t-il assez progressé pour lire ce chapitre ? */
	private boolean unlocked(Treatise.Chapter chapter) {
		AlchemistData me = alchemist();
		return chapter.unlock().met(me.rank(), totalMastery(me));
	}

	/** Ce qui manque pour lire un chapitre scellé : le rang d'abord, puis la maîtrise. */
	private Component sealedHint(Treatise.Chapter chapter) {
		AlchemistData me = alchemist();
		return me.rank().atLeast(chapter.unlock().rank())
				? Component.translatable("treatise.fmab.sealed.mastery", totalMastery(me), chapter.unlock().mastery())
				: Component.translatable("treatise.fmab.sealed.rank",
						Component.translatable(chapter.unlock().rank().translationKey()));
	}

	private static int totalMastery(AlchemistData me) {
		return me.mastery().values().stream().mapToInt(Integer::intValue).sum();
	}

	private boolean revealed(Glyph g) {
		AlchemistData me = alchemist();
		return Treatise.revealed(g.id(), me.known(), me.uses(g.id()));
	}

	/** Un glyphe ni appris ni tracé : une page blanche, et où l'apprendre. */
	private void drawUnknown(GuiGraphicsExtractor graphics, Glyph g) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal("???").withStyle(s -> s.withBold(true)));
		lines.add(Component.translatable("treatise.fmab.rank", Component.translatable(g.rank().translationKey())));
		lines.add(Component.empty());
		lines.add(Component.translatable("treatise.fmab.unknown"));
		Tomes.teaching(g.id()).ifPresent(tome -> {
			lines.add(Component.empty());
			lines.add(Component.translatable("treatise.fmab.unknown.tome", Component.translatable(tome.titleKey())));
			lines.add(Component.translatable(Tomes.HIDDEN.contains(tome)
					? "treatise.fmab.unknown.where." + tome.titleKey().substring("tome.fmab.".length())
					: "treatise.fmab.unknown.where.library"));
		});
		int y = top + 26;
		for (Component line : lines) {
			if (line.getString().isEmpty()) {
				y += 6;
			}
			for (FormattedCharSequence seq : font.split(line, BOOK_WIDTH - 24)) {
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

	private void drawImage(GuiGraphicsExtractor graphics, Drawing d) {
		int x = left + BOOK_WIDTH - 12 - IMAGE;
		int y = top + 26;
		graphics.outline(x - 1, y - 1, IMAGE + 2, IMAGE + 2, FADED);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CircleTextures.get(d, INK), x, y, 0, 0, IMAGE, IMAGE,
				CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE);
	}

	private AlchemistData alchemist() {
		AlchemistData data = minecraft == null || minecraft.player == null ? null
				: FmabAttachments.ALCHEMIST.get(minecraft.player);
		return data == null ? AlchemistData.NEW : data;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
