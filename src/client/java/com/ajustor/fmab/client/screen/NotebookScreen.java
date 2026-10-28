package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.PlacedGlyph;
import com.ajustor.fmab.alchemy.circle.Stage;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.DrawingCode;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.SimpleCircles;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphLayer;
import com.ajustor.fmab.alchemy.glyph.GlyphPicker;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.client.render.CircleTextures;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.network.SaveNotebookPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.IssueText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.ajustor.fmab.platform.client.ClientPlatform;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Carnet de cercles (sa touche l'ouvre) : on y trace ses cercles sur une grille de 32 cases, on les
 * nomme, on les range (l'ordre des pages est celui de la roue des cercles), et le carnet dit ce qu'il
 * lit avant qu'on aille les tracer à la craie.
 */
public class NotebookScreen extends Screen {
	private enum Tool {
		LINE, CIRCLE, POLYGON, ARC, DOT,
		/** Pose d'un clic un glyphe compris : pas besoin de le retracer trait par trait. */
		STAMP;

		Component label() {
			return Component.translatable("notebook.fmab.tool." + name().toLowerCase(Locale.ROOT));
		}
	}

	private static final int PAPER = 0xFFEDE3C8;
	private static final int GRID_LINE = 0x22000000;
	private static final int AXIS_LINE = 0x44000000;
	private static final int INK = 0xFF2B2B40;
	private static final int PREVIEW = 0xFF3A6FD8;
	private static final int GLYPH_MARK = 0xFF2E9E4F;
	private static final int ISSUE_MARK = 0xFFD03A2F;
	private static final int TEXT = 0xFFE0E0E0;
	private static final int DIM = 0xFFA8A8A8;
	private static final int GOOD = 0xFF7FD67F;
	private static final int BAD = 0xFFFF7A6A;
	private static final int WARN = 0xFFFFB45A;
	private static final int EFFECT = 0xFFF0D080;
	private static final int STATUS = 0xFF9CC8FF;
	private static final int PANEL_BG = 0xB0101018;
	private static final int PANEL_WIDTH = 190;

	private NotebookContents contents;
	private int page;
	private List<Primitive> strokes;

	private Tool tool = Tool.LINE;
	private int sides = 3;
	private boolean symmetry;
	/** Point de départ du geste en cours (coordonnées de grille), ou null. */
	private Vec2 anchor;
	/** Pour l'arc : point de départ fixé, on choisit maintenant l'angle de fin. */
	private Vec2 arcStart;
	private Vec2 cursor = Drawing.CENTER_POINT;

	private Analysis cachedAnalysis;
	private Drawing analyzed;
	private Component status = Component.empty();

	private int cell;
	private int canvasX;
	private int canvasY;
	private EditBox nameBox;
	private final List<Button> toolButtons = new ArrayList<>();
	private Button sidesButton;
	private Button symmetryButton;
	private Button stampButton;
	private Button stampSizeButton;
	/** Glyphe du tampon, parmi ceux que le joueur comprend (null : le premier) ; demi-taille et rotation. */
	private String stampId;
	private int stampHalfSize = 3;
	/** Tailles du tampon, en demi-largeur : de 6 à 12 cases, assez grand pour être reconnu. */
	private static final int MIN_STAMP_HALF_SIZE = 3;
	private static final int MAX_STAMP_HALF_SIZE = 6;
	private int stampRotation;
	/** Nombre de traits ajoutés par chaque geste, pour qu'Annuler défasse un tampon d'un coup. */
	private final ArrayDeque<Integer> gestures = new ArrayDeque<>();
	/** Haut du panneau d'analyse. */
	private int panelTop;
	/** Le numéro de page : un clic ouvre le sommaire. */
	private Button pageButton;
	/** Le sommaire, ouvert à la place de la grille : toutes les pages par leur nom, à cliquer. */
	private boolean contentsOpen;
	private int contentsScroll;
	/** Une seule colonne : les noms se lisent en entier. */
	private static final int CONTENTS_COLUMNS = 1;
	private static final int CONTENTS_ROW = 14;
	private static final int CONTENTS_THUMB = 12;
	private static final int CONTENTS_TOP = 16;
	private static final int CONTENTS_HOVER = 0x30000000;
	private static final int CONTENTS_CURRENT = 0x40E0B040;
	/**
	 * Le sélecteur de glyphes, ouvert à la place de la grille comme le sommaire : une recherche, un
	 * onglet par couche, et les glyphes compris en miniatures, les derniers posés en tête.
	 */
	private boolean pickerOpen;
	/** La couche montrée, ou null pour toutes. */
	private GlyphLayer pickerLayer;
	private int pickerScroll;
	private EditBox searchBox;
	/** Le glyphe sous la souris dans le sélecteur : le panneau le décrit. */
	private Glyph pickerHovered;
	/** Les tracés des miniatures, gardés d'une image à l'autre : leur texture se retrouve sans recalcul. */
	private final Map<String, Drawing> thumbnails = new HashMap<>();
	/** Les derniers glyphes posés, le plus récent d'abord ; ils restent d'une ouverture du carnet à l'autre. */
	private static List<String> recentStamps = List.of();
	/** La touche G vient d'ouvrir le sélecteur : son caractère ne s'écrit pas dans la recherche. */
	private boolean swallowNextChar;
	private static final int PICKER_TABS = 22;
	private static final int PICKER_TOP = 38;
	private static final int PICKER_CELL = 48;
	private static final int PICKER_THUMB = 28;
	private static final int PICKER_ROW = 42;
	private static final int THUMB_HALF_SIZE = 12;
	private static final GlyphLayer[] PICKER_LAYERS = {null, GlyphLayer.ELEMENT, GlyphLayer.ACTION, GlyphLayer.MODIFIER};

	/** Une ligne du panneau d'analyse et sa couleur. */
	private record Line(Component text, int color) {
	}

	public NotebookScreen() {
		super(Component.translatable("key.fmab.notebook"));
		Player player = Minecraft.getInstance().player;
		this.contents = player == null ? NotebookContents.EMPTY : Notebooks.of(player);
		this.page = contents.selected();
		this.strokes = new ArrayList<>(contents.pages().isEmpty() ? List.of() : contents.pages().get(page).drawing().primitives());
	}

	@Override
	protected void init() {
		cell = Math.max(4, Math.min(8, (height - 24) / Drawing.GRID));
		int canvasSize = cell * Drawing.GRID;
		int total = canvasSize + 10 + PANEL_WIDTH;
		canvasX = Math.max(4, (width - total) / 2);
		canvasY = Math.max(4, (height - canvasSize) / 2);
		int px = canvasX + canvasSize + 10;
		int y = canvasY;

		toolButtons.clear();
		int tx = px;
		for (Tool t : Tool.values()) {
			if (t == Tool.STAMP) {
				continue;
			}
			Button b = addRenderableWidget(Button.builder(t.label(), btn -> selectTool(t)).bounds(tx, y, 37, 18).build());
			toolButtons.add(b);
			tx += 38;
		}
		y += 20;
		sidesButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
			sides = sides >= 12 ? 3 : sides + 1;
			refreshLabels();
		}).bounds(px, y, 92, 18).build());
		symmetryButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
			symmetry = !symmetry;
			refreshLabels();
		}).bounds(px + 94, y, 96, 18).build());
		y += 20;
		// Tampon : < glyphe > et sa taille. Le nom ouvre le sélecteur ; la molette fait tourner le tampon.
		addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleStamp(-1)).bounds(px, y, 20, 18).build());
		stampButton = addRenderableWidget(Button.builder(Component.empty(), b -> togglePicker())
				.tooltip(Tooltip.create(Component.translatable("notebook.fmab.stamp.tooltip")))
				.bounds(px + 22, y, 104, 18).build());
		addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleStamp(1)).bounds(px + 128, y, 20, 18).build());
		stampSizeButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
			stampHalfSize = stampHalfSize >= MAX_STAMP_HALF_SIZE ? MIN_STAMP_HALF_SIZE : stampHalfSize + 1;
			refreshLabels();
		}).bounds(px + 150, y, 40, 18).build());
		y += 20;
		addRenderableWidget(Button.builder(Component.translatable("notebook.fmab.undo"), b -> undo())
				.bounds(px, y, 62, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("notebook.fmab.clear"), b -> {
			strokes.clear();
			resetGesture();
			forgetGestures();
		}).bounds(px + 64, y, 62, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("notebook.fmab.save"), b -> save(true))
				.bounds(px + 128, y, 62, 18).build());
		y += 22;
		nameBox = addRenderableWidget(new EditBox(font, px, y, 126, 18, Component.translatable("notebook.fmab.name")));
		nameBox.setMaxLength(NotebookContents.MAX_NAME_LENGTH);
		nameBox.setValue(currentName());
		addRenderableWidget(Button.builder(Component.translatable("notebook.fmab.new_page"), b -> newPage())
				.bounds(px + 128, y, 62, 18).build());
		y += 20;
		addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(px, y, 20, 18).build());
		pageButton = addRenderableWidget(Button.builder(pageLabel(), b -> toggleContents())
				.tooltip(Tooltip.create(Component.translatable("notebook.fmab.contents.tooltip")))
				.bounds(px + 22, y, 82, 18).build());
		addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(px + 106, y, 20, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("notebook.fmab.delete_page"), b -> deletePage())
				.bounds(px + 128, y, 62, 18).build());
		y += 20;
		addRenderableWidget(Button.builder(Component.translatable("notebook.fmab.export"), b -> exportCode())
				.tooltip(Tooltip.create(Component.translatable("notebook.fmab.export.tooltip")))
				.bounds(px, y, 66, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("notebook.fmab.import"), b -> importCode())
				.tooltip(Tooltip.create(Component.translatable("notebook.fmab.import.tooltip")))
				.bounds(px + 68, y, 66, 18).build());
		// Ranger la page : son rang dans le carnet est sa place sur la roue des cercles.
		addRenderableWidget(Button.builder(Component.literal("«"), b -> move(-1))
				.tooltip(Tooltip.create(Component.translatable("notebook.fmab.move_earlier")))
				.bounds(px + 136, y, 26, 18).build());
		addRenderableWidget(Button.builder(Component.literal("»"), b -> move(1))
				.tooltip(Tooltip.create(Component.translatable("notebook.fmab.move_later")))
				.bounds(px + 164, y, 26, 18).build());
		panelTop = y + 26;
		// La recherche du sélecteur : posée sur le papier, elle se dessine après lui (voir drawPicker).
		int size = cell * Drawing.GRID;
		String query = searchBox == null ? "" : searchBox.getValue();
		searchBox = addWidget(new EditBox(font, canvasX + 4, canvasY + 4, size - 8, 16,
				Component.translatable("notebook.fmab.picker.search")));
		searchBox.setHint(Component.translatable("notebook.fmab.picker.search"));
		searchBox.setValue(query);
		searchBox.setResponder(s -> pickerScroll = 0);
		searchBox.visible = pickerOpen;
		refreshLabels();
	}

	// ---- Rendu ---------------------------------------------------------------------------------

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		int size = cell * Drawing.GRID;
		graphics.fill(canvasX - 2, canvasY - 2, canvasX + size + 2, canvasY + size + 2, 0xFF8A7A5A);
		graphics.fill(canvasX, canvasY, canvasX + size, canvasY + size, PAPER);
		pageButton.setMessage(pageLabel());
		if (contentsOpen) {
			drawContents(graphics, mouseX, mouseY);
			drawPanel(graphics);
			return;
		}
		if (pickerOpen) {
			drawPicker(graphics, mouseX, mouseY, a);
			drawPanel(graphics);
			return;
		}
		for (int i = 0; i <= Drawing.GRID; i++) {
			int color = i == Drawing.GRID / 2 ? AXIS_LINE : GRID_LINE;
			graphics.fill(canvasX + i * cell, canvasY, canvasX + i * cell + 1, canvasY + size, color);
			graphics.fill(canvasX, canvasY + i * cell, canvasX + size, canvasY + i * cell + 1, color);
		}

		Drawing drawing = new Drawing(strokes);
		if (!drawing.isEmpty()) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, CircleTextures.get(drawing, INK), canvasX, canvasY, 0, 0,
					size, size, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE);
		}
		Primitive preview = preview();
		if (preview != null) {
			plot(graphics, preview, PREVIEW);
			if (symmetry) {
				plot(graphics, mirror(preview), PREVIEW);
			}
		}
		if (tool == Tool.STAMP && insideCanvas(mouseX, mouseY)) {
			for (Primitive p : stampAt(cursor)) {
				plot(graphics, p, PREVIEW);
			}
		}

		Analysis analysis = analysis();
		if (analysis != null) {
			for (Stage stage : analysis.parsed().stages()) {
				for (PlacedGlyph g : stage.glyphs()) {
					mark(graphics, g.position().add(Drawing.CENTER_POINT), GLYPH_MARK);
				}
			}
			for (CircleIssue issue : analysis.issues()) {
				if (issue.where() != null) {
					mark(graphics, issue.where(), ISSUE_MARK);
				}
			}
		}
		// Curseur accroché à la grille.
		if (insideCanvas(mouseX, mouseY)) {
			int cx = screenX(cursor.x()), cy = screenY(cursor.y());
			graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, PREVIEW);
		}
		drawPanel(graphics);
	}

	/** Le sommaire : chaque page, sa miniature et son nom ; la page ouverte est surlignée. */
	private void drawContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int size = cell * Drawing.GRID;
		graphics.centeredText(font, Component.translatable("notebook.fmab.contents"), canvasX + size / 2, canvasY + 4,
				INK);
		contentsScroll = Math.clamp(contentsScroll, 0, maxContentsScroll());
		int hovered = contentsEntryAt(mouseX, mouseY);
		int columnWidth = size / CONTENTS_COLUMNS;
		graphics.enableScissor(canvasX, canvasY + CONTENTS_TOP, canvasX + size, canvasY + size);
		for (int i = 0; i < contents.pages().size(); i++) {
			int x = canvasX + (i % CONTENTS_COLUMNS) * columnWidth;
			int y = canvasY + CONTENTS_TOP + (i / CONTENTS_COLUMNS) * CONTENTS_ROW - contentsScroll;
			if (y + CONTENTS_ROW < canvasY || y > canvasY + size) {
				continue;
			}
			if (i == page) {
				graphics.fill(x + 1, y, x + columnWidth - 1, y + CONTENTS_ROW - 1, CONTENTS_CURRENT);
			} else if (i == hovered) {
				graphics.fill(x + 1, y, x + columnWidth - 1, y + CONTENTS_ROW - 1, CONTENTS_HOVER);
			}
			NotebookContents.Page p = contents.pages().get(i);
			if (!p.drawing().isEmpty()) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, CircleTextures.get(p.drawing(), INK), x + 3, y + 1, 0, 0,
						CONTENTS_THUMB, CONTENTS_THUMB, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE,
						CircleTextures.SIZE);
			}
			String name = (i + 1) + ". " + displayName(p.name());
			graphics.text(font, font.plainSubstrByWidth(name, columnWidth - CONTENTS_THUMB - 10),
					x + CONTENTS_THUMB + 6, y + (CONTENTS_ROW - 8) / 2 - 1, INK, false);
		}
		graphics.disableScissor();
	}

	/** La page du sommaire sous la souris, ou −1. */
	private int contentsEntryAt(double x, double y) {
		int size = cell * Drawing.GRID;
		if (x < canvasX || x >= canvasX + size || y < canvasY + CONTENTS_TOP || y >= canvasY + size) {
			return -1;
		}
		int column = (int) ((x - canvasX) / (size / CONTENTS_COLUMNS));
		int row = (int) ((y - canvasY - CONTENTS_TOP + contentsScroll) / CONTENTS_ROW);
		int index = row * CONTENTS_COLUMNS + Math.min(column, CONTENTS_COLUMNS - 1);
		return index < contents.pages().size() ? index : -1;
	}

	private int maxContentsScroll() {
		int rows = (contents.pages().size() + CONTENTS_COLUMNS - 1) / CONTENTS_COLUMNS;
		return Math.max(0, rows * CONTENTS_ROW - (cell * Drawing.GRID - CONTENTS_TOP));
	}

	/** Le sélecteur : la recherche, les onglets des couches, puis les glyphes retenus en miniatures. */
	private void drawPicker(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int size = cell * Drawing.GRID;
		searchBox.extractRenderState(graphics, mouseX, mouseY, a);
		int tabWidth = (size - 8) / PICKER_LAYERS.length;
		for (int i = 0; i < PICKER_LAYERS.length; i++) {
			int x = canvasX + 4 + i * tabWidth, y = canvasY + PICKER_TABS;
			boolean current = PICKER_LAYERS[i] == pickerLayer;
			boolean over = mouseX >= x && mouseX < x + tabWidth && mouseY >= y && mouseY < y + 12;
			if (current || over) {
				graphics.fill(x + 1, y, x + tabWidth - 1, y + 12, current ? CONTENTS_CURRENT : CONTENTS_HOVER);
			}
			String label = font.plainSubstrByWidth(layerLabel(PICKER_LAYERS[i]).getString(), tabWidth - 4);
			graphics.centeredText(font, Component.literal(label), x + tabWidth / 2, y + 2, INK);
		}
		pickerScroll = Math.clamp(pickerScroll, 0, maxPickerScroll());
		List<Glyph> glyphs = pickerGlyphs();
		int hovered = pickerEntryAt(mouseX, mouseY);
		pickerHovered = hovered >= 0 ? glyphs.get(hovered) : null;
		if (glyphs.isEmpty()) {
			graphics.centeredText(font, Component.translatable("notebook.fmab.picker.empty"), canvasX + size / 2,
					canvasY + PICKER_TOP + 8, INK);
			return;
		}
		int columns = pickerColumns();
		int columnWidth = (size - 8) / columns;
		Glyph current = stampGlyph();
		graphics.enableScissor(canvasX, canvasY + PICKER_TOP, canvasX + size, canvasY + size);
		for (int i = 0; i < glyphs.size(); i++) {
			int x = canvasX + 4 + (i % columns) * columnWidth;
			int y = canvasY + PICKER_TOP + (i / columns) * PICKER_ROW - pickerScroll;
			if (y + PICKER_ROW < canvasY + PICKER_TOP || y > canvasY + size) {
				continue;
			}
			Glyph g = glyphs.get(i);
			if (g.equals(current)) {
				graphics.fill(x + 1, y, x + columnWidth - 1, y + PICKER_ROW - 1, CONTENTS_CURRENT);
			} else if (i == hovered) {
				graphics.fill(x + 1, y, x + columnWidth - 1, y + PICKER_ROW - 1, CONTENTS_HOVER);
			}
			graphics.blit(RenderPipelines.GUI_TEXTURED, CircleTextures.get(thumbnail(g), INK),
					x + (columnWidth - PICKER_THUMB) / 2, y + 2, 0, 0, PICKER_THUMB, PICKER_THUMB,
					CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE, CircleTextures.SIZE);
			String name = font.plainSubstrByWidth(glyphName(g), columnWidth - 2);
			graphics.centeredText(font, Component.literal(name), x + columnWidth / 2, y + PICKER_THUMB + 4, INK);
		}
		graphics.disableScissor();
	}

	private Component layerLabel(GlyphLayer layer) {
		return Component.translatable("notebook.fmab.picker." + (layer == null ? "all" : layer.name().toLowerCase(Locale.ROOT)));
	}

	/** Le tracé de référence d'un glyphe, au milieu de la grille : sa miniature. */
	private Drawing thumbnail(Glyph g) {
		return thumbnails.computeIfAbsent(g.id(),
				id -> new Drawing(SimpleCircles.place(g, Drawing.CENTER_POINT, THUMB_HALF_SIZE, 0)));
	}

	private int pickerColumns() {
		return Math.max(1, (cell * Drawing.GRID - 8) / PICKER_CELL);
	}

	/** Le glyphe du sélecteur sous la souris (son rang parmi les retenus), ou −1. */
	private int pickerEntryAt(double x, double y) {
		int size = cell * Drawing.GRID;
		if (x < canvasX + 4 || x >= canvasX + size - 4 || y < canvasY + PICKER_TOP || y >= canvasY + size) {
			return -1;
		}
		int columns = pickerColumns();
		int column = Math.min((int) ((x - canvasX - 4) / ((size - 8) / columns)), columns - 1);
		int row = (int) ((y - canvasY - PICKER_TOP + pickerScroll) / PICKER_ROW);
		int index = row * columns + column;
		return index < pickerGlyphs().size() ? index : -1;
	}

	/** L'onglet de couche sous la souris (rang dans {@link #PICKER_LAYERS}), ou −1. */
	private int pickerTabAt(double x, double y) {
		int size = cell * Drawing.GRID;
		if (x < canvasX + 4 || x >= canvasX + size - 4 || y < canvasY + PICKER_TABS || y >= canvasY + PICKER_TABS + 12) {
			return -1;
		}
		return Math.min((int) ((x - canvasX - 4) / ((size - 8) / PICKER_LAYERS.length)), PICKER_LAYERS.length - 1);
	}

	private int maxPickerScroll() {
		int rows = (pickerGlyphs().size() + pickerColumns() - 1) / pickerColumns();
		return Math.max(0, rows * PICKER_ROW - (cell * Drawing.GRID - PICKER_TOP));
	}

	/** Les glyphes compris que retiennent la couche et la recherche, les derniers posés en tête. */
	private List<Glyph> pickerGlyphs() {
		return GlyphPicker.filter(stampable(), pickerLayer, searchBox.getValue(), this::glyphName, recentStamps);
	}

	private String glyphName(Glyph g) {
		return Component.translatable(g.nameKey()).getString();
	}

	/** Le panneau du sélecteur : le glyphe pointé (ou celui du tampon), ce qu'il vise, ce qu'il coûte. */
	private List<Line> pickerLines() {
		List<Line> out = new ArrayList<>();
		Glyph g = pickerHovered != null ? pickerHovered : stampGlyph();
		if (g != null) {
			out.add(new Line(Component.translatable(g.nameKey()), EFFECT));
			out.add(new Line(Component.translatable("treatise.fmab.layer." + g.layer().name().toLowerCase(Locale.ROOT)), DIM));
			out.add(new Line(Component.translatable(g.descriptionKey()), TEXT));
			out.add(new Line(Component.translatable("notebook.fmab.picker.cost", g.complexity(), g.concentration()), DIM));
		}
		out.add(new Line(Component.translatable("notebook.fmab.picker.hint"), DIM));
		return out;
	}

	private void drawPanel(GuiGraphicsExtractor graphics) {
		int px = canvasX + cell * Drawing.GRID + 10;
		List<Line> lines = pickerOpen ? pickerLines() : analysisLines();
		if (!status.getString().isEmpty()) {
			lines.add(new Line(status, STATUS));
		}
		List<FormattedCharSequence> wrapped = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();
		for (Line line : lines) {
			for (FormattedCharSequence seq : font.split(line.text(), PANEL_WIDTH - 8)) {
				wrapped.add(seq);
				colors.add(line.color());
			}
		}
		int bottom = Math.min(height - 4, panelTop + 6 + wrapped.size() * 10);
		graphics.fill(px, panelTop, px + PANEL_WIDTH, bottom, PANEL_BG);
		int y = panelTop + 4;
		for (int i = 0; i < wrapped.size() && y + 9 <= bottom; i++) {
			graphics.text(font, wrapped.get(i), px + 4, y, colors.get(i));
			y += 10;
		}
	}

	private List<Line> analysisLines() {
		List<Line> out = new ArrayList<>();
		Analysis a = analysis();
		if (a == null) {
			out.add(new Line(Component.translatable("notebook.fmab.empty"), TEXT));
			return out;
		}
		AlchemistData me = alchemist();
		// Le verdict d'abord : c'est ce qu'on cherche en ouvrant le carnet.
		int verdictColor = switch (a.outcome()) {
			case WORKS -> GOOD;
			case REBOUND -> BAD;
			default -> DIM;
		};
		out.add(new Line(Component.translatable("notebook.fmab.outcome." + a.outcome().name().toLowerCase(Locale.ROOT),
				Math.round(a.reboundSeverity() * 100)), verdictColor));
		for (Analysis.StageEffect e : a.effects()) {
			out.add(new Line(Component.translatable("notebook.fmab.effect",
					Component.translatable("effect." + e.combination().effect().replace(':', '.')),
					String.format(Locale.ROOT, "%.1f", e.range())), EFFECT));
		}
		if (a.risk() > 0) {
			out.add(new Line(Component.translatable("notebook.fmab.risk", Math.round(a.risk() * 100)), WARN));
		}
		addIssues(out, a);
		if (!a.parsed().stages().isEmpty()) {
			Stage first = a.parsed().stages().getFirst();
			out.add(new Line(Component.translatable("notebook.fmab.stages", a.parsed().stages().size(),
					first.sides() == 0 ? Component.translatable("notebook.fmab.no_polygon")
							: Component.translatable("notebook.fmab.polygon", first.sides())), TEXT));
			List<Component> names = new ArrayList<>();
			for (Stage s : a.parsed().stages()) {
				s.glyphs().forEach(g -> names.add(Component.translatable(g.glyph().nameKey())));
				if (s.index() > 0) {
					out.add(new Line(Component.translatable("notebook.fmab.link", s.index() + 1,
							Component.translatable("notebook.fmab.link." + s.link().name().toLowerCase(Locale.ROOT))),
							TEXT));
				}
				if (!s.satellites().isEmpty()) {
					out.add(new Line(Component.translatable("notebook.fmab.satellites", s.index() + 1,
							s.satellites().size()), TEXT));
				}
			}
			out.add(new Line(names.isEmpty() ? Component.translatable("notebook.fmab.no_glyphs")
					: Component.translatable("notebook.fmab.glyphs", join(names)), TEXT));
		}
		int cap = me.rank().complexityCap();
		out.add(new Line(Component.translatable("notebook.fmab.complexity", a.complexity(), cap,
				Component.translatable(a.requiredRank().translationKey())), a.complexity() > cap ? BAD : DIM));
		out.add(new Line(Component.translatable("notebook.fmab.stability", Math.round(a.stability() * 100)),
				a.stability() < 1 ? WARN : DIM));
		float concentration = minecraft.player == null ? me.maxConcentration() : Concentration.of(minecraft.player);
		out.add(new Line(Component.translatable("notebook.fmab.concentration", a.concentration(),
				(int) concentration), a.concentration() > concentration ? BAD : DIM));
		return out;
	}

	/**
	 * Les problèmes, regroupés quand ils se répètent. Pour un trait non reconnu, on dit de quel
	 * glyphe il se rapproche et de combien il s'en écarte.
	 */
	private void addIssues(List<Line> out, Analysis a) {
		Map<CircleIssue.Kind, Integer> counts = new LinkedHashMap<>();
		Map<CircleIssue.Kind, CircleIssue> firsts = new LinkedHashMap<>();
		List<String> hints = new ArrayList<>();
		for (CircleIssue issue : a.issues()) {
			counts.merge(issue.kind(), 1, Integer::sum);
			firsts.putIfAbsent(issue.kind(), issue);
			if (issue.kind() == CircleIssue.Kind.UNKNOWN_GLYPH) {
				hints.add(issue.detail());
			}
		}
		for (Map.Entry<CircleIssue.Kind, Integer> e : counts.entrySet()) {
			Component text = IssueText.of(firsts.get(e.getKey()));
			if (e.getValue() > 1) {
				text = Component.translatable("notebook.fmab.issue_count", text, e.getValue());
			}
			out.add(new Line(Component.literal("• ").append(text), WARN));
			if (e.getKey() == CircleIssue.Kind.UNKNOWN_GLYPH) {
				hints.forEach(h -> out.add(new Line(closestHint(h), DIM)));
			}
		}
	}

	/** « Le plus proche : Terre (écart 0.140, marge 0.100) », à partir du détail {@code id|écart}. */
	private Component closestHint(String detail) {
		int bar = detail.indexOf('|');
		if (bar < 0 || minecraft == null || minecraft.level == null) {
			return Component.translatable("notebook.fmab.no_close_glyph");
		}
		String id = detail.substring(0, bar);
		String score = detail.substring(bar + 1);
		return AlchemyRules.of(minecraft.level.registryAccess()).glyphs().stream()
				.filter(g -> g.id().equals(id))
				.findFirst()
				.<Component>map(g -> Component.translatable("notebook.fmab.closest", Component.translatable(g.nameKey()),
						score, String.format(Locale.ROOT, "%.3f", g.tolerance().position())))
				.orElseGet(() -> Component.translatable("notebook.fmab.no_close_glyph"));
	}

	private static Component join(List<Component> parts) {
		var out = Component.empty();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0) {
				out.append(", ");
			}
			out.append(parts.get(i));
		}
		return out;
	}

	/** Trace une primitive point par point : sert à l'aperçu du geste en cours. */
	private void plot(GuiGraphicsExtractor graphics, Primitive p, int color) {
		for (Vec2 v : p.sample(0.15)) {
			int x = screenX(v.x()), y = screenY(v.y());
			graphics.fill(x - 1, y - 1, x + 1, y + 1, color);
		}
	}

	private void mark(GuiGraphicsExtractor graphics, Vec2 at, int color) {
		int x = screenX(at.x()), y = screenY(at.y());
		graphics.outline(x - 3, y - 3, 7, 7, color);
	}

	// ---- Saisie --------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (contentsOpen) {
			int index = contentsEntryAt(event.x(), event.y());
			if (index >= 0 && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
				openPage(index);
			}
			return insideCanvas(event.x(), event.y());
		}
		if (pickerOpen) {
			if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
				int tab = pickerTabAt(event.x(), event.y());
				int index = pickerEntryAt(event.x(), event.y());
				if (tab >= 0) {
					pickerLayer = PICKER_LAYERS[tab];
					pickerScroll = 0;
					setFocused(searchBox);
				} else if (index >= 0) {
					pick(pickerGlyphs().get(index));
				}
			}
			return insideCanvas(event.x(), event.y());
		}
		if (!insideCanvas(event.x(), event.y())) {
			return false;
		}
		Vec2 p = toGrid(event.x(), event.y());
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			resetGesture();
			erase(p);
			return true;
		}
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return false;
		}
		setFocused(null);
		switch (tool) {
			case DOT -> commit(new Primitive.Dot(p));
			case STAMP -> {
				Glyph g = stampGlyph();
				if (g != null) {
					recentStamps = GlyphPicker.remember(recentStamps, g.id());
				}
				commitAll(stampAt(p));
			}
			case ARC -> {
				if (arcStart != null) {
					commit(arc(anchor, arcStart, p));
				} else {
					anchor = p;
				}
			}
			default -> anchor = p;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		cursor = toGrid(event.x(), event.y());
		return super.mouseDragged(event, dx, dy) || anchor != null;
	}

	@Override
	public void mouseMoved(double x, double y) {
		cursor = toGrid(x, y);
		super.mouseMoved(x, y);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (anchor == null || event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return super.mouseReleased(event);
		}
		Vec2 p = toGrid(event.x(), event.y());
		switch (tool) {
			case LINE -> {
				if (!p.equals(anchor)) {
					commit(new Primitive.Line(anchor, p));
				} else {
					resetGesture();
				}
			}
			case CIRCLE -> {
				double r = snap(anchor.distance(p));
				if (r >= 0.5) {
					commit(new Primitive.Circle(anchor, r));
				} else {
					resetGesture();
				}
			}
			case POLYGON -> {
				if (anchor.distance(p) >= 0.5) {
					commit(polygon(anchor, p));
				} else {
					resetGesture();
				}
			}
			case ARC -> {
				if (arcStart == null) {
					if (anchor.distance(p) >= 0.5) {
						arcStart = p;
					} else {
						resetGesture();
					}
				}
			}
			default -> {
			}
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (contentsOpen && insideCanvas(x, y) && scrollY != 0) {
			contentsScroll = Math.clamp(contentsScroll - (int) Math.round(scrollY * CONTENTS_ROW), 0, maxContentsScroll());
			return true;
		}
		if (pickerOpen && insideCanvas(x, y) && scrollY != 0) {
			pickerScroll = Math.clamp(pickerScroll - (int) Math.round(scrollY * PICKER_ROW / 2.0), 0, maxPickerScroll());
			return true;
		}
		if (tool == Tool.STAMP && insideCanvas(x, y) && scrollY != 0) {
			stampRotation = Math.floorMod(stampRotation + (scrollY > 0 ? 15 : -15), 360);
			refreshLabels();
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		swallowNextChar = false;
		if (pickerOpen) {
			if (event.key() == InputConstants.KEY_ESCAPE) {
				togglePicker();
				return true;
			}
			if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
				List<Glyph> glyphs = pickerGlyphs();
				if (!glyphs.isEmpty()) {
					pick(glyphs.getFirst());
				}
				return true;
			}
			return super.keyPressed(event);
		}
		boolean typing = nameBox.isFocused();
		if (event.key() == InputConstants.KEY_Z && event.hasControlDown() && !typing) {
			undo();
			return true;
		}
		if (event.key() == InputConstants.KEY_G && !typing && !event.hasControlDown()) {
			togglePicker();
			swallowNextChar = true;
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (swallowNextChar) {
			swallowNextChar = false;
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	public void onClose() {
		save(false);
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---- Outils ---------------------------------------------------------------------------------

	private Primitive preview() {
		if (anchor == null) {
			return null;
		}
		return switch (tool) {
			case LINE -> new Primitive.Line(anchor, cursor);
			case CIRCLE -> new Primitive.Circle(anchor, Math.max(0.5, snap(anchor.distance(cursor))));
			case POLYGON -> anchor.distance(cursor) >= 0.5 ? polygon(anchor, cursor) : null;
			case ARC -> arcStart == null
					? new Primitive.Line(anchor, cursor)
					: arc(anchor, arcStart, cursor);
			case DOT, STAMP -> null;
		};
	}

	/** Les glyphes que le joueur comprend : eux seuls se posent au tampon. */
	private List<Glyph> stampable() {
		if (minecraft == null || minecraft.level == null) {
			return List.of();
		}
		AlchemistData me = alchemist();
		return AlchemyRules.of(minecraft.level.registryAccess()).glyphs().stream()
				.filter(g -> me.known().contains(g.id()))
				.toList();
	}

	private Glyph stampGlyph() {
		List<Glyph> glyphs = stampable();
		return glyphs.stream().filter(g -> g.id().equals(stampId)).findFirst()
				.orElse(glyphs.isEmpty() ? null : glyphs.getFirst());
	}

	private void cycleStamp(int delta) {
		List<Glyph> glyphs = stampable();
		if (!glyphs.isEmpty()) {
			int index = Math.max(0, glyphs.indexOf(stampGlyph()));
			stampId = glyphs.get(Math.floorMod(index + delta, glyphs.size())).id();
		}
		selectTool(Tool.STAMP);
	}

	/** Le glyphe choisi au sélecteur passe au tampon, et la grille revient. */
	private void pick(Glyph g) {
		stampId = g.id();
		recentStamps = GlyphPicker.remember(recentStamps, g.id());
		closePicker();
		selectTool(Tool.STAMP);
	}

	/** Ouvre ou referme le sélecteur de glyphes, à la place de la grille ; la recherche prend la main. */
	public void togglePicker() {
		if (pickerOpen) {
			closePicker();
			return;
		}
		if (stampable().isEmpty()) {
			return;
		}
		resetGesture();
		contentsOpen = false;
		pickerOpen = true;
		pickerScroll = 0;
		searchBox.visible = true;
		searchBox.setValue("");
		setFocused(searchBox);
	}

	private void closePicker() {
		pickerOpen = false;
		pickerHovered = null;
		searchBox.visible = false;
		if (getFocused() == searchBox) {
			setFocused(null);
		}
	}

	public boolean pickerOpen() {
		return pickerOpen;
	}

	/** Tape une recherche dans le sélecteur ; rend les identifiants retenus, dans l'ordre affiché. */
	public List<String> searchGlyphs(String query) {
		searchBox.setValue(query);
		return pickerGlyphs().stream().map(Glyph::id).toList();
	}

	/** Choisit le premier glyphe retenu, comme la touche Entrée. */
	public boolean pickFirst() {
		List<Glyph> glyphs = pickerGlyphs();
		if (glyphs.isEmpty()) {
			return false;
		}
		pick(glyphs.getFirst());
		return true;
	}

	/** L'identifiant du glyphe du tampon, si l'outil tampon est en main ; sinon null. */
	public String stampInHand() {
		Glyph g = stampGlyph();
		return tool == Tool.STAMP && g != null ? g.id() : null;
	}

	/** Le glyphe du tampon posé en {@code at}, ou rien si le joueur n'en comprend aucun. */
	private List<Primitive> stampAt(Vec2 at) {
		Glyph g = stampGlyph();
		return g == null ? List.of() : SimpleCircles.place(g, at, stampHalfSize, stampRotation);
	}

	/** Polygone régulier de centre {@code c} dont un sommet est en {@code vertex}. */
	private Primitive polygon(Vec2 c, Vec2 vertex) {
		double r = c.distance(vertex);
		double start = vertex.sub(c).angle();
		List<Vec2> points = new ArrayList<>();
		for (int i = 0; i < sides; i++) {
			Vec2 v = c.add(Vec2.polar(r, start + 2 * Math.PI * i / sides));
			points.add(new Vec2(snap(v.x()), snap(v.y())));
		}
		return new Primitive.Polygon(points);
	}

	/** Arc de centre {@code c}, partant de {@code start}, tournant dans le sens horaire jusqu'à {@code end}. */
	private static Primitive arc(Vec2 c, Vec2 start, Vec2 end) {
		double a0 = Math.toDegrees(start.sub(c).angle());
		double a1 = Math.toDegrees(end.sub(c).angle());
		double sweep = ((a1 - a0) % 360 + 360) % 360;
		if (sweep < 1) {
			sweep = 360;
		}
		return new Primitive.Arc(c, snap(c.distance(start)), Math.round(a0), Math.round(sweep));
	}

	/** Image d'une primitive par la symétrie d'axe vertical du carnet. */
	private static Primitive mirror(Primitive p) {
		if (p instanceof Primitive.Arc a) {
			Vec2 c = new Vec2(Drawing.GRID - a.center().x(), a.center().y());
			return new Primitive.Arc(c, a.radius(), 180 - (a.startDeg() + a.sweepDeg()), a.sweepDeg());
		}
		return p.map(v -> new Vec2(Drawing.GRID - v.x(), v.y()));
	}

	private void commit(Primitive p) {
		commitAll(List.of(p));
	}

	/** Ajoute les traits d'un geste (et leur reflet en symétrie) ; Annuler les retirera ensemble. */
	private void commitAll(List<Primitive> ps) {
		int before = strokes.size();
		List<Primitive> all = new ArrayList<>(ps);
		if (symmetry) {
			for (Primitive p : ps) {
				Primitive m = mirror(p);
				if (!m.equals(p)) {
					all.add(m);
				}
			}
		}
		for (Primitive p : all) {
			if (strokes.size() < Drawing.MAX_PRIMITIVES) {
				strokes.add(p);
			}
		}
		if (strokes.size() > before) {
			gestures.push(strokes.size() - before);
		}
		resetGesture();
	}

	private void erase(Vec2 at) {
		int best = -1;
		double bestDistance = 1.0;
		for (int i = 0; i < strokes.size(); i++) {
			for (Vec2 v : strokes.get(i).sample(0.25)) {
				double d = v.distance(at);
				if (d < bestDistance) {
					bestDistance = d;
					best = i;
				}
			}
		}
		if (best >= 0) {
			strokes.remove(best);
			gestures.clear();
		}
	}

	private void undo() {
		resetGesture();
		int count = gestures.isEmpty() ? 1 : gestures.pop();
		for (int i = 0; i < count && !strokes.isEmpty(); i++) {
			strokes.removeLast();
		}
	}

	private void resetGesture() {
		anchor = null;
		arcStart = null;
	}

	/** Le tracé a changé d'un bloc (page, import, effacement) : l'historique des gestes ne vaut plus. */
	private void forgetGestures() {
		gestures.clear();
	}

	private void selectTool(Tool t) {
		tool = t;
		resetGesture();
		refreshLabels();
	}

	private void refreshLabels() {
		for (int i = 0; i < toolButtons.size(); i++) {
			toolButtons.get(i).active = Tool.values()[i] != tool;
		}
		Glyph g = stampGlyph();
		stampButton.active = g != null;
		stampButton.setMessage(g == null ? Component.translatable("notebook.fmab.stamp.none")
				: Component.translatable("notebook.fmab.stamp", Component.translatable(g.nameKey())));
		stampSizeButton.setMessage(Component.translatable("notebook.fmab.stamp.size", stampHalfSize * 2));
		sidesButton.setMessage(Component.translatable("notebook.fmab.sides", sides));
		symmetryButton.setMessage(Component.translatable(symmetry ? "notebook.fmab.symmetry.on" : "notebook.fmab.symmetry.off"));
	}

	// ---- Pages ------------------------------------------------------------------------------------

	private String currentName() {
		return page < contents.pages().size() ? displayName(contents.pages().get(page).name()) : defaultName(page);
	}

	/** Un nom de page « @clé » (cercles du carnet de départ et du Traité) s'affiche traduit. */
	public static String displayName(String name) {
		return name.startsWith("@") ? Component.translatable(name.substring(1)).getString() : name;
	}

	private Component pageLabel() {
		int count = Math.max(contents.pages().size(), page + 1);
		return Component.translatable("notebook.fmab.page", page + 1, count);
	}

	private static String defaultName(int index) {
		return Component.translatable("notebook.fmab.default_name", index + 1).getString();
	}

	/** Range le tracé courant dans sa page (sans l'envoyer au serveur). */
	private void store() {
		if (strokes.isEmpty() && page >= contents.pages().size()) {
			return;
		}
		String name = nameBox.getValue().isBlank() ? defaultName(page) : nameBox.getValue();
		contents = contents.withPage(page, new NotebookContents.Page(name, new Drawing(strokes)));
	}

	private void save(boolean announce) {
		store();
		contents = contents.select(Math.min(page, Math.max(0, contents.pages().size() - 1)));
		ClientPlatform.get().send(new SaveNotebookPayload(contents));
		if (announce) {
			status = Component.translatable("notebook.fmab.saved");
		}
	}

	private void newPage() {
		if (contents.pages().size() >= NotebookContents.MAX_PAGES) {
			status = Component.translatable("notebook.fmab.full");
			return;
		}
		store();
		contentsOpen = false;
		page = contents.pages().size();
		strokes = new ArrayList<>();
		nameBox.setValue(defaultName(page));
		resetGesture();
		forgetGestures();
	}

	private void turn(int delta) {
		store();
		int count = contents.pages().size();
		if (count == 0) {
			return;
		}
		openPage(Math.floorMod(page + delta, count));
	}

	/** Ouvre une page du carnet (le sommaire se referme) ; le tracé en cours est rangé dans la sienne. */
	public void openPage(int index) {
		if (index < 0 || index >= contents.pages().size()) {
			return;
		}
		store();
		contentsOpen = false;
		page = index;
		strokes = new ArrayList<>(contents.pages().get(page).drawing().primitives());
		nameBox.setValue(currentName());
		resetGesture();
		forgetGestures();
	}

	/** Ouvre ou referme le sommaire, à la place de la grille. */
	public void toggleContents() {
		store();
		resetGesture();
		closePicker();
		contentsOpen = !contentsOpen;
		if (contentsOpen) {
			// La page ouverte reste en vue.
			contentsScroll = (page / CONTENTS_COLUMNS) * CONTENTS_ROW - (cell * Drawing.GRID - CONTENTS_TOP) / 2;
		}
	}

	public boolean contentsOpen() {
		return contentsOpen;
	}

	/** La page ouverte, et le nombre de pages du carnet tel que l'écran le tient. */
	public int openedPage() {
		return page;
	}

	public int pageCount() {
		return contents.pages().size();
	}

	/** Avance ou recule la page d'un rang ; elle reste ouverte. */
	private void move(int delta) {
		store();
		if (page >= contents.pages().size()) {
			return;
		}
		contents = contents.moved(page, delta);
		page = contents.selected();
	}

	private void deletePage() {
		if (page < contents.pages().size()) {
			contents = contents.withoutPage(page);
		}
		contentsOpen = false;
		page = Math.max(0, Math.min(page, contents.pages().size() - 1));
		strokes = new ArrayList<>(contents.pages().isEmpty() ? List.of() : contents.pages().get(page).drawing().primitives());
		forgetGestures();
		nameBox.setValue(currentName());
		resetGesture();
	}

	private void exportCode() {
		Minecraft.getInstance().keyboardHandler.setClipboard(DrawingCode.encode(new Drawing(strokes)));
		status = Component.translatable("notebook.fmab.exported");
	}

	private void importCode() {
		try {
			Drawing d = DrawingCode.decode(Minecraft.getInstance().keyboardHandler.getClipboard());
			strokes = new ArrayList<>(d.primitives());
			forgetGestures();
			resetGesture();
			status = Component.translatable("notebook.fmab.imported");
		} catch (RuntimeException e) {
			status = Component.translatable("notebook.fmab.import_failed");
		}
	}

	// ---- Analyse ---------------------------------------------------------------------------------

	private Analysis analysis() {
		Drawing d = new Drawing(strokes);
		if (d.isEmpty() || minecraft == null || minecraft.level == null) {
			return null;
		}
		if (!d.equals(analyzed)) {
			AlchemistData me = alchemist();
			cachedAnalysis = AlchemyRules.of(minecraft.level.registryAccess()).analyze(d, me);
			analyzed = d;
		}
		return cachedAnalysis;
	}

	private AlchemistData alchemist() {
		Player player = minecraft == null ? null : minecraft.player;
		AlchemistData data = player == null ? null : FmabAttachments.ALCHEMIST.get(player);
		return data == null ? AlchemistData.NEW : data;
	}

	// ---- Coordonnées -------------------------------------------------------------------------------

	private boolean insideCanvas(double x, double y) {
		int size = cell * Drawing.GRID;
		return x >= canvasX - cell / 2.0 && y >= canvasY - cell / 2.0
				&& x <= canvasX + size + cell / 2.0 && y <= canvasY + size + cell / 2.0;
	}

	/** Position de grille, accrochée à la demi-case. */
	private Vec2 toGrid(double x, double y) {
		double gx = Math.clamp((x - canvasX) / cell, 0, Drawing.GRID);
		double gy = Math.clamp((y - canvasY) / cell, 0, Drawing.GRID);
		return new Vec2(snap(gx), snap(gy));
	}

	private static double snap(double v) {
		return Math.round(v * 2) / 2.0;
	}

	private int screenX(double gx) {
		return canvasX + (int) Math.round(gx * cell);
	}

	private int screenY(double gy) {
		return canvasY + (int) Math.round(gy * cell);
	}
}
