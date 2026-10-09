package com.ajustor.fmab.client;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.client.render.ChimeraBeastRenderer;
import com.ajustor.fmab.client.render.ChimeraCrawlerRenderer;
import com.ajustor.fmab.client.render.DrachmaSoldierRenderer;
import com.ajustor.fmab.client.render.EnvyRenderer;
import com.ajustor.fmab.client.render.FatherRenderer;
import com.ajustor.fmab.client.render.GluttonyRenderer;
import com.ajustor.fmab.client.render.GreedRenderer;
import com.ajustor.fmab.client.render.HauntedArmorRenderer;
import com.ajustor.fmab.client.render.ImmortalSoldierRenderer;
import com.ajustor.fmab.client.render.IzumiRenderer;
import com.ajustor.fmab.client.render.KunaiRenderer;
import com.ajustor.fmab.client.render.LustRenderer;
import com.ajustor.fmab.client.render.MayChangRenderer;
import com.ajustor.fmab.client.render.OlivierRenderer;
import com.ajustor.fmab.client.render.PrideRenderer;
import com.ajustor.fmab.client.render.SlothRenderer;
import com.ajustor.fmab.client.render.SoldierRenderer;
import com.ajustor.fmab.client.render.StateExaminerRenderer;
import com.ajustor.fmab.client.render.StoneGolemRenderer;
import com.ajustor.fmab.client.render.ThrowingKnifeRenderer;
import com.ajustor.fmab.client.render.TransmutationCircleRenderer;
import com.ajustor.fmab.client.render.TruthRenderer;
import com.ajustor.fmab.client.render.WinryRenderer;
import com.ajustor.fmab.client.render.WrathRenderer;
import com.ajustor.fmab.client.screen.ExamScreen;
import com.ajustor.fmab.client.screen.GlovesScreen;
import com.ajustor.fmab.client.screen.IzumiScreen;
import com.ajustor.fmab.client.screen.NotebookScreen;
import com.ajustor.fmab.client.screen.TattooScreen;
import com.ajustor.fmab.client.screen.TreatiseScreen;
import com.ajustor.fmab.client.screen.TruthScreen;
import com.ajustor.fmab.client.screen.WinryScreen;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.network.CastGlovesPayload;
import com.ajustor.fmab.network.OpenExamPayload;
import com.ajustor.fmab.network.OpenIzumiPayload;
import com.ajustor.fmab.network.OpenTattooPayload;
import com.ajustor.fmab.network.OpenTruthPayload;
import com.ajustor.fmab.network.OpenWinryPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlockEntities;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.stone.Eclipse;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import org.lwjgl.glfw.GLFW;

public class FmabClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Fmab.setClientHooks((kind, hand) -> {
			Minecraft mc = Minecraft.getInstance();
			switch (kind) {
				case NOTEBOOK -> mc.gui.setScreen(new NotebookScreen(hand, mc.player.getItemInHand(hand)));
				case TREATISE -> mc.gui.setScreen(new TreatiseScreen());
			}
		});
		KeyMapping.Category category = KeyMapping.Category.register(Fmab.id("alchemy"));
		KeyMapping cast = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.fmab.cast_gloves", GLFW.GLFW_KEY_G, category));
		KeyMapping gloves = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.fmab.gloves", GLFW.GLFW_KEY_H, category));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (cast.consumeClick()) {
				// Maj : joindre les mains, les deux gants agissent.
				ClientPlayNetworking.send(new CastGlovesPayload(mc.hasShiftDown()));
			}
			while (gloves.consumeClick()) {
				mc.gui.setScreen(new GlovesScreen());
			}
		});
		EntityRendererRegistry.register(FmabEntities.IZUMI, IzumiRenderer::new);
		EntityRendererRegistry.register(FmabEntities.STATE_EXAMINER, StateExaminerRenderer::new);
		EntityRendererRegistry.register(FmabEntities.STONE_GOLEM, StoneGolemRenderer::new);
		EntityRendererRegistry.register(FmabEntities.TRUTH, TruthRenderer::new);
		EntityRendererRegistry.register(FmabEntities.WINRY, WinryRenderer::new);
		EntityRendererRegistry.register(FmabEntities.LUST, LustRenderer::new);
		EntityRendererRegistry.register(FmabEntities.GLUTTONY, GluttonyRenderer::new);
		EntityRendererRegistry.register(FmabEntities.ENVY, EnvyRenderer::new);
		EntityRendererRegistry.register(FmabEntities.GREED, GreedRenderer::new);
		EntityRendererRegistry.register(FmabEntities.SLOTH, SlothRenderer::new);
		EntityRendererRegistry.register(FmabEntities.WRATH, WrathRenderer::new);
		EntityRendererRegistry.register(FmabEntities.PRIDE, PrideRenderer::new);
		EntityRendererRegistry.register(FmabEntities.FATHER, FatherRenderer::new);
		EntityRendererRegistry.register(FmabEntities.KUNAI, KunaiRenderer::new);
		EntityRendererRegistry.register(FmabEntities.MAY_CHANG, MayChangRenderer::new);
		EntityRendererRegistry.register(FmabEntities.DRACHMA_SOLDIER, DrachmaSoldierRenderer::new);
		EntityRendererRegistry.register(FmabEntities.OLIVIER, OlivierRenderer::new);
		EntityRendererRegistry.register(FmabEntities.AMESTRIAN_SOLDIER, c -> new SoldierRenderer(c, "amestrian_soldier"));
		EntityRendererRegistry.register(FmabEntities.BRIGGS_SOLDIER, c -> new SoldierRenderer(c, "briggs_soldier"));
		EntityRendererRegistry.register(FmabEntities.IMMORTAL_SOLDIER, ImmortalSoldierRenderer::new);
		EntityRendererRegistry.register(FmabEntities.HAUNTED_ARMOR, c -> new HauntedArmorRenderer<>(c, "haunted_armor"));
		EntityRendererRegistry.register(FmabEntities.BARRY, c -> new HauntedArmorRenderer<>(c, "barry"));
		EntityRendererRegistry.register(FmabEntities.CHIMERA_BEAST, ChimeraBeastRenderer::new);
		EntityRendererRegistry.register(FmabEntities.CHIMERA_CRAWLER, ChimeraCrawlerRenderer::new);
		EntityRendererRegistry.register(FmabEntities.ALCHEMICAL_MINE, NoopRenderer::new);
		EntityRendererRegistry.register(FmabEntities.THROWING_KNIFE, ThrowingKnifeRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(OpenTruthPayload.TYPE,
				(payload, context) -> context.client().gui.setScreen(new TruthScreen(payload)));
		ClientPlayNetworking.registerGlobalReceiver(OpenWinryPayload.TYPE,
				(payload, context) -> context.client().gui.setScreen(new WinryScreen(payload.entityId())));
		ClientPlayNetworking.registerGlobalReceiver(OpenTattooPayload.TYPE,
				(payload, context) -> context.client().gui.setScreen(new TattooScreen(payload)));
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.has(FmabComponents.EMBROIDERY)) {
				lines.add(Component.translatable("item.fmab.embroidered").withStyle(ChatFormatting.DARK_AQUA));
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(OpenExamPayload.TYPE,
				(payload, context) -> context.client().gui.setScreen(new ExamScreen(payload)));
		ClientPlayNetworking.registerGlobalReceiver(OpenIzumiPayload.TYPE,
				(payload, context) -> context.client().gui.setScreen(new IzumiScreen(payload)));
		BlockEntityRendererRegistry.register(FmabBlockEntities.TRANSMUTATION_CIRCLE, TransmutationCircleRenderer::new);
		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, Fmab.id("concentration"),
				(graphics, delta) -> concentrationBar(graphics));
		HudElementRegistry.attachElementBefore(VanillaHudElements.MISC_OVERLAYS, Fmab.id("lost_sight"),
				(graphics, delta) -> lostSight(graphics));
		HudElementRegistry.attachElementBefore(VanillaHudElements.MISC_OVERLAYS, Fmab.id("eclipse"),
				(graphics, delta) -> eclipse(graphics));
		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, Fmab.id("living_stone"),
				(graphics, delta) -> livingStone(graphics));
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
		GateState gate = mc.player.getAttached(FmabAttachments.GATE);
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
		int w = graphics.guiWidth();
		int h = graphics.guiHeight();
		int cx = w / 2;
		int cy = h / 2;
		double clear = Math.min(w, h) * 0.18;
		double dark = Math.min(w, h) * 0.55;
		int step = 6;
		for (int y = 0; y < h; y += step) {
			for (int x = 0; x < w; x += step) {
				double d = Math.hypot(x + step / 2.0 - cx, (y + step / 2.0 - cy) * 1.2);
				double t = Math.clamp((d - clear) / (dark - clear), 0, 1);
				int alpha = (int) (t * 250);
				if (alpha > 0) {
					graphics.fill(x, y, x + step, y + step, alpha << 24);
				}
			}
		}
	}

	/** Pendant l'éclipse, le jour se teinte d'un crépuscule orangé et sombre. */
	private static void eclipse(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !Eclipse.now(mc.level) || !mc.level.canSeeSky(mc.player.blockPosition())) {
			return;
		}
		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0x7A1A0800);
	}

	/** Le compteur d'âmes d'une Pierre philosophale vivante, au-dessus de la barre de concentration. */
	private static void livingStone(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		Integer souls = mc.player.getAttached(FmabAttachments.LIVING_STONE);
		if (souls == null || souls <= 0) {
			return;
		}
		int x = graphics.guiWidth() / 2 + 10;
		int y = graphics.guiHeight() - 39 - 10 - 16;
		graphics.text(mc.font, Component.translatable("hud.fmab.living_stone", souls), x, y, 0xFFE0303A, true);
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
		AlchemistData data = mc.player.getAttached(FmabAttachments.ALCHEMIST);
		if (data == null || data.concentration() >= AlchemistData.MAX_CONCENTRATION) {
			return;
		}
		int width = 81;
		int x = graphics.guiWidth() / 2 + 10;
		int y = graphics.guiHeight() - 39 - 10 - 4;
		int filled = Math.round(width * data.concentration() / AlchemistData.MAX_CONCENTRATION);
		graphics.fill(x - 1, y - 1, x + width + 1, y + 4, 0xC0000000);
		graphics.fill(x, y, x + filled, y + 3, 0xFF4FA3FF);
	}
}
