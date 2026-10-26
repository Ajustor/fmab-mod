package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.SimpleCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.item.PhilosopherStoneItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.stone.LivingStone;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.Transmutation;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * La Pierre paie à chaque usage qui fait quelque chose : une transmutation réussie, chaque
 * prolongation (une âme sur quatre) ; un essai à vide ne coûte ni âme ni concentration. La Pierre
 * vivante paie même un cercle à un étage. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=stone_cost}.
 */
public class StoneCostScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("stone_cost")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("tp @p 0.5 -60 0.5 0 0");
			sp.getConnection().waitForChunksRender();

			// Un mur, la Pierre en main gauche : une âme, plus une par étage.
			String wall = server(sp, p -> {
				ItemStack stone = holdStone(p);
				Transmutation.Result result = cast(p, new BlockPos(0, -60, 4), "fmab:terre", "fmab:fixer");
				return result + " " + PhilosopherStoneItem.souls(stone);
			});
			check(wall.equals("DONE 98"), "un mur avec la Pierre devrait coûter deux âmes : " + wall);
			check(server(sp, p -> TransmutationLightning.crackles(p.level(), new BlockPos(0, -60, 4), true)
					&& !TransmutationLightning.crackles(p.level(), new BlockPos(0, -60, 4), false)),
					"une Pierre en main : les éclairs de la transmutation sont rouges");

			// Le garder sous la main : la première prolongation paie déjà une âme, si elle fait grandir le
			// mur ; une prolongation qui ne fait rien ne coûte rien.
			String held = server(sp, p -> {
				ItemStack stone = holdStone(p);
				BlockPos circle = new BlockPos(10, -60, 4);
				cast(p, circle, "fmab:terre", "fmab:fixer");
				int before = PhilosopherStoneItem.souls(stone);
				int blocks = solid(p, circle);
				BlockState state = p.level().getBlockState(circle);
				Drawing drawing = ((TransmutationCircleBlockEntity) p.level().getBlockEntity(circle)).drawing();
				boolean prolonged = Transmutation.channel(p.level(), circle, state, drawing, p, CircleSize.NORMAL);
				boolean grew = solid(p, circle) != blocks;
				return prolonged + " " + (before - PhilosopherStoneItem.souls(stone)) + " " + (grew ? 1 : 0);
			});
			check(held.startsWith("true ") && held.charAt(5) == held.charAt(7),
					"prolonger avec la Pierre ne devrait coûter une âme que s'il fait grandir le mur (prolongé, âmes, a grandi) : " + held);

			// Décomposer du fer quand il n'y en a pas : rien ne se fait, rien ne se paie.
			String empty = server(sp, p -> {
				ItemStack stone = holdStone(p);
				Transmutation.Result result = cast(p, new BlockPos(20, -60, 4), "fmab:fer", "fmab:decomposer");
				return result + " " + PhilosopherStoneItem.souls(stone);
			});
			check(empty.equals("NOTHING 100"), "un essai à vide ne devrait coûter aucune âme : " + empty);
			String tired = server(sp, p -> {
				p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
				// Des glyphes compris : sans Pierre, un glyphe mal compris peut faire rebondir le cercle.
				FmabAttachments.ALCHEMIST.set(p, FmabAttachments.ALCHEMIST.getOrCreate(p).learn("fmab:fer").learn("fmab:decomposer"));
				Concentration.set(p, Concentration.max(p));
				Transmutation.Result result = cast(p, new BlockPos(25, -60, 4), "fmab:fer", "fmab:decomposer");
				return result + " " + (Concentration.of(p) == Concentration.max(p));
			});
			check(tired.equals("NOTHING true"), "un essai à vide ne devrait coûter aucune concentration : " + tired);

			// La Pierre vivante paie même un cercle à un étage.
			String living = server(sp, p -> {
				p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
				FmabAttachments.LIVING_STONE.set(p, 50);
				Transmutation.Result result = cast(p, new BlockPos(30, -60, 4), "fmab:terre", "fmab:fixer");
				return result + " " + LivingStone.souls(p);
			});
			check(living.equals("DONE 49"), "un mur coûte une âme à la Pierre vivante : " + living);
			check(server(sp, p -> TransmutationLightning.crackles(p.level(), new BlockPos(30, -60, 4), true)),
					"une Pierre vivante : ses éclairs sont rouges");

			// Sans Pierre, l'alchimie reste bleue.
			String plain = server(sp, p -> {
				FmabAttachments.LIVING_STONE.remove(p);
				Concentration.set(p, Concentration.max(p));
				BlockPos circle = new BlockPos(40, -60, 4);
				Transmutation.Result result = cast(p, circle, "fmab:terre", "fmab:fixer");
				return result + " " + TransmutationLightning.crackles(p.level(), circle, false)
						+ " " + TransmutationLightning.crackles(p.level(), circle, true);
			});
			check(plain.equals("DONE true false"), "sans Pierre, des éclairs bleus : " + plain);
		}
	}

	/** Les blocs pleins autour du cercle : de quoi voir si un mur a grandi. */
	private static int solid(ServerPlayer p, BlockPos circle) {
		int n = 0;
		for (BlockPos at : BlockPos.betweenClosed(circle.offset(-6, 0, -6), circle.offset(6, 8, 6))) {
			if (!p.level().getBlockState(at).isAir()) {
				n++;
			}
		}
		return n;
	}

	private static ItemStack holdStone(ServerPlayer p) {
		ItemStack stone = new ItemStack(FmabItems.PHILOSOPHER_STONE);
		p.setItemInHand(InteractionHand.OFF_HAND, stone);
		return p.getItemInHand(InteractionHand.OFF_HAND);
	}

	/** Trace le cercle à la craie et pose la paume dessus. */
	private static Transmutation.Result cast(ServerPlayer p, BlockPos circle, String element, String action) {
		var level = p.level();
		AlchemyRules rules = AlchemyRules.of(level.registryAccess());
		Glyph e = rules.glyph(element).orElseThrow();
		Glyph a = rules.glyph(action).orElseThrow();
		Drawing drawing = SimpleCircles.of(e, a, SimpleCircles.sidesFor(e, a));
		BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
				.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
		level.setBlockAndUpdate(circle, state);
		if (level.getBlockEntity(circle) instanceof TransmutationCircleBlockEntity be) {
			be.setDrawing(drawing);
		}
		return Transmutation.activate(level, circle, state, drawing, p, CircleSize.NORMAL);
	}

	private static <T> T server(TestSingleplayerContext sp, java.util.function.Function<ServerPlayer, T> action) {
		return sp.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
	}
}
