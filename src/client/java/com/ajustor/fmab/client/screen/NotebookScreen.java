package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.PlacedGlyph;
import com.ajustor.fmab.alchemy.circle.Stage;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.DrawingCode;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.client.render.CircleTextures;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.network.SaveNotebookPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.transmutation.AlchemyRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Carnet de cercles : on y trace ses cercles sur une grille de 32 cases, on les nomme, on les
 * enregistre, et le carnet dit ce qu'il lit avant qu'on aille les tracer à la craie.
 */
public class NotebookScreen extends Screen {
	private enum Tool {
		LINE, CIRCLE, POLYGON, ARC, DOT;

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
	private static final int PANEL_WIDTH = 190;

	private final InteractionHand hand;
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

	public NotebookScreen(InteractionHand hand, ItemStack stack) {
		super(Component.translatable("item.fmab.circle_notebook"));
		this.hand = hand;
		this.contents = stack.getOrDefault(FmabComponents.NOTEBOOK, NotebookContents.EMPTY);
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
		addRenderableWidget(Button.builder(Component.translatable("notebook.fmab.undo"), b -> undo())
				.bounds(px, y, 62, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("notebook.fmab.clear"), b -> {
			strokes.clear();
			resetGesture();
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
		addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(px + 106, y, 20, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("notebook.fmab.delete_page"), b -> deletePage())
				.bounds(px + 128, y, 62, 18).build());
		y += 20;
		addRenderableWidget(Button.builder(Component.translatable("notebook.fmab.export"), b -> exportCode())
				.bounds(px, y, 94, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("notebook.fmab.import"), b -> importCode())
				.bounds(px + 96, y, 94, 18).build());
		refreshLabels();
	}

	// ---- Rendu ---------------------------------------------------------------------------------

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		int size = cell * Drawing.GRID;
		graphics.fill(canvasX - 2, canvasY - 2, canvasX + size + 2, canvasY + size + 2, 0xFF8A7A5A);
		graphics.fill(canvasX, canvasY, canvasX + size, canvasY + size, PAPER);
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

	private void drawPanel(GuiGraphicsExtractor graphics) {
		int px = canvasX + cell * Drawing.GRID + 10;
		int y = canvasY + 6 * 20 + 2;
		graphics.centeredText(font, pageLabel(), px + 63, y - 15, TEXT);
		List<Component> lines = analysisLines();
		lines.add(status);
		for (Component line : lines) {
			for (FormattedCharSequence seq : font.split(line, PANEL_WIDTH)) {
				graphics.text(font, seq, px, y, TEXT);
				y += 10;
			}
		}
	}

	private List<Component> analysisLines() {
		List<Component> out = new ArrayList<>();
		Analysis a = analysis();
		if (a == null) {
			out.add(Component.translatable("notebook.fmab.empty"));
			return out;
		}
		AlchemistData me = alchemist();
		if (!a.parsed().stages().isEmpty()) {
			Stage first = a.parsed().stages().getFirst();
			out.add(Component.translatable("notebook.fmab.stages", a.parsed().stages().size(),
					first.sides() == 0 ? Component.translatable("notebook.fmab.no_polygon")
							: Component.translatable("notebook.fmab.polygon", first.sides())));
			List<Component> names = new ArrayList<>();
			for (Stage s : a.parsed().stages()) {
				s.glyphs().forEach(g -> names.add(Component.translatable(g.glyph().nameKey())));
				if (s.index() > 0) {
					out.add(Component.translatable("notebook.fmab.link", s.index() + 1,
							Component.translatable("notebook.fmab.link." + s.link().name().toLowerCase(Locale.ROOT))));
				}
				if (!s.satellites().isEmpty()) {
					out.add(Component.translatable("notebook.fmab.satellites", s.index() + 1, s.satellites().size()));
				}
			}
			if (!names.isEmpty()) {
				out.add(Component.translatable("notebook.fmab.glyphs", join(names)));
			}
		}
		for (Analysis.StageEffect e : a.effects()) {
			out.add(Component.translatable("notebook.fmab.effect",
					Component.translatable("effect." + e.combination().effect().replace(':', '.')),
					String.format(Locale.ROOT, "%.1f", e.range())));
		}
		out.add(Component.translatable("notebook.fmab.complexity", a.complexity(), me.rank().complexityCap(),
				Component.translatable(a.requiredRank().translationKey())));
		out.add(Component.translatable("notebook.fmab.stability", Math.round(a.stability() * 100)));
		out.add(Component.translatable("notebook.fmab.concentration", a.concentration(), (int) me.concentration()));
		out.add(Component.translatable("notebook.fmab.outcome." + a.outcome().name().toLowerCase(Locale.ROOT),
				Math.round(a.reboundSeverity() * 100)));
		for (CircleIssue issue : a.issues()) {
			out.add(Component.literal("• ").append(Component.translatable(issue.kind().translationKey())));
		}
		return out;
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
		if (!insideCanvas(event.x(), event.y())) {
			return false;
		}
		Vec2 p = toGrid(event.x(), event.y());
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			resetGesture();
			erase(p);
			return true;
		}
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			return false;
		}
		setFocused(null);
		switch (tool) {
			case DOT -> commit(new Primitive.Dot(p));
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
		if (anchor == null || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
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
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_Z && event.hasControlDown() && !nameBox.isFocused()) {
			undo();
			return true;
		}
		return super.keyPressed(event);
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
			case DOT -> null;
		};
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
		if (strokes.size() < Drawing.MAX_PRIMITIVES) {
			strokes.add(p);
			Primitive m = mirror(p);
			if (symmetry && !m.equals(p) && strokes.size() < Drawing.MAX_PRIMITIVES) {
				strokes.add(m);
			}
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
		}
	}

	private void undo() {
		resetGesture();
		if (!strokes.isEmpty()) {
			strokes.removeLast();
		}
	}

	private void resetGesture() {
		anchor = null;
		arcStart = null;
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
		sidesButton.setMessage(Component.translatable("notebook.fmab.sides", sides));
		symmetryButton.setMessage(Component.translatable(symmetry ? "notebook.fmab.symmetry.on" : "notebook.fmab.symmetry.off"));
	}

	// ---- Pages ------------------------------------------------------------------------------------

	private String currentName() {
		return page < contents.pages().size() ? contents.pages().get(page).name() : defaultName(page);
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
		ClientPlayNetworking.send(new SaveNotebookPayload(hand, contents));
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
		page = contents.pages().size();
		strokes = new ArrayList<>();
		nameBox.setValue(defaultName(page));
		resetGesture();
	}

	private void turn(int delta) {
		store();
		int count = contents.pages().size();
		if (count == 0) {
			return;
		}
		page = Math.floorMod(page + delta, count);
		strokes = new ArrayList<>(contents.pages().get(page).drawing().primitives());
		nameBox.setValue(currentName());
		resetGesture();
	}

	private void deletePage() {
		if (page < contents.pages().size()) {
			contents = contents.withoutPage(page);
		}
		page = Math.max(0, Math.min(page, contents.pages().size() - 1));
		strokes = new ArrayList<>(contents.pages().isEmpty() ? List.of() : contents.pages().get(page).drawing().primitives());
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
		AlchemistData data = player == null ? null : player.getAttached(FmabAttachments.ALCHEMIST);
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
