package com.ajustor.fmab.client.screen;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.arts.IshvalArm;
import com.ajustor.fmab.client.render.CircleTextures;
import com.ajustor.fmab.client.render.TattooPreview;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.network.TattooArtPayload;
import com.ajustor.fmab.platform.client.ClientPlatform;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.tattoo.BodyArt;
import com.ajustor.fmab.tattoo.BodyCircles;
import com.ajustor.fmab.tattoo.Canvas;
import com.ajustor.fmab.tattoo.Sleeve;
import com.ajustor.fmab.tattoo.TattooSlot;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * L'éditeur des dessins sur le corps, ouvert d'un clic droit dans le vide : l'encre dessine sur la
 * peau, le burin grave l'acier des bras d'automail. À gauche, le joueur, qu'on fait tourner en le
 * tirant, porte déjà le dessin en cours ; au milieu, la grille de l'emplacement choisi ; à droite, les
 * outils du carnet, un trait à main levée, les couleurs, et le tampon : un cercle du carnet, posé où
 * l'on veut, qui fait de l'emplacement un cercle alchimique. Le tour d'un bras se dessine sur une page
 * dépliée ({@link Sleeve}), deux fois plus large que haute, où l'on pose plusieurs tampons ; l'aperçu se
 * tourne vers la face que survole la souris.
 *
 * <p>Chaque emplacement garde son brouillon tant que l'éditeur est ouvert ; « Encrer » (ou « Graver »)
 * envoie celui de l'emplacement montré au serveur, qui vérifie l'outil et le tampon et prélève son dû.
 */
public class TattooEditorScreen extends Screen {
	private enum Tool {
		FREEHAND, LINE, CIRCLE, POLYGON, ARC, DOT, STAMP;

		Component label() {
			return Component.translatable("tattoo.fmab.editor.tool." + name().toLowerCase(Locale.ROOT));
		}
	}

	/** Les couleurs à portée de clic ; le champ hexadécimal donne toutes les autres. */
	private static final int[] PALETTE = {
			BodyArt.SKIN_INK, 0x4A4A4A, 0xF4F1EA, 0x7A4A28,
			0xB0201A, 0xE0662A, 0xE8C23A, 0x3E8E3A,
			0x1E5E3A, 0x2AA6B8, 0x2C5CC8, 0x1E2A6B,
			0x7A3AB8, 0xC83A9E, 0xF0A0B8, BodyArt.STEEL_INK};
	private static final int PAPER = 0xFFEDE3C8;
	private static final int STEEL = 0xFF9AA0A8;
	private static final int FRAME = 0xFF6B4F2A;
	private static final int GRID_LINE = 0x22000000;
	private static final int AXIS_LINE = 0x44000000;
	/** Les arêtes du bras, sur la page du tour du bras. */
	private static final int EDGE_LINE = 0x88000000;
	private static final int FACE_LABEL = 0x99000000;
	private static final int TEXT = 0xFFE0E0E0;
	private static final int DIM = 0xFFA8A8A8;
	private static final int BAD = 0xFFFF7A6A;
	private static final int GOOD = 0xFF7FD67F;
	private static final int SELECTED = 0xFFFFFFFF;
	private static final int PREVIEW_W = 76;
	private static final int PANEL_W = 150;
	private static final int SWATCH = 14;
	/** Un point de plus au trait libre dès que la souris s'éloigne d'autant (en cases). */
	private static final double FREEHAND_STEP = 0.75;
	/** Points au plus d'un trait libre : au-delà, il continue en un nouveau trait. */
	private static final int FREEHAND_POINTS = 48;

	private final boolean engrave;
	private final List<TattooSlot> slots = new ArrayList<>();
	private final Map<TattooSlot, BodyArt> drafts = new EnumMap<>(TattooSlot.class);
	private final ArrayDeque<BodyArt> history = new ArrayDeque<>();
	private TattooSlot slot;
	private Tool tool = Tool.FREEHAND;
	private int sides = 5;
	private int color;
	private int stampPage;
	private double stampHalf = 10;
	/** Le tampon se déroule autour du bras plutôt que de se poser à plat (tour du bras seulement). */
	private boolean unroll;
	/** Le geste en cours (coordonnées de grille) : départ, ou points du trait libre. */
	private @Nullable Vec2 anchor;
	private @Nullable Vec2 arcStart;
	private final List<Vec2> freehand = new ArrayList<>();
	private Vec2 cursor = Drawing.CENTER_POINT;
	/** L'angle du joueur dans l'aperçu, en degrés : 0 de face. */
	private float yaw;
	private boolean turning;
	/** La face du bras que la souris survolait : l'aperçu ne se tourne que quand elle change. */
	private Sleeve.@Nullable Face hovered;
	/** Les cercles déjà analysés, pour ne pas les relire à chaque image. */
	private final Map<Drawing, Analysis> analyses = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Drawing, Analysis> eldest) {
			return size() > 16;
		}
	};

	private int cell;
	private int canvasX;
	private int canvasY;
	private int previewX;
	private int panelX;
	private final List<Button> slotButtons = new ArrayList<>();
	private final List<Button> toolButtons = new ArrayList<>();
	private Button sidesButton;
	private Button stampButton;
	private Button stampSizeButton;
	private Button layoutButton;
	private Button applyButton;
	private EditBox hexBox;
	private int swatchY;
	/** Haut des lignes d'information (étages du tampon, coût). */
	private int infoY;
	private static final int INFO_LINES = 4;

	public TattooEditorScreen(boolean engrave) {
		super(Component.translatable(engrave ? "tattoo.fmab.editor.engrave_title" : "tattoo.fmab.editor.title"));
		this.engrave = engrave;
		this.color = engrave ? BodyArt.STEEL_INK : BodyArt.SKIN_INK;
		Player player = Minecraft.getInstance().player;
		BodyCircles.Surface wanted = engrave ? BodyCircles.Surface.STEEL : BodyCircles.Surface.SKIN;
		for (TattooSlot s : TattooSlot.values()) {
			if (player != null && BodyCircles.surface(player, s) == wanted) {
				slots.add(s);
				drafts.put(s, BodyCircles.art(player, s).orElse(BodyArt.EMPTY));
			}
		}
		slot = slots.isEmpty() ? TattooSlot.PALM_RIGHT : slots.getFirst();
		yaw = facing(slot);
	}

	// ---- Mise en page ----------------------------------------------------------------------------

	@Override
	protected void init() {
		int top = 26;
		Canvas page = page();
		// Le tour du bras est large : il accepte des cases plus petites pour tenir à l'écran.
		cell = Math.max(page.width() > page.height() ? 2 : 3, Math.min(7, Math.min((height - top - 22) / page.height(),
				(width - PREVIEW_W - PANEL_W - 24) / page.width())));
		int total = PREVIEW_W + 8 + canvasW() + 8 + PANEL_W;
		previewX = Math.max(4, (width - total) / 2);
		canvasX = previewX + PREVIEW_W + 8;
		canvasY = top;
		panelX = canvasX + canvasW() + 8;

		slotButtons.clear();
		int tabW = Math.min(70, (width - 8) / Math.max(1, slots.size()) - 2);
		int tx = (width - slots.size() * (tabW + 2)) / 2;
		for (TattooSlot s : slots) {
			slotButtons.add(addRenderableWidget(Button.builder(Component.translatable(s.translationKey()),
					b -> selectSlot(s)).bounds(tx, 4, tabW, 18).build()));
			tx += tabW + 2;
		}

		int y = top;
		toolButtons.clear();
		Tool[] drawing = {Tool.FREEHAND, Tool.LINE, Tool.CIRCLE, Tool.POLYGON, Tool.ARC, Tool.DOT};
		for (int i = 0; i < drawing.length; i++) {
			Tool t = drawing[i];
			toolButtons.add(addRenderableWidget(Button.builder(t.label(), b -> selectTool(t))
					.bounds(panelX + (i % 3) * 50, y + (i / 3) * 20, 49, 18).build()));
		}
		y += 40;
		sidesButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
			sides = sides >= 12 ? 3 : sides + 1;
			refreshLabels();
		}).bounds(panelX, y, 74, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("tattoo.fmab.editor.undo"), b -> undo())
				.bounds(panelX + 76, y, 74, 18).build());
		y += 22;
		// Le tampon : < page du carnet >, sa taille, et le retirer.
		addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleStamp(-1)).bounds(panelX, y, 16, 18).build());
		stampButton = addRenderableWidget(Button.builder(Component.empty(), b -> selectTool(Tool.STAMP))
				.tooltip(Tooltip.create(Component.translatable("tattoo.fmab.editor.stamp.tooltip")))
				.bounds(panelX + 18, y, 114, 18).build());
		addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleStamp(1)).bounds(panelX + 134, y, 16, 18).build());
		y += 20;
		stampSizeButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
			stampHalf = stampHalf >= page().maxStampHalf() ? BodyArt.MIN_STAMP_HALF : stampHalf + 2;
			refreshLabels();
		}).bounds(panelX, y, 74, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("tattoo.fmab.editor.unstamp"),
				b -> change(draft().withStamp(null))).bounds(panelX + 76, y, 74, 18).build());
		y += 20;
		layoutButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
			unroll = !unroll;
			tool = Tool.STAMP;
			refreshLabels();
		}).tooltip(Tooltip.create(Component.translatable("tattoo.fmab.editor.layout.tooltip")))
				.bounds(panelX, y, 150, 18).build());
		y += 24;
		swatchY = y;
		y += 2 * (SWATCH + 2) + 2;
		hexBox = addRenderableWidget(new EditBox(font, panelX, y, 74, 16, Component.translatable("tattoo.fmab.editor.hex")));
		hexBox.setMaxLength(7);
		hexBox.setValue(hex(color));
		hexBox.setResponder(this::typedColor);
		addRenderableWidget(Button.builder(Component.translatable("tattoo.fmab.editor.clear"), b -> change(BodyArt.EMPTY))
				.bounds(panelX + 76, y - 1, 74, 18).build());

		// Sous le champ de couleur : deux lignes sur le tampon et le coût, puis les boutons. La colonne
		// s'empile de haut en bas, quelle que soit la hauteur de la grille.
		y += 20;
		infoY = y;
		y += INFO_LINES * 10 + 4;
		addRenderableWidget(Button.builder(Component.translatable("tattoo.fmab.editor.close"), b -> onClose())
				.bounds(panelX, y, 60, 18).build());
		applyButton = addRenderableWidget(Button.builder(Component.translatable(engrave
						? "tattoo.fmab.editor.engrave" : "tattoo.fmab.editor.ink"), b -> apply())
				.tooltip(Tooltip.create(Component.translatable(engrave
						? "tattoo.fmab.editor.engrave.tooltip" : "tattoo.fmab.editor.ink.tooltip")))
				.bounds(panelX + 62, y, 88, 18).build());
		refreshLabels();
		showPreview();
	}

	private void refreshLabels() {
		for (int i = 0; i < slotButtons.size(); i++) {
			slotButtons.get(i).active = slots.get(i) != slot;
		}
		Tool[] drawing = {Tool.FREEHAND, Tool.LINE, Tool.CIRCLE, Tool.POLYGON, Tool.ARC, Tool.DOT};
		for (int i = 0; i < toolButtons.size(); i++) {
			toolButtons.get(i).active = drawing[i] != tool;
		}
		sidesButton.setMessage(Component.translatable("tattoo.fmab.editor.sides", sides));
		List<NotebookContents.Page> pages = pages();
		Component stamp = pages.isEmpty() ? Component.translatable("tattoo.fmab.editor.stamp.none")
				: Component.literal(NotebookScreen.displayName(pages.get(stampPage).name()));
		stampButton.setMessage(tool == Tool.STAMP ? Component.literal("» ").append(stamp) : stamp);
		stampButton.active = !pages.isEmpty();
		stampSizeButton.setMessage(Component.translatable("tattoo.fmab.editor.stamp.size", (int) (stampHalf * 2)));
		layoutButton.active = slot.sleeve();
		layoutButton.setMessage(Component.translatable(unrolled() ? "tattoo.fmab.editor.layout.unrolled"
				: "tattoo.fmab.editor.layout.whole"));
		if (applyButton != null) {
			applyButton.active = !slots.isEmpty() && !draft().equals(current(slot));
		}
	}

	// ---- Rendu -----------------------------------------------------------------------------------

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		refreshLabels();
		Canvas page = page();
		int cw = canvasW(), ch = canvasH();
		// L'aperçu, aussi haut que la page, et au moins aussi haut qu'une page carrée : on y voit le joueur.
		int previewH = Math.max(ch, cell * Drawing.GRID);
		graphics.centeredText(font, title, width / 2, canvasY + Math.max(ch, previewH) + 6, TEXT);
		// L'aperçu : le joueur, tourné à la main, porte le brouillon de l'emplacement.
		graphics.fill(previewX - 1, canvasY - 1, previewX + PREVIEW_W + 1, canvasY + previewH + 1, FRAME);
		graphics.fill(previewX, canvasY, previewX + PREVIEW_W, canvasY + previewH, 0xFF101014);
		if (minecraft != null && minecraft.player != null) {
			player(graphics, previewX, canvasY, previewX + PREVIEW_W, canvasY + previewH, minecraft.player);
		}
		graphics.text(font, Component.translatable("tattoo.fmab.editor.turn"), previewX + 3, canvasY + previewH - 10,
				DIM, false);

		graphics.fill(canvasX - 2, canvasY - 2, canvasX + cw + 2, canvasY + ch + 2, FRAME);
		graphics.fill(canvasX, canvasY, canvasX + cw, canvasY + ch, engrave ? STEEL : PAPER);
		if (slots.isEmpty()) {
			graphics.centeredText(font, Component.translatable("tattoo.fmab.nothing_to_engrave"),
					canvasX + cw / 2, canvasY + ch / 2, BAD);
			return;
		}
		for (int i = 0; i <= page.width(); i += 2) {
			int line = i * 2 == page.width() ? AXIS_LINE : GRID_LINE;
			graphics.fill(canvasX + i * cell, canvasY, canvasX + i * cell + 1, canvasY + ch, line);
		}
		for (int i = 0; i <= page.height(); i += 2) {
			int line = i * 2 == page.height() ? AXIS_LINE : GRID_LINE;
			graphics.fill(canvasX, canvasY + i * cell, canvasX + cw, canvasY + i * cell + 1, line);
		}
		if (slot.sleeve()) {
			faces(graphics);
		}
		BodyArt art = draft();
		if (!art.isEmpty()) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, CircleTextures.get(art, page), canvasX, canvasY, 0, 0, cw, ch,
					page.textureWidth(), page.textureHeight(), page.textureWidth(), page.textureHeight());
		}
		int ghost = 0xC0000000 | color;
		for (Primitive p : gesture()) {
			plot(graphics, p, ghost);
		}
		if (insideCanvas(mouseX, mouseY)) {
			int cx = screenX(cursor.x()), cy = screenY(cursor.y());
			graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF000000 | color);
		}

		// Les couleurs, la courante cerclée de blanc.
		for (int i = 0; i < PALETTE.length; i++) {
			int x = panelX + (i % 8) * (SWATCH + 4), y = swatchY + (i / 8) * (SWATCH + 2);
			graphics.fill(x, y, x + SWATCH, y + SWATCH, 0xFF000000 | PALETTE[i]);
			if (PALETTE[i] == color) {
				graphics.outline(x - 1, y - 1, SWATCH + 2, SWATCH + 2, SELECTED);
			}
		}
		// Ce que dit le tampon : ses étages, et ce que l'emplacement en porte.
		int lineY = infoY;
		for (Component line : stampInfo()) {
			graphics.text(font, line, panelX, lineY, line.getStyle().getColor() == null ? DIM
					: line.getStyle().getColor().getValue() | 0xFF000000, false);
			lineY += 10;
		}
		graphics.text(font, Component.translatable(engrave ? "tattoo.fmab.editor.cost.engrave"
				: "tattoo.fmab.editor.cost.ink"), panelX, lineY, DIM, false);
	}

	/** Le joueur dans l'aperçu, à l'angle choisi, comme l'inventaire le montre. */
	private void player(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, LivingEntity entity) {
		EntityRenderer<? super LivingEntity, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher()
				.getRenderer(entity);
		EntityRenderState state = renderer.createRenderState(entity, 1.0F);
		state.shadowPieces.clear();
		state.outlineColor = 0;
		if (state instanceof LivingEntityRenderState living) {
			living.bodyRot = 180.0F + yaw;
			living.yRot = 0;
			living.xRot = 0;
			living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
			living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
			living.scale = 1.0F;
		}
		int size = Math.min((x1 - x0) * 3 / 5, (y1 - y0) * 2 / 5);
		Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
		graphics.entity(state, size, translation, new Quaternionf().rotateZ((float) Math.PI), new Quaternionf(),
				x0, y0, x1, y1);
	}

	private List<Component> stampInfo() {
		List<Component> out = new ArrayList<>();
		BodyArt art = draft();
		Drawing circle = tool == Tool.STAMP ? stampCircle() : art.circle().orElse(null);
		if (circle == null) {
			out.add(Component.translatable("tattoo.fmab.editor.cosmetic"));
		} else {
			int stages = stages(circle);
			boolean fits = stages > 0 && stages <= slot.stages();
			out.add(Component.translatable("tattoo.fmab.editor.stages", stages, slot.stages())
					.withColor((fits ? GOOD : BAD) & 0xFFFFFF));
		}
		if (slot.maxStamps() > 1) {
			out.add(Component.translatable("tattoo.fmab.editor.stamps", art.circles().size(), slot.maxStamps()));
		}
		Player player = minecraft == null ? null : minecraft.player;
		if (slot.sleeve() && player != null && IshvalArm.knows(player)) {
			// Le client ne sait pas si le joueur connaît l'alkahestry : la recomposition se montre telle
			// qu'elle est tracée, le serveur la lira ou non.
			List<Analysis> drawn = art.circles().stream().map(this::analysis).filter(a -> a != null).toList();
			Set<IshvalArm.Half> halves = IshvalArm.halves(drawn, true);
			String key = halves.size() == 2 ? "both" : halves.contains(IshvalArm.Half.DECOMPOSE) ? "decompose"
					: halves.contains(IshvalArm.Half.RECOMPOSE) ? "recompose" : "none";
			out.add(Component.translatable("tattoo.fmab.editor.ishval",
					Component.translatable("tattoo.fmab.editor.ishval." + key))
					.withColor((halves.isEmpty() ? DIM : GOOD) & 0xFFFFFF));
		}
		return out;
	}

	/** Les étages du cercle. */
	private int stages(Drawing circle) {
		Analysis a = analysis(circle);
		return a == null ? 0 : a.parsed().stages().size();
	}

	/** Le cercle analysé, une fois par cercle et non à chaque image ; null hors d'un monde. */
	private @Nullable Analysis analysis(Drawing circle) {
		if (minecraft == null || minecraft.level == null || minecraft.player == null) {
			return null;
		}
		AlchemistData me = FmabAttachments.ALCHEMIST.get(minecraft.player);
		AlchemyRules rules = AlchemyRules.of(minecraft.level.registryAccess());
		return analyses.computeIfAbsent(circle, c -> rules.analyze(c, me == null ? AlchemistData.NEW : me));
	}

	/** Les arêtes du bras sur la page dépliée, et le nom de chaque face. */
	private void faces(GuiGraphicsExtractor graphics) {
		int ch = canvasH();
		for (int i = 0; i < 4; i++) {
			int x = canvasX + (Sleeve.FACE / 2 + i * Sleeve.FACE) * cell;
			graphics.fill(x, canvasY, x + 1, canvasY + ch, EDGE_LINE);
		}
		List<Sleeve.Face> order = Sleeve.order(slot.isLeft());
		for (int i = 1; i < 4; i++) {
			int middle = canvasX + i * Sleeve.FACE * cell;
			Component name = Component.translatable("tattoo.fmab.editor.face."
					+ order.get(i).name().toLowerCase(Locale.ROOT));
			graphics.text(font, name, middle - font.width(name) / 2, canvasY + 2, FACE_LABEL, false);
		}
	}

	/** Les traits du geste en cours, et le tampon sous la souris. */
	private List<Primitive> gesture() {
		if (tool == Tool.STAMP) {
			Drawing circle = stampCircle();
			return circle == null ? List.of() : stampAt(circle, cursor).placed(page().width());
		}
		if (tool == Tool.FREEHAND) {
			return freehand.size() < 2 ? List.of() : List.of(new Primitive.Polyline(freehand));
		}
		if (anchor == null) {
			return List.of();
		}
		Primitive p = shape(anchor, cursor);
		return p == null ? List.of() : List.of(p);
	}

	private void plot(GuiGraphicsExtractor graphics, Primitive p, int argb) {
		for (Vec2 v : p.sample(0.2)) {
			int x = screenX(v.x()), y = screenY(v.y());
			graphics.fill(x - 1, y - 1, x + 1, y + 1, argb);
		}
	}

	// ---- Saisie ----------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		double mx = event.x(), my = event.y();
		int previewH = Math.max(canvasH(), cell * Drawing.GRID);
		if (mx >= previewX && mx < previewX + PREVIEW_W && my >= canvasY && my < canvasY + previewH) {
			turning = true;
			return true;
		}
		for (int i = 0; i < PALETTE.length; i++) {
			int x = panelX + (i % 8) * (SWATCH + 4), y = swatchY + (i / 8) * (SWATCH + 2);
			if (mx >= x && mx < x + SWATCH && my >= y && my < y + SWATCH) {
				setColor(PALETTE[i]);
				return true;
			}
		}
		if (slots.isEmpty() || !insideCanvas(mx, my)) {
			return false;
		}
		setFocused(null);
		Vec2 p = toGrid(mx, my);
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			resetGesture();
			erase(p);
			return true;
		}
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return false;
		}
		switch (tool) {
			case DOT -> add(new Primitive.Dot(p));
			case STAMP -> {
				Drawing circle = stampCircle();
				if (circle != null) {
					change(stamped(draft(), stampAt(circle, p)));
				}
			}
			case FREEHAND -> {
				freehand.clear();
				freehand.add(fine(mx, my));
			}
			case ARC -> {
				if (arcStart != null) {
					Primitive arc = shape(anchor, p);
					if (arc != null) {
						add(arc);
					}
					resetGesture();
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
		if (turning) {
			yaw = (float) ((yaw - dx * 2) % 360);
			return true;
		}
		cursor = toGrid(event.x(), event.y());
		if (tool == Tool.FREEHAND && !freehand.isEmpty() && insideCanvas(event.x(), event.y())) {
			Vec2 at = fine(event.x(), event.y());
			if (at.distance(freehand.getLast()) >= FREEHAND_STEP) {
				freehand.add(at);
				if (freehand.size() >= FREEHAND_POINTS) {
					// Un trait trop long continue en un nouveau, depuis le même point.
					add(new Primitive.Polyline(freehand));
					freehand.clear();
					freehand.add(at);
				}
			}
			return true;
		}
		return super.mouseDragged(event, dx, dy) || anchor != null;
	}

	@Override
	public void mouseMoved(double x, double y) {
		cursor = toGrid(x, y);
		if (slot.sleeve() && !turning && insideCanvas(x, y)) {
			// L'aperçu se tourne vers la face du bras qu'on survole.
			Sleeve.Face face = Sleeve.faceAt(slot.isLeft(), cursor.x());
			if (face != hovered) {
				hovered = face;
				yaw = facing(slot, face);
			}
		}
		super.mouseMoved(x, y);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (turning) {
			turning = false;
			return true;
		}
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return super.mouseReleased(event);
		}
		if (tool == Tool.FREEHAND && !freehand.isEmpty()) {
			if (freehand.size() == 1) {
				add(new Primitive.Dot(freehand.getFirst()));
			} else {
				add(new Primitive.Polyline(freehand));
			}
			freehand.clear();
			return true;
		}
		if (anchor == null) {
			return super.mouseReleased(event);
		}
		Vec2 p = toGrid(event.x(), event.y());
		if (tool == Tool.ARC) {
			if (arcStart == null && anchor.distance(p) >= 0.5) {
				arcStart = p;
			} else if (arcStart == null) {
				resetGesture();
			}
			return true;
		}
		Primitive shape = shape(anchor, p);
		if (shape != null) {
			add(shape);
		}
		resetGesture();
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.hasControlDown() && event.key() == InputConstants.KEY_Z) {
			undo();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		TattooPreview.clear();
		super.onClose();
	}

	@Override
	public void removed() {
		TattooPreview.clear();
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---- Dessin ----------------------------------------------------------------------------------

	/** La forme de l'outil entre le point de départ et {@code p} ; null si elle est trop petite. */
	private @Nullable Primitive shape(Vec2 from, Vec2 p) {
		return switch (tool) {
			case LINE -> p.equals(from) ? null : new Primitive.Line(from, p);
			case CIRCLE -> {
				double r = snap(from.distance(p));
				yield r < 0.5 ? null : new Primitive.Circle(from, r);
			}
			case POLYGON -> from.distance(p) < 0.5 ? null : polygon(from, p);
			case ARC -> arcStart == null ? new Primitive.Line(from, p) : arc(from, arcStart, p);
			default -> null;
		};
	}

	private Primitive polygon(Vec2 c, Vec2 vertex) {
		double r = c.distance(vertex);
		double start = vertex.sub(c).angle();
		List<Vec2> points = new ArrayList<>();
		for (int i = 0; i < sides; i++) {
			Vec2 v = c.add(Vec2.polar(r, start + 2 * Math.PI * i / sides));
			points.add(clamp(new Vec2(snap(v.x()), snap(v.y()))));
		}
		return new Primitive.Polygon(points);
	}

	private static Primitive arc(Vec2 c, Vec2 start, Vec2 end) {
		double a0 = Math.toDegrees(start.sub(c).angle());
		double a1 = Math.toDegrees(end.sub(c).angle());
		double sweep = ((a1 - a0) % 360 + 360) % 360;
		if (sweep < 1) {
			sweep = 360;
		}
		return new Primitive.Arc(c, Math.max(0.5, snap(c.distance(start))), Math.round(a0), Math.round(sweep));
	}

	/** Le tampon posé en {@code at}, à la taille choisie, gardé dans la grille. */
	private BodyArt.Stamp stampAt(Drawing circle, Vec2 at) {
		return new BodyArt.Stamp(circle, clamp(at), Math.min(stampHalf, page().maxStampHalf()), color, unrolled());
	}

	/** Le prochain tampon se déroule-t-il ? Seulement autour d'un bras. */
	private boolean unrolled() {
		return unroll && slot.sleeve();
	}

	/**
	 * Le dessin avec ce tampon : il remplace le tampon d'un emplacement qui n'en porte qu'un ; autour
	 * d'un bras, il s'ajoute aux autres, ou, la place prise, remplace le plus proche.
	 */
	private BodyArt stamped(BodyArt art, BodyArt.Stamp stamp) {
		if (slot.maxStamps() <= 1) {
			return art.withStamp(stamp);
		}
		if (art.stamps().size() < slot.maxStamps()) {
			return art.plusStamp(stamp);
		}
		int nearest = 0;
		for (int i = 1; i < art.stamps().size(); i++) {
			if (art.stamps().get(i).center().distance(stamp.center())
					< art.stamps().get(nearest).center().distance(stamp.center())) {
				nearest = i;
			}
		}
		return art.withoutStamp(nearest).plusStamp(stamp);
	}

	private void add(Primitive p) {
		BodyArt art = draft();
		if (art.strokes().size() >= BodyArt.MAX_STROKES) {
			return;
		}
		List<BodyArt.Stroke> strokes = new ArrayList<>(art.strokes());
		strokes.add(new BodyArt.Stroke(p, color));
		BodyArt next = new BodyArt(strokes, art.stamps());
		if (next.valid(page())) {
			change(next);
		}
	}

	/** Le trait le plus proche du clic droit s'efface ; sans trait tout près, le tampon sous le clic. */
	private void erase(Vec2 at) {
		BodyArt art = draft();
		int best = -1;
		double bestDistance = 1.0;
		for (int i = 0; i < art.strokes().size(); i++) {
			for (Vec2 v : art.strokes().get(i).shape().sample(0.25)) {
				double d = v.distance(at);
				if (d < bestDistance) {
					bestDistance = d;
					best = i;
				}
			}
		}
		if (best >= 0) {
			change(art.withoutStroke(best));
			return;
		}
		for (int i = art.stamps().size() - 1; i >= 0; i--) {
			BodyArt.Stamp st = art.stamps().get(i);
			if (st.covers(at)) {
				change(art.withoutStamp(i));
				return;
			}
		}
	}

	private void change(BodyArt next) {
		BodyArt before = draft();
		if (next.equals(before)) {
			return;
		}
		history.push(before);
		drafts.put(slot, next);
		showPreview();
	}

	private void undo() {
		resetGesture();
		if (!history.isEmpty()) {
			drafts.put(slot, history.pop());
			showPreview();
		}
	}

	private void apply() {
		if (slots.isEmpty()) {
			return;
		}
		ClientPlatform.get().send(new TattooArtPayload(slot.id(), draft()));
	}

	private void selectSlot(TattooSlot value) {
		resetGesture();
		boolean newPage = !value.canvas().equals(slot.canvas());
		slot = value;
		history.clear();
		hovered = null;
		yaw = facing(value);
		showPreview();
		if (newPage) {
			// La page change de forme : la grille et les outils se replacent autour d'elle.
			rebuildWidgets();
		}
	}

	private void selectTool(Tool value) {
		resetGesture();
		tool = value;
	}

	private void cycleStamp(int delta) {
		int n = pages().size();
		if (n > 0) {
			stampPage = Math.floorMod(stampPage + delta, n);
			tool = Tool.STAMP;
		}
	}

	private void setColor(int rgb) {
		color = rgb & 0xFFFFFF;
		if (hexBox != null && !hexBox.getValue().equalsIgnoreCase(hex(color))) {
			hexBox.setValue(hex(color));
		}
	}

	private void typedColor(String value) {
		String digits = value.startsWith("#") ? value.substring(1) : value;
		if (digits.length() == 6) {
			try {
				color = Integer.parseInt(digits, 16);
			} catch (NumberFormatException ignored) {
				// Un code mal formé n'y change rien : on garde la couleur d'avant.
			}
		}
	}

	private static String hex(int rgb) {
		return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
	}

	private void resetGesture() {
		anchor = null;
		arcStart = null;
		freehand.clear();
	}

	private void showPreview() {
		if (!slots.isEmpty()) {
			TattooPreview.show(slot, draft(), engrave);
		}
	}

	private BodyArt draft() {
		return drafts.getOrDefault(slot, BodyArt.EMPTY);
	}

	/** Ce que l'emplacement porte vraiment (le serveur a pu le changer depuis l'ouverture). */
	private BodyArt current(TattooSlot s) {
		Player player = minecraft == null ? null : minecraft.player;
		return player == null ? BodyArt.EMPTY : BodyCircles.art(player, s).orElse(BodyArt.EMPTY);
	}

	private List<NotebookContents.Page> pages() {
		Player player = minecraft == null ? Minecraft.getInstance().player : minecraft.player;
		List<NotebookContents.Page> all = player == null ? List.of() : Notebooks.of(player).pages();
		return all.stream().filter(p -> !p.drawing().isEmpty()).toList();
	}

	private @Nullable Drawing stampCircle() {
		List<NotebookContents.Page> pages = pages();
		if (pages.isEmpty()) {
			return null;
		}
		stampPage = Math.min(stampPage, pages.size() - 1);
		return pages.get(stampPage).drawing();
	}

	/**
	 * L'angle d'où l'on voit l'emplacement : de face pour la paume et le torse, de dos pour le dos, de
	 * côté pour les bras. On le corrige en tirant sur le joueur.
	 */
	private static float facing(TattooSlot s) {
		return switch (s) {
			case BACK -> 180;
			case ARM_LEFT, ARM_RIGHT -> facing(s, Sleeve.Face.OUTER);
			default -> 0;
		};
	}

	/** L'angle d'où l'on voit cette face du bras. */
	private static float facing(TattooSlot arm, Sleeve.Face face) {
		float side = arm.isLeft() ? -1 : 1;
		return switch (face) {
			case OUTER -> 70 * side;
			case INNER -> -110 * side;
			case FRONT -> 20 * side;
			case BACK -> 180 - 20 * side;
		};
	}

	// ---- Coordonnées -----------------------------------------------------------------------------

	/** La page de l'emplacement montré. */
	private Canvas page() {
		return slot.canvas();
	}

	private int canvasW() {
		return cell * page().width();
	}

	private int canvasH() {
		return cell * page().height();
	}

	private boolean insideCanvas(double x, double y) {
		return x >= canvasX && y >= canvasY && x <= canvasX + canvasW() && y <= canvasY + canvasH();
	}

	/** Position de grille, accrochée à la demi-case. */
	private Vec2 toGrid(double x, double y) {
		return new Vec2(snap(Math.clamp((x - canvasX) / cell, 0, page().width())),
				snap(Math.clamp((y - canvasY) / cell, 0, page().height())));
	}

	/** Position de grille plus fine, pour le trait libre : au quart de case. */
	private Vec2 fine(double x, double y) {
		return new Vec2(Math.round(Math.clamp((x - canvasX) / cell, 0, page().width()) * 4) / 4.0,
				Math.round(Math.clamp((y - canvasY) / cell, 0, page().height()) * 4) / 4.0);
	}

	private Vec2 clamp(Vec2 v) {
		return page().clamp(v);
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
