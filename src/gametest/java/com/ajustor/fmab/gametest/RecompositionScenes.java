package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.ForbiddenCircles;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.SimpleCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.client.screen.DesignScreen;
import com.ajustor.fmab.network.ChooseDesignPayload;
import com.ajustor.fmab.network.RequestDesignsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.InteractionHand;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Transmutation;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Fabriquer un objet par l'alchimie, en jeu : on décompose une pioche pour la comprendre (elle rend
 * sa matière), puis on en recompose une d'après modèle avec du fer et du bois. Sans l'avoir
 * comprise, sans la bonne matière ou avec le mauvais élément, rien ne se fait. Chaque étape est
 * vérifiée et capturée.
 */
public class RecompositionScenes implements FabricClientGameTest {
	private static final BlockPos CIRCLE = new BlockPos(0, -60, 2);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("recomposition")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("tp @p 0.5 -60 -1.0 0 30");
			sp.getConnection().waitForChunksRender();
			server(context, sp, (level, p) -> {
				AlchemistData data = p.getAttachedOrCreate(FmabAttachments.ALCHEMIST).withRank(Rank.ALCHEMIST);
				for (Glyph g : AlchemyRules.of(level.registryAccess()).glyphs()) {
					data = data.learn(g.id());
				}
				p.setAttached(FmabAttachments.ALCHEMIST, data);
				p.getInventory().clearContent();
				return null;
			});
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, false);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});

			// 1. Comprendre : Fer + Décomposer défait une pioche de fer en sa matière.
			drop(context, sp, new ItemStack(Items.IRON_PICKAXE));
			context.takeScreenshot("recomposition_00_pickaxe_on_iron_decompose");
			cast(context, sp, "fmab:fer", "fmab:decomposer");
			check(count(context, sp, Items.IRON_PICKAXE) == 0, "la pioche devrait s'être défaite");
			check(count(context, sp, Items.IRON_INGOT) == 3, "la pioche devrait rendre trois lingots : "
					+ count(context, sp, Items.IRON_INGOT));
			check(count(context, sp, Items.OAK_PLANKS) == 1, "et une planche (deux bâtons)");
			check(server(context, sp, (level, p) -> p.getAttachedOrCreate(FmabAttachments.UNDERSTOOD)
					.contains("minecraft:iron_pickaxe")), "l'alchimiste devrait avoir compris la pioche");
			context.takeScreenshot("recomposition_01_pickaxe_broken_down");

			// 2. Recomposer : la pioche en modèle, la matière rendue autour, Fer + Recomposer.
			drop(context, sp, new ItemStack(Items.IRON_PICKAXE));
			cast(context, sp, "fmab:fer", "fmab:recomposer");
			check(count(context, sp, Items.IRON_PICKAXE) == 2, "le modèle et sa copie : "
					+ count(context, sp, Items.IRON_PICKAXE));
			check(count(context, sp, Items.IRON_INGOT) == 0 && count(context, sp, Items.OAK_PLANKS) == 0,
					"la matière devrait être consommée");
			context.takeScreenshot("recomposition_02_pickaxe_copied");

			// 3. Sans assez de matière, rien ne se fait.
			cast(context, sp, "fmab:fer", "fmab:recomposer");
			check(count(context, sp, Items.IRON_PICKAXE) == 2, "sans fer, pas de copie");
			context.takeScreenshot("recomposition_03_not_enough_matter");
			clear(context, sp);

			// 4. Un objet jamais décomposé ne se copie pas.
			drop(context, sp, new ItemStack(Items.IRON_SHOVEL));
			drop(context, sp, new ItemStack(Items.IRON_INGOT, 2));
			drop(context, sp, new ItemStack(Items.OAK_PLANKS, 2));
			cast(context, sp, "fmab:fer", "fmab:recomposer");
			check(count(context, sp, Items.IRON_SHOVEL) == 1, "une pelle jamais comprise ne se copie pas");
			context.takeScreenshot("recomposition_04_not_understood");
			clear(context, sp);

			// 5. Une pioche de pierre est surtout de terre : le cercle de fer ne la vise pas.
			drop(context, sp, new ItemStack(Items.STONE_PICKAXE));
			cast(context, sp, "fmab:fer", "fmab:decomposer");
			check(count(context, sp, Items.STONE_PICKAXE) == 1, "le fer ne défait pas une pioche de pierre");
			cast(context, sp, "fmab:terre", "fmab:decomposer");
			check(count(context, sp, Items.STONE_PICKAXE) == 0 && count(context, sp, Items.COBBLESTONE) == 3,
					"la terre la défait en trois pierres");
			drop(context, sp, new ItemStack(Items.STONE_PICKAXE));
			cast(context, sp, "fmab:terre", "fmab:recomposer");
			check(count(context, sp, Items.STONE_PICKAXE) == 2, "et la recompose d'après modèle");
			context.takeScreenshot("recomposition_05_stone_pickaxe");
			clear(context, sp);

			// 6. La page de conception : on pense à la pioche de fer.
			context.runOnClient(mc -> ClientPlayNetworking.send(RequestDesignsPayload.INSTANCE));
			context.waitForScreen(DesignScreen.class);
			context.takeScreenshot("recomposition_06_design_page");
			context.runOnClient(mc -> ClientPlayNetworking.send(new ChooseDesignPayload("minecraft:iron_pickaxe")));
			sp.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			context.runOnClient(mc -> ClientPlayNetworking.send(RequestDesignsPayload.INSTANCE));
			context.waitTicks(5);
			context.takeScreenshot("recomposition_07_design_chosen");
			context.setScreen(() -> null);
			check(server(context, sp, (level, p) -> "minecraft:iron_pickaxe".equals(p.getAttached(FmabAttachments.DESIGN))),
					"la conception devrait être la pioche de fer");

			// 7. Sans modèle : le cercle crée l'objet auquel on pense ; un lingot de plus le rend plus solide.
			drop(context, sp, new ItemStack(Items.IRON_INGOT, 4));
			drop(context, sp, new ItemStack(Items.OAK_PLANKS, 1));
			server(context, sp, (level, p) -> {
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FmabItems.CHALK));
				return null;
			});
			cast(context, sp, "fmab:fer", "fmab:recomposer");
			check(count(context, sp, Items.IRON_PICKAXE) == 1, "la pioche pensée devrait être créée");
			check(count(context, sp, Items.IRON_INGOT) == 0, "le lingot en plus devrait entrer dans la pioche");
			int max = server(context, sp, (level, p) -> level.getEntitiesOfClass(ItemEntity.class, around(),
					e -> e.getItem().is(Items.IRON_PICKAXE)).getFirst().getItem().getMaxDamage());
			check(max == 250 + 83, "un tiers de fer en plus, un tiers de durabilité en plus : " + max);
			context.takeScreenshot("recomposition_08_created_from_design_sturdier");
			clear(context, sp);

			// 8. Avec une Pierre philosophale en main : créée de toutes pièces, sans matière.
			server(context, sp, (level, p) -> {
				p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(FmabItems.PHILOSOPHER_STONE));
				return null;
			});
			cast(context, sp, "fmab:fer", "fmab:recomposer");
			check(count(context, sp, Items.IRON_PICKAXE) == 1, "la Pierre crée la pioche sans matière");
			context.takeScreenshot("recomposition_09_from_nothing_with_the_stone");
			clear(context, sp);
			server(context, sp, (level, p) -> {
				p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
				p.removeAttached(FmabAttachments.DESIGN);
				return null;
			});

			// 9. Bois : un coffre (huit planches) se défait en deux bûches et se recompose.
			drop(context, sp, new ItemStack(Items.CHEST));
			cast(context, sp, "fmab:bois", "fmab:decomposer");
			check(count(context, sp, Items.CHEST) == 0 && count(context, sp, Items.OAK_LOG) == 2,
					"le coffre devrait rendre deux bûches : " + count(context, sp, Items.OAK_LOG));
			drop(context, sp, new ItemStack(Items.CHEST));
			cast(context, sp, "fmab:bois", "fmab:recomposer");
			check(count(context, sp, Items.CHEST) == 2 && count(context, sp, Items.OAK_LOG) == 0,
					"le bois devrait recomposer le coffre");
			context.takeScreenshot("recomposition_10_wood_chest");
			clear(context, sp);

			// 10. Fibre : un lit est surtout de laine ; le bois de son cadre vient avec.
			drop(context, sp, new ItemStack(Items.BED.white()));
			cast(context, sp, "fmab:bois", "fmab:decomposer");
			check(count(context, sp, Items.BED.white()) == 1, "le bois ne vise pas un lit, surtout fait de laine");
			cast(context, sp, "fmab:fibre", "fmab:decomposer");
			check(count(context, sp, Items.BED.white()) == 0 && count(context, sp, Items.WOOL.white()) == 3
					&& count(context, sp, Items.OAK_PLANKS) == 3, "le lit devrait rendre trois laines et trois planches");
			drop(context, sp, new ItemStack(Items.BED.white()));
			cast(context, sp, "fmab:fibre", "fmab:recomposer");
			check(count(context, sp, Items.BED.white()) == 2, "la fibre devrait recomposer le lit");
			context.takeScreenshot("recomposition_11_fiber_bed");
			clear(context, sp);

			// 11. Carbone : le diamant se comprend sans se défaire, puis se recompose avec du charbon.
			drop(context, sp, new ItemStack(Items.DIAMOND));
			cast(context, sp, "fmab:carbone", "fmab:decomposer");
			check(count(context, sp, Items.DIAMOND) == 1, "le diamant reste un diamant");
			check(server(context, sp, (level, p) -> p.getAttachedOrCreate(FmabAttachments.UNDERSTOOD)
					.contains("minecraft:diamond")), "l'alchimiste devrait avoir compris le diamant");
			drop(context, sp, new ItemStack(Items.COAL, 64));
			cast(context, sp, "fmab:carbone", "fmab:recomposer");
			check(count(context, sp, Items.DIAMOND) == 2 && count(context, sp, Items.COAL) == 0,
					"64 charbons devraient faire un diamant : " + count(context, sp, Items.DIAMOND) + " diamants, "
							+ count(context, sp, Items.COAL) + " charbons");
			context.takeScreenshot("recomposition_12_carbon_diamond");
			clear(context, sp);

			// 12. Le fer brut est du fer sous une autre forme : le cercle de Fer en fait des lingots.
			drop(context, sp, new ItemStack(Items.RAW_IRON, 3));
			cast(context, sp, "fmab:fer", "fmab:decomposer");
			check(count(context, sp, Items.RAW_IRON) == 0 && count(context, sp, Items.IRON_INGOT) == 3,
					"trois fers bruts devraient rendre trois lingots");
			context.takeScreenshot("recomposition_13_raw_iron_refined");
			clear(context, sp);

			// 13. Une pioche en diamant : surtout du carbone, et le bois de son manche.
			drop(context, sp, new ItemStack(Items.DIAMOND_PICKAXE));
			cast(context, sp, "fmab:carbone", "fmab:decomposer");
			check(count(context, sp, Items.DIAMOND) == 3 && count(context, sp, Items.OAK_PLANKS) == 1,
					"la pioche en diamant devrait rendre trois diamants et une planche");
			context.takeScreenshot("recomposition_14_diamond_pickaxe");
			clear(context, sp);

			// 14. Cristal, Plante, Chair : l'améthyste, le pain, le gâteau…
			drop(context, sp, new ItemStack(Items.SPYGLASS));
			cast(context, sp, "fmab:cuivre", "fmab:decomposer");
			check(count(context, sp, Items.SPYGLASS) == 0, "la longue-vue est surtout de cuivre");
			clear(context, sp);
			drop(context, sp, new ItemStack(Items.BREAD));
			cast(context, sp, "fmab:plante", "fmab:decomposer");
			check(count(context, sp, Items.BREAD) == 0 && count(context, sp, Items.WHEAT) == 3,
					"le pain devrait rendre trois blés");
			context.takeScreenshot("recomposition_15_plant_bread");
			clear(context, sp);

			// 15. Le nouveau cercle de transmutation humaine, vu d'en haut (tracé, pas activé).
			server(context, sp, (level, p) -> {
				BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
						.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
				level.setBlockAndUpdate(CIRCLE, state);
				if (level.getBlockEntity(CIRCLE) instanceof TransmutationCircleBlockEntity be) {
					be.setSize(CircleSize.LARGE);
					be.setDrawing(ForbiddenCircles.HUMAN_TRANSMUTATION);
				}
				return null;
			});
			sp.getServer().runCommand("gamemode spectator @p");
			sp.getServer().runCommand("tp @p 0.5 -57.2 2.5 0 90");
			context.runOnClient(mc -> RenderShowcase.hideGui(mc, true));
			context.waitTicks(60);
			context.takeScreenshot("recomposition_16_human_transmutation_circle");
		}
	}

	@FunctionalInterface
	private interface OnServer<T> {
		T run(ServerLevel level, ServerPlayer player);
	}

	private static <T> T server(ClientGameTestContext context, TestSingleplayerContext sp, OnServer<T> action) {
		T result = sp.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			return action.run(player.level(), player);
		});
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(2);
		return result;
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	/** Pose un objet au centre du cercle, sans qu'on puisse le ramasser. */
	private static void drop(ClientGameTestContext context, TestSingleplayerContext sp, ItemStack stack) {
		server(context, sp, (level, p) -> {
			ItemEntity entity = new ItemEntity(level, CIRCLE.getX() + 0.5, CIRCLE.getY() + 0.1, CIRCLE.getZ() + 0.5, stack);
			entity.setDeltaMovement(0, 0, 0);
			entity.setNeverPickUp();
			level.addFreshEntity(entity);
			return null;
		});
		context.waitTicks(5);
	}

	/** Trace le cercle (élément + action) à la craie et pose la paume dessus. */
	private static void cast(ClientGameTestContext context, TestSingleplayerContext sp, String element, String action) {
		server(context, sp, (level, p) -> {
			AlchemyRules rules = AlchemyRules.of(level.registryAccess());
			Glyph e = rules.glyph(element).orElseThrow();
			Glyph a = rules.glyph(action).orElseThrow();
			Drawing drawing = SimpleCircles.of(e, a, SimpleCircles.sidesFor(e, a));
			BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
					.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
			level.setBlockAndUpdate(CIRCLE, state);
			if (level.getBlockEntity(CIRCLE) instanceof TransmutationCircleBlockEntity be) {
				be.setDrawing(drawing);
			}
			AlchemistData data = p.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
			p.setAttached(FmabAttachments.ALCHEMIST, data.withConcentration(data.maxConcentration()));
			return Transmutation.activate(level, CIRCLE, state, drawing, p, CircleSize.NORMAL);
		});
		context.waitTicks(20);
	}

	private static int count(ClientGameTestContext context, TestSingleplayerContext sp, Item item) {
		return server(context, sp, (level, p) -> level.getEntitiesOfClass(ItemEntity.class, around(),
				e -> e.getItem().is(item)).stream().mapToInt(e -> e.getItem().getCount()).sum());
	}

	private static void clear(ClientGameTestContext context, TestSingleplayerContext sp) {
		server(context, sp, (level, p) -> {
			level.getEntitiesOfClass(ItemEntity.class, around()).forEach(ItemEntity::discard);
			return null;
		});
	}

	private static AABB around() {
		return new AABB(CIRCLE).inflate(2.5, 1.5, 2.5);
	}
}
