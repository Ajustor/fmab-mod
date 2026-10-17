package com.ajustor.fmab.client;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.FmabClientConfig;
import com.ajustor.fmab.client.render.AutomailLayer;
import com.ajustor.fmab.client.render.OutfitLayer;
import com.ajustor.fmab.gate.Shells;
import com.ajustor.fmab.network.ShellKeyPayload;
import com.ajustor.fmab.client.render.BossModels;
import com.ajustor.fmab.client.render.ChimeraBeastRenderer;
import com.ajustor.fmab.client.render.ChimeraCrawlerRenderer;
import com.ajustor.fmab.client.render.CornelloRenderer;
import com.ajustor.fmab.client.render.DrachmaSoldierRenderer;
import com.ajustor.fmab.client.render.EnvyRenderer;
import com.ajustor.fmab.client.render.FatherRenderer;
import com.ajustor.fmab.client.render.GateHandRenderer;
import com.ajustor.fmab.client.render.GluttonyRenderer;
import com.ajustor.fmab.client.render.GreedRenderer;
import com.ajustor.fmab.client.render.HauntedArmorRenderer;
import com.ajustor.fmab.client.render.HohenheimRenderer;
import com.ajustor.fmab.client.render.ImmortalSoldierRenderer;
import com.ajustor.fmab.client.render.IzumiRenderer;
import com.ajustor.fmab.client.render.KunaiRenderer;
import com.ajustor.fmab.client.render.LustRenderer;
import com.ajustor.fmab.client.render.MarcohRenderer;
import com.ajustor.fmab.client.render.MayChangRenderer;
import com.ajustor.fmab.client.render.OlivierRenderer;
import com.ajustor.fmab.client.render.PrideRenderer;
import com.ajustor.fmab.client.render.ScarRenderer;
import com.ajustor.fmab.client.render.SlothRenderer;
import com.ajustor.fmab.client.render.SoldierRenderer;
import com.ajustor.fmab.client.render.StateExaminerRenderer;
import com.ajustor.fmab.client.render.StoneGolemRenderer;
import com.ajustor.fmab.client.render.ThrowingKnifeRenderer;
import com.ajustor.fmab.client.render.TransmutationCircleRenderer;
import com.ajustor.fmab.client.render.TruthRenderer;
import com.ajustor.fmab.client.render.WheelchairRenderer;
import com.ajustor.fmab.client.render.WinryRenderer;
import com.ajustor.fmab.client.render.WrathRenderer;
import com.ajustor.fmab.client.screen.BargainScreen;
import com.ajustor.fmab.client.screen.BodyScreen;
import com.ajustor.fmab.client.screen.CircleWheelScreen;
import com.ajustor.fmab.client.screen.DesignScreen;
import com.ajustor.fmab.client.screen.ExamScreen;
import com.ajustor.fmab.client.screen.GlovesScreen;
import com.ajustor.fmab.client.screen.InventoryTabs;
import com.ajustor.fmab.client.screen.IzumiScreen;
import com.ajustor.fmab.client.screen.NotebookScreen;
import com.ajustor.fmab.client.screen.TattooScreen;
import com.ajustor.fmab.client.screen.TreatiseScreen;
import com.ajustor.fmab.client.screen.SoulGiftScreen;
import com.ajustor.fmab.client.screen.SoulOfferScreen;
import com.ajustor.fmab.client.screen.StoneChoiceScreen;
import com.ajustor.fmab.client.screen.TruthScreen;
import com.ajustor.fmab.client.screen.WinryScreen;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.network.CastGlovesPayload;
import com.ajustor.fmab.network.CinematicPayload;
import com.ajustor.fmab.network.BargainPayload;
import com.ajustor.fmab.network.OpenDesignsPayload;
import com.ajustor.fmab.network.OpenExamPayload;
import com.ajustor.fmab.network.OpenIzumiPayload;
import com.ajustor.fmab.network.OpenTattooPayload;
import com.ajustor.fmab.network.OpenTruthPayload;
import com.ajustor.fmab.network.SoulGiftPayload;
import com.ajustor.fmab.network.SoulOfferPayload;
import com.ajustor.fmab.network.StoneChoicePayload;
import com.ajustor.fmab.network.OpenWinryPayload;
import com.ajustor.fmab.network.RequestDesignsPayload;
import com.ajustor.fmab.network.SelectCirclePayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlockEntities;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.registry.FmabMenus;
import com.ajustor.fmab.stone.Eclipse;
import com.ajustor.fmab.transmutation.Concentration;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import com.ajustor.fmab.platform.client.ClientEvents;
import com.ajustor.fmab.platform.client.ClientPlatform;

public final class FmabClient {
	private FmabClient() {
	}

	/** Lance le client du mod : appelé par le point d'entrée du loader, une fois ses événements branchés. */
	public static void init() {
		// Avant le premier chargement des langues : celle choisie pour le mod s'y applique déjà.
		FmabClientConfig.load();
		Fmab.setClientHooks((kind, hand) -> {
			switch (kind) {
				case TREATISE -> Minecraft.getInstance().gui.setScreen(new TreatiseScreen());
			}
		});
		KeyMapping.Category category = KeyMapping.Category.register(Fmab.id("alchemy"));
		KeyMapping cast = ClientPlatform.get().keyMapping(
				new KeyMapping("key.fmab.cast_gloves", InputConstants.KEY_G, category));
		KeyMapping gloves = ClientPlatform.get().keyMapping(
				new KeyMapping("key.fmab.gloves", InputConstants.KEY_H, category));
		KeyMapping notebook = ClientPlatform.get().keyMapping(
				new KeyMapping("key.fmab.notebook", InputConstants.KEY_N, category));
		// La page de conception : l'objet auquel on pense en recomposant.
		KeyMapping design = ClientPlatform.get().keyMapping(
				new KeyMapping("key.fmab.design", InputConstants.KEY_J, category));
		KeyMapping wheel = ClientPlatform.get().keyMapping(
				new KeyMapping("key.fmab.circle_wheel", InputConstants.KEY_R, category));
		// Passer d'un cercle à l'autre sans ouvrir la roue : sans touche par défaut.
		KeyMapping next = ClientPlatform.get().keyMapping(
				new KeyMapping("key.fmab.next_circle", InputConstants.UNKNOWN.getValue(), category));
		KeyMapping previous = ClientPlatform.get().keyMapping(
				new KeyMapping("key.fmab.previous_circle", InputConstants.UNKNOWN.getValue(), category));
		// L'âme en armure se fige (une armure qu'on peut revêtir), s'éveille, ou relâche qui est dedans.
		KeyMapping shell = ClientPlatform.get().keyMapping(
				new KeyMapping("key.fmab.shell", InputConstants.KEY_V, category));
		ClientEvents.END_TICK.register(mc -> {
			while (shell.consumeClick()) {
				ClientPlatform.get().send(ShellKeyPayload.INSTANCE);
			}
			while (cast.consumeClick()) {
				// Maj : joindre les mains, les deux gants agissent.
				ClientPlatform.get().send(new CastGlovesPayload(mc.hasShiftDown()));
			}
			while (gloves.consumeClick()) {
				mc.gui.setScreen(new GlovesScreen());
			}
			while (notebook.consumeClick()) {
				mc.gui.setScreen(new NotebookScreen());
			}
			while (design.consumeClick()) {
				ClientPlatform.get().send(RequestDesignsPayload.INSTANCE);
			}
			while (wheel.consumeClick()) {
				CircleWheelScreen.open(wheel);
			}
			while (next.consumeClick()) {
				cycleCircle(mc, 1);
			}
			while (previous.consumeClick()) {
				cycleCircle(mc, -1);
			}
		});
		ClientPlatform.get().entityRenderer(FmabEntities.IZUMI, IzumiRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.STATE_EXAMINER, StateExaminerRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.STONE_GOLEM, StoneGolemRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.TRUTH, TruthRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.WINRY, WinryRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.WHEELCHAIR, WheelchairRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.LUST, LustRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.GLUTTONY, GluttonyRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.ENVY, EnvyRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.GREED, GreedRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.SLOTH, SlothRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.WRATH, WrathRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.PRIDE, PrideRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.FATHER, FatherRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.KUNAI, KunaiRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.MAY_CHANG, MayChangRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.DRACHMA_SOLDIER, DrachmaSoldierRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.OLIVIER, OlivierRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.AMESTRIAN_SOLDIER, c -> new SoldierRenderer(c, "amestrian_soldier"));
		ClientPlatform.get().entityRenderer(FmabEntities.BRIGGS_SOLDIER, c -> new SoldierRenderer(c, "briggs_soldier"));
		ClientPlatform.get().entityRenderer(FmabEntities.IMMORTAL_SOLDIER, ImmortalSoldierRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.HAUNTED_ARMOR, c -> new HauntedArmorRenderer<>(c, "haunted_armor"));
		ClientPlatform.get().entityRenderer(FmabEntities.BARRY, c -> new HauntedArmorRenderer<>(c, "barry", BossModels.barry()));
		ClientPlatform.get().entityRenderer(FmabEntities.CHIMERA_BEAST, ChimeraBeastRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.CHIMERA_CRAWLER, ChimeraCrawlerRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.ALCHEMICAL_MINE, NoopRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.THROWING_KNIFE, ThrowingKnifeRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.CORNELLO, CornelloRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.SCAR, ScarRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.HOHENHEIM, HohenheimRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.MARCOH, MarcohRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.GATE_HAND, GateHandRenderer::new);
		ClientPlatform.get().entityRenderer(FmabEntities.FATHER_SUN, c -> new ThrownItemRenderer<>(c, 4.0F, true));
		// Les automails se dessinent sur le corps du joueur, à la place des membres perdus.
		ClientPlatform.get().renderLayers((type, renderer, layers, context) -> {
			if (renderer instanceof AvatarRenderer avatar) {
				layers.add(new AutomailLayer(avatar, context));
			}
			// Les volumes des tenues (capuche, pans, épaulières...), sur tout corps humanoïde qui les porte.
			if (renderer.getModel() instanceof net.minecraft.client.model.HumanoidModel<?>) {
				registerOutfits(layers, renderer);
			}
		});
		ClientPlatform.get().receive(CinematicPayload.TYPE, (payload, client) -> Cinematics.start(payload));
		ClientEvents.END_TICK.register(mc -> Cinematics.tick());
		// Pendant une cinématique, le chat est caché sous les bandes noires : ce que dit la Vérité
		// s'affiche aussi en sous-titre.
		ClientEvents.GAME_MESSAGE.register((message, overlay) -> {
			if (!overlay) {
				Cinematics.subtitle(message);
			}
		});
		// Une nouvelle connexion (ou un autre monde) repart sans cinématique en cours.
		ClientEvents.DISCONNECT.register(client -> Cinematics.stop());
		ClientPlatform.get().receive(OpenTruthPayload.TYPE,
				(payload, client) -> client.gui.setScreen(new TruthScreen(payload)));
		ClientPlatform.get().receive(SoulGiftPayload.TYPE,
				(payload, client) -> client.gui.setScreen(new SoulGiftScreen(payload)));
		ClientPlatform.get().receive(SoulOfferPayload.TYPE,
				(payload, client) -> client.gui.setScreen(new SoulOfferScreen(payload)));
		ClientPlatform.get().receive(StoneChoicePayload.TYPE,
				(payload, client) -> client.gui.setScreen(new StoneChoiceScreen(payload)));
		ClientPlatform.get().receive(BargainPayload.TYPE, (payload, client) -> {
			if (client.gui.screen() instanceof BargainScreen open) {
				if (payload.souls() < 0) {
					open.closeSilently();
				} else {
					open.update(payload);
				}
			} else if (payload.souls() >= 0) {
				client.gui.setScreen(new BargainScreen(payload));
			}
		});
		ClientPlatform.get().receive(OpenWinryPayload.TYPE,
				(payload, client) -> client.gui.setScreen(new WinryScreen(payload.entityId())));
		ClientPlatform.get().receive(OpenDesignsPayload.TYPE,
				(payload, client) -> client.gui.setScreen(new DesignScreen(payload)));
		ClientPlatform.get().receive(OpenTattooPayload.TYPE,
				(payload, client) -> client.gui.setScreen(new TattooScreen(payload)));
		ClientEvents.ITEM_TOOLTIP.register((stack, context, flag, lines) -> {
			ItemHints.append(stack, lines);
			if (stack.has(FmabComponents.EMBROIDERY)) {
				lines.add(Component.translatable("item.fmab.embroidered").withStyle(ChatFormatting.DARK_AQUA));
			}
			// Décomposé une fois sur un cercle : l'alchimiste sait le recomposer d'après modèle.
			var player = Minecraft.getInstance().player;
			if (player != null && FmabAttachments.UNDERSTOOD.getOrCreate(player)
					.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
				lines.add(Component.translatable("item.fmab.understood").withStyle(ChatFormatting.DARK_AQUA));
			}
		});
		ClientPlatform.get().receive(OpenExamPayload.TYPE,
				(payload, client) -> client.gui.setScreen(new ExamScreen(payload)));
		ClientPlatform.get().receive(OpenIzumiPayload.TYPE,
				(payload, client) -> client.gui.setScreen(new IzumiScreen(payload)));
		ClientPlatform.get().blockEntityRenderer(FmabBlockEntities.TRANSMUTATION_CIRCLE, TransmutationCircleRenderer::new);
		MenuScreens.register(FmabMenus.BODY, BodyScreen::new);
		ClientPlatform.get().hudElement(Fmab.id("concentration"), ClientPlatform.HudAnchor.AFTER_FOOD_BAR,
				(graphics, delta) -> concentrationBar(graphics));
		ClientPlatform.get().hudElement(Fmab.id("lost_sight"), ClientPlatform.HudAnchor.BEFORE_MISC_OVERLAYS,
				(graphics, delta) -> lostSight(graphics));
		ClientPlatform.get().hudElement(Fmab.id("shell_slit"), ClientPlatform.HudAnchor.BEFORE_MISC_OVERLAYS,
				(graphics, delta) -> shellSlit(graphics));
		ClientPlatform.get().hudElement(Fmab.id("eclipse"), ClientPlatform.HudAnchor.BEFORE_MISC_OVERLAYS,
				(graphics, delta) -> eclipse(graphics));
		ClientPlatform.get().hudElement(Fmab.id("living_stone"), ClientPlatform.HudAnchor.AFTER_FOOD_BAR,
				(graphics, delta) -> livingStone(graphics));
		ClientPlatform.get().hudElement(Fmab.id("alchemist_panel"), ClientPlatform.HudAnchor.BEFORE_MISC_OVERLAYS,
				(graphics, delta) -> AlchemyHud.draw(graphics));
	}

	/** Le cercle suivant (ou précédent) du carnet devient la sélection, sans ouvrir la roue. */
	private static final Identifier LOST_SIGHT = Fmab.id("textures/gui/hud/lost_sight.png");
	private static final Identifier CONCENTRATION_ICON = Fmab.id("textures/gui/hud/concentration.png");
	/** L'icône du compteur d'âmes, créée au premier affichage (les objets ne sont pas prêts au chargement). */
	private static ItemStack soulsIcon;

	private static void cycleCircle(Minecraft mc, int delta) {
		if (mc.player == null) {
			return;
		}
		NotebookContents contents = Notebooks.of(mc.player);
		if (contents.pages().isEmpty()) {
			return;
		}
		int index = Math.floorMod(contents.selected() + delta, contents.pages().size());
		ClientPlatform.get().send(new SelectCirclePayload(index, false));
	}

	/**
	 * La vue que la Vérité a prise : un voile noir qui ne laisse qu'un trou flou au centre, comme
	 * Mustang après le Jour promis.
	 */
	private static void lostSight(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		GateState gate = FmabAttachments.GATE.get(mc.player);
		if (gate == null) {
			return;
		}
		// Une armure d'âme voit par son heaume : sans heaume, elle voit aussi mal qu'un aveugle.
		boolean blind = gate.soulBound()
				? gate.inArmor() && mc.player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
				: gate.lost(BodyPart.SIGHT);
		if (!blind) {
			return;
		}
		// Un voile peint en dégradé doux, étiré à tout l'écran : le trou reste au centre, les bords se perdent.
		int w = graphics.guiWidth();
		int h = graphics.guiHeight();
		graphics.blit(RenderPipelines.GUI_TEXTURED, LOST_SIGHT, 0, 0, 0, 0, w, h, 256, 256, 256, 256);
	}

	/**
	 * Dans l'armure d'une âme, on voit par la fente du heaume : le haut et le bas de l'écran dans le
	 * noir, une bande claire au milieu, ses bords fondus.
	 */
	private static void shellSlit(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !Shells.inside(mc.player)) {
			return;
		}
		int w = graphics.guiWidth();
		int h = graphics.guiHeight();
		int top = h * 2 / 5;
		int bottom = h * 3 / 5;
		int fade = Math.max(4, h / 14);
		int dark = 0xF0050608;
		graphics.fill(0, 0, w, top - fade, dark);
		graphics.fill(0, bottom + fade, w, h, dark);
		graphics.fillGradient(0, top - fade, w, top, dark, 0x00050608);
		graphics.fillGradient(0, bottom, w, bottom + fade, 0x00050608, dark);
	}

	/** Pendant l'éclipse, le jour se teinte d'un crépuscule orangé et sombre. */
	private static void eclipse(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !Eclipse.now(mc.level) || !mc.level.canSeeSky(mc.player.blockPosition())) {
			return;
		}
		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0x401A0800);
	}

	/** Le compteur d'âmes d'une Pierre philosophale vivante, au-dessus de la barre de concentration. */
	private static void livingStone(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		Integer souls = FmabAttachments.LIVING_STONE.get(mc.player);
		if (souls == null || souls <= 0) {
			return;
		}
		// La Pierre et son nombre d'âmes, sur un fond discret, au-dessus de la jauge de concentration.
		int x = graphics.guiWidth() / 2 + 10;
		int y = graphics.guiHeight() - 39 - 10 - 26;
		Component text = Component.translatable("hud.fmab.living_stone", souls);
		graphics.fill(x - 2, y - 1, x + 20 + mc.font.width(text) + 3, y + 17, 0x80000000);
		if (soulsIcon == null) {
			soulsIcon = new ItemStack(FmabItems.PHILOSOPHER_STONE);
		}
		graphics.item(soulsIcon, x, y);
		graphics.text(mc.font, text, x + 19, y + 4, 0xFFFF5A5A, true);
	}

	/**
	 * Barre de concentration au-dessus de la faim. Elle ne s'affiche qu'en dessous du maximum :
	 * un alchimiste reposé n'a pas besoin de la voir.
	 */
	private static void concentrationBar(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		float concentration = Concentration.of(mc.player), max = Concentration.max(mc.player);
		if (concentration >= max) {
			return;
		}
		int width = 81;
		int x = graphics.guiWidth() / 2 + 10;
		int y = graphics.guiHeight() - 39 - 10 - 4;
		// Une jauge encadrée : son icône, un remplissage bleu à reflet, une graduation par quart.
		int filled = Math.round(width * concentration / max);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONCENTRATION_ICON, x - 11, y - 3, 0, 0, 9, 9, 9, 9, 9, 9);
		graphics.fill(x - 2, y - 2, x + width + 2, y + 5, 0xFF1A1E28);
		graphics.fill(x - 1, y - 1, x + width + 1, y + 4, 0xFF39414F);
		graphics.fill(x, y, x + width, y + 3, 0xFF141820);
		graphics.fill(x, y, x + filled, y + 3, 0xFF3C8CE6);
		graphics.fill(x, y, x + filled, y + 1, 0xFF9ED0FF);
		for (int quarter = 1; quarter < 4; quarter++) {
			graphics.fill(x + width * quarter / 4, y, x + width * quarter / 4 + 1, y + 3, 0x80000000);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void registerOutfits(ClientPlatform.LayerSink layers,
			net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?, ?> renderer) {
		layers.add(new OutfitLayer((net.minecraft.client.renderer.entity.RenderLayerParent) renderer));
	}
}
