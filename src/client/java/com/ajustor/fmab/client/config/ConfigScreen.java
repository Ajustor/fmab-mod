package com.ajustor.fmab.client.config;

import com.ajustor.fmab.FmabClientConfig;
import com.ajustor.fmab.FmabConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Les réglages du mod, ouverts depuis le menu des mods (Mod Menu) : la langue du mod, l'affichage du
 * cercle sélectionné et les infobulles, puis le réglage des mondes solo. Rien n'est écrit avant
 * « Terminé » ; la langue ne se recharge que si elle a changé.
 */
public class ConfigScreen extends Screen {
	private static final int BUTTON_WIDTH = 240;
	private static final int ROW = 24;
	private static final int HEADER = 0xFFE0C080;

	private final Screen parent;
	private FmabClientConfig client = FmabClientConfig.get();
	private FmabConfig world = FmabConfig.get();
	private int interfaceTop;
	private int worldTop;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("config.fmab.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = (width - BUTTON_WIDTH) / 2;
		interfaceTop = 36;
		int y = interfaceTop + 14;

		List<String> languages = new ArrayList<>();
		languages.add("");
		languages.addAll(ModLanguage.available(minecraft.getResourceManager()));
		if (!languages.contains(client.language())) {
			languages.add(client.language());
		}
		addRenderableWidget(CycleButton.<String>builder(
						code -> code.isEmpty() ? Component.translatable("config.fmab.language.game") : ModLanguage.name(code),
						client.language())
				.withValues(languages)
				.withTooltip(code -> Tooltip.create(Component.translatable("config.fmab.language.tooltip")))
				.create(x, y, BUTTON_WIDTH, 20, Component.translatable("config.fmab.language"),
						(button, code) -> client = client.withLanguage(code)));
		y += ROW;
		addRenderableWidget(CycleButton.onOffBuilder(client.circleHud())
				.withTooltip(on -> Tooltip.create(Component.translatable("config.fmab.circle_hud.tooltip")))
				.create(x, y, BUTTON_WIDTH, 20, Component.translatable("config.fmab.circle_hud"),
						(button, on) -> client = client.withCircleHud(on)));
		y += ROW;
		addRenderableWidget(CycleButton.onOffBuilder(client.itemHints())
				.withTooltip(on -> Tooltip.create(Component.translatable("config.fmab.item_hints.tooltip")))
				.create(x, y, BUTTON_WIDTH, 20, Component.translatable("config.fmab.item_hints"),
						(button, on) -> client = client.withItemHints(on)));

		worldTop = y + ROW + 10;
		y = worldTop + 14;
		addRenderableWidget(CycleButton.onOffBuilder(world.restartWipesProgress())
				.withTooltip(on -> Tooltip.create(Component.translatable("config.fmab.restart_wipes_progress.tooltip")))
				.create(x, y, BUTTON_WIDTH, 20, Component.translatable("config.fmab.restart_wipes_progress"),
						(button, on) -> world = world.withRestartWipesProgress(on)));
		y += ROW;
		addRenderableWidget(CycleButton.onOffBuilder(world.soulBindingRequiresSacrifice())
				.withTooltip(on -> Tooltip.create(Component.translatable("config.fmab.soul_binding_requires_sacrifice.tooltip")))
				.create(x, y, BUTTON_WIDTH, 20, Component.translatable("config.fmab.soul_binding_requires_sacrifice"),
						(button, on) -> world = world.withSoulBindingRequiresSacrifice(on)));

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds((width - 200) / 2, height - 28, 200, 20).build());
	}

	/** Écrit les réglages et revient au menu des mods. */
	@Override
	public void onClose() {
		boolean languageChanged = !client.language().equals(FmabClientConfig.get().language());
		FmabClientConfig.save(client);
		FmabConfig.save(world);
		if (languageChanged) {
			ModLanguage.apply(client.language());
		}
		minecraft.gui.setScreen(parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(font, title, width / 2, 14, 0xFFFFFFFF);
		int x = (width - BUTTON_WIDTH) / 2;
		graphics.text(font, Component.translatable("config.fmab.section.interface"), x, interfaceTop, HEADER, true);
		boolean remote = minecraft.level != null && minecraft.getSingleplayerServer() == null;
		graphics.text(font, Component.translatable(remote ? "config.fmab.section.world.remote" : "config.fmab.section.world"),
				x, worldTop, HEADER, true);
	}
}
