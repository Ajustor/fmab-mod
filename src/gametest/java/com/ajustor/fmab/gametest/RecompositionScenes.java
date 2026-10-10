package com.ajustor.fmab.gametest;

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
