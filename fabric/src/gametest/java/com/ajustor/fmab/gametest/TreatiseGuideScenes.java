package com.ajustor.fmab.gametest;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.Treatise;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.client.screen.TreatiseScreen;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.Transmutation;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Le Traité guide le débutant : il s'ouvre sur ses premiers pas, ses textes trop longs se poursuivent
 * page suivante, et les cercles du quotidien font dans le monde ce que disent leurs pages (le
 * mur, le pont de glace, le puits de mine, l'interrupteur de cuivre, le four). Chaque page et chaque
 * cercle à l'œuvre est capturé. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=treatise_guide}.
 */
public class TreatiseGuideScenes implements FabricClientGameTest {
	/** Le cercle, au sol devant le joueur ; il regarde vers le sud, comme le haut de la page. */
	private static final BlockPos CIRCLE = new BlockPos(0, -60, 3);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("treatise_guide")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("difficulty peaceful");
			sp.getConnection().waitForChunksRender();
			context.waitTicks(20);
			// En français, puis en anglais : les deux Traités se lisent en entier.
			language(context, "fr_fr");
			pages(context, "fr");
			language(context, "en_us");
			pages(context, "en");
			language(context, "fr_fr");
			world(context, sp);
			language(context, "en_us");
		}
	}

	/** Le Traité d'un nouveau venu, page par page. */
	private static void pages(ClientGameTestContext context, String lang) {
		context.setScreen(TreatiseScreen::new);
		context.waitTicks(2);
		check("treatise.fmab.start.welcome".equals(text(context)), "un nouveau venu devrait ouvrir le Traité sur ses premiers pas");
		context.takeScreenshot("guide_" + lang + "_00_welcome");
		String[] shown = {"treatise.fmab.start.kit", "treatise.fmab.start.choose", "treatise.fmab.start.draw",
				"treatise.fmab.start.palm", "treatise.fmab.start.trouble", "treatise.fmab.start.practice",
				"treatise.fmab.start.next", "treatise.fmab.everyday.p1", "treatise.fmab.everyday.night",
				"treatise.fmab.everyday.river", "treatise.fmab.everyday.mine", "treatise.fmab.everyday.switch",
				"treatise.fmab.everyday.repair", "treatise.fmab.everyday.recycling", "treatise.fmab.everyday.furnace",
				"treatise.fmab.everyday.trap", "treatise.fmab.everyday.firebreak", "treatise.fmab.everyday.habits",
				"treatise.fmab.design.p5", "treatise.fmab.design.aimed_spike"};
		for (int i = 0; i < shown.length; i++) {
			String key = shown[i];
			check(context.computeOnClient(mc -> ((TreatiseScreen) mc.gui.screen()).goToPage(key)), key + " devrait se lire");
			context.waitTick();
			context.takeScreenshot(String.format("guide_%s_%02d_%s", lang, i + 1, key.substring("treatise.fmab.".length())));
		}
		// Un texte trop long pour sa page se poursuit à la suivante, plutôt que d'être coupé.
		check(context.computeOnClient(mc -> ((TreatiseScreen) mc.gui.screen()).goToPage("treatise.fmab.ch2.objects")),
				"la fabrication des objets devrait se lire");
		context.waitTick();
		context.takeScreenshot("guide_" + lang + "_30_long_text");
		context.runOnClient(mc -> ((TreatiseScreen) mc.gui.screen()).nextPage());
		context.waitTick();
		check("treatise.fmab.ch2.objects (suite)".equals(text(context)), "la suite devrait venir page suivante : " + text(context));
		context.takeScreenshot("guide_" + lang + "_31_long_text_continued");
		context.runOnClient(mc -> ((TreatiseScreen) mc.gui.screen()).showContents());
		context.waitTick();
		context.takeScreenshot("guide_" + lang + "_32_contents");
		context.setScreen(() -> null);
	}

	/** Les cercles du quotidien, à l'œuvre dans le monde. */
	private static void world(ClientGameTestContext context, TestSingleplayerContext sp) {
		Map<String, Drawing> examples = examples(context);
		context.runOnClient(mc -> {
			RenderShowcase.hideGui(mc, true);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
		});
		sp.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
			AlchemistData data = AlchemistData.NEW.withRank(Rank.ALCHEMIST);
			for (Glyph g : AlchemyRules.of(p.level().registryAccess()).glyphs()) {
				data = data.learn(g.id());
			}
			FmabAttachments.ALCHEMIST.set(p, data);
			p.getInventory().clearContent();
		});

		// Le mur de la première nuit : la terre creusée devant lui.
		ground(context, sp, "grass_block");
		view(context, sp, 0.5, -60, -1.5, 25);
		context.takeScreenshot("guide_world_00_wall_circle");
		check(cast(context, sp, examples.get("treatise.fmab.ch5.wall")), "le mur devrait se lever");
		check(block(sp, CIRCLE.south(2)) != Blocks.AIR.defaultBlockState(), "le mur devrait se dresser à deux pas");
		context.takeScreenshot("guide_world_01_wall");

		// Le pont de glace : la rivière se fige au pied du cercle.
		ground(context, sp, "grass_block");
		sp.getServer().runCommand("fill -6 -62 4 6 -61 7 water");
		view(context, sp, 0.5, -60, -1.5, 30);
		context.takeScreenshot("guide_world_02_river");
		check(cast(context, sp, examples.get("treatise.fmab.everyday.river")), "la rivière devrait geler");
		check(block(sp, CIRCLE.below().south()).is(Blocks.ICE), "l'eau devant le cercle devrait être de la glace");
		context.takeScreenshot("guide_world_03_river_frozen");

		// Le puits de mine : cinq sur cinq, trois de profondeur, le bloc qui porte le cercle compris.
		ground(context, sp, "stone");
		view(context, sp, 0.5, -60, -1.5, 45);
		check(cast(context, sp, examples.get("treatise.fmab.everyday.mine")), "le puits devrait se creuser");
		check(block(sp, CIRCLE.below().east(2).south(2).below(2)).isAir(), "le puits devrait faire cinq de côté et trois de fond");
		check(block(sp, CIRCLE.below()).isAir(), "le bloc qui porte le cercle devrait céder avec le reste");
		check(!block(sp, CIRCLE).is(FmabBlocks.TRANSMUTATION_CIRCLE), "le cercle devrait partir avec son support");
		context.takeScreenshot("guide_world_04_mine_shaft");

		// L'interrupteur de cuivre : le bloc de redstone allume la lampe qui le touche.
		ground(context, sp, "grass_block");
		sp.getServer().runCommand("setblock 1 -60 4 redstone_lamp");
		view(context, sp, 0.5, -60, -1.5, 25);
		check(cast(context, sp, examples.get("treatise.fmab.everyday.switch")), "le cuivre devrait alimenter");
		context.waitTicks(4);
		check(block(sp, CIRCLE.south()).is(Blocks.REDSTONE_BLOCK), "un bloc de redstone devrait paraître devant le cercle");
		check(block(sp, CIRCLE.south().east()).getValue(RedstoneLampBlock.LIT), "la lampe devrait s'allumer");
		context.takeScreenshot("guide_world_05_copper_switch");

		// Le four sans fourneau : le fer brut cuit en lingots, au charbon.
		ground(context, sp, "grass_block");
		view(context, sp, 0.5, -60, 0.5, 50);
		drop(sp, new ItemStack(Items.RAW_IRON, 8));
		drop(sp, new ItemStack(Items.COAL, 1));
		context.waitTicks(10);
		check(cast(context, sp, examples.get("treatise.fmab.everyday.furnace")), "le four devrait cuire");
		int ingots = sp.getServer().computeOnServer(server -> server.overworld()
				.getEntitiesOfClass(ItemEntity.class, new AABB(CIRCLE).inflate(2.5),
						e -> e.getItem().is(Items.IRON_INGOT)).stream().mapToInt(e -> e.getItem().getCount()).sum());
		check(ingots == 8, "huit lingots devraient sortir du four : " + ingots);
		context.takeScreenshot("guide_world_06_furnace");
		context.runOnClient(mc -> RenderShowcase.hideGui(mc, false));
	}

	/** Change la langue du jeu, et attend que ses textes soient rechargés. */
	private static void language(ClientGameTestContext context, String code) {
		java.util.concurrent.CompletableFuture<Void> reloaded = context.computeOnClient(mc -> {
			mc.options.languageCode = code;
			mc.getLanguageManager().setSelected(code);
			return mc.reloadResourcePacks();
		});
		context.waitFor(mc -> reloaded.isDone(), 1200);
		// Le voile du rechargement s'efface en fondu.
		context.waitTicks(60);
	}

	private static String text(ClientGameTestContext context) {
		return context.computeOnClient(mc -> ((TreatiseScreen) mc.gui.screen()).currentText());
	}

	/** Les cercles d'exemple du Traité, par leur texte. */
	private static Map<String, Drawing> examples(ClientGameTestContext context) {
		return context.computeOnClient(mc -> {
			Map<String, Drawing> out = new HashMap<>();
			try (Reader reader = mc.getResourceManager().openAsReader(Fmab.id("treatise/treatise.json"))) {
				for (Treatise.Chapter c : Treatise.parse(JsonParser.parseReader(reader).getAsJsonObject()).chapters()) {
					for (Treatise.Page page : c.pages()) {
						if (page instanceof Treatise.ExamplePage e) {
							out.put(e.textKey(), e.drawing());
						}
					}
				}
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			return out;
		});
	}

	/** Un terrain neuf autour du cercle : trois couches de ce sol, rien au-dessus, ni objet qui traîne. */
	private static void ground(ClientGameTestContext context, TestSingleplayerContext sp, String block) {
		sp.getServer().runCommand("fill -8 -60 -4 8 -52 12 air");
		sp.getServer().runCommand("fill -8 -63 -4 8 -62 12 " + (block.equals("stone") ? "stone" : "dirt"));
		sp.getServer().runCommand("fill -8 -61 -4 8 -61 12 " + block);
		sp.getServer().runCommand("kill @e[type=item]");
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(5);
	}

	/** Le joueur, debout là, regarde vers le sud, à cette inclinaison. */
	private static void view(ClientGameTestContext context, TestSingleplayerContext sp, double x, double y, double z,
			float pitch) {
		sp.getServer().runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f 0 %.1f", x, y, z, pitch));
		sp.getConnection().waitForChunksRender();
		context.waitTicks(10);
	}

	/** Trace le cercle à la craie, face au sud, et pose la paume dessus. */
	private static boolean cast(ClientGameTestContext context, TestSingleplayerContext sp, Drawing drawing) {
		Transmutation.Result result = sp.getServer().computeOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
			var level = p.level();
			BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
					.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
			level.setBlockAndUpdate(CIRCLE, state);
			if (level.getBlockEntity(CIRCLE) instanceof TransmutationCircleBlockEntity be) {
				be.setDrawing(drawing);
			}
			Concentration.set(p, Concentration.max(p));
			return Transmutation.activate(level, CIRCLE, state, drawing, p, CircleSize.NORMAL);
		});
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(30);
		return result == Transmutation.Result.DONE;
	}

	private static BlockState block(TestSingleplayerContext sp, BlockPos pos) {
		return sp.getServer().computeOnServer(server -> server.overworld().getBlockState(pos));
	}

	/** Pose des objets au centre du cercle, immobiles et hors de portée du joueur. */
	private static void drop(TestSingleplayerContext sp, ItemStack stack) {
		sp.getServer().runOnServer(server -> {
			var level = server.overworld();
			ItemEntity entity = new ItemEntity(level, CIRCLE.getX() + 0.5, CIRCLE.getY() + 0.1, CIRCLE.getZ() + 0.5, stack);
			entity.setDeltaMovement(0, 0, 0);
			entity.setPickUpDelay(32767);
			level.addFreshEntity(entity);
		});
	}
}
