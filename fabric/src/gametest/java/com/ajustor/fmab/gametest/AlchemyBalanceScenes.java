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
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.Transmutation;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * FMAB-61 : la lame transmutée vaut mieux qu'une épée de forge (affûtée, renforcée), et la lance de
 * pierre ne prend qu'une sorte de bloc (elle ne change plus la terre en deepslate). Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=alchemy_balance}.
 */
public class AlchemyBalanceScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("alchemy_balance")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("tp @p 0.5 -60 0.5 0 0");
			sp.getConnection().waitForChunksRender();
			String blade = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				BlockPos circle = new BlockPos(0, -60, 3);
				ItemEntity iron = new ItemEntity(p.level(), 0.5, -59.9, 3.5, new ItemStack(Items.IRON_INGOT, 4));
				iron.setNeverPickUp();
				p.level().addFreshEntity(iron);
				// Des glyphes compris : un glyphe qu'on ne comprend pas peut faire rebondir le cercle.
				FmabAttachments.ALCHEMIST.set(p, AlchemistData.NEW.learn("fmab:fer").learn("fmab:projeter"));
				Transmutation.Result r = cast(p, circle, "fmab:fer", "fmab:projeter");
				int sharpness = p.level().getEntitiesOfClass(ItemEntity.class, new AABB(circle).inflate(3),
								e -> e.getItem().is(Items.IRON_SWORD)).stream()
						.mapToInt(e -> e.getItem().getEnchantments().getLevel(p.level().registryAccess()
								.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS)))
						.max().orElse(-1);
				return r + " tranchant=" + sharpness;
			});
			check(blade.equals("DONE tranchant=1"), "la lame transmutée devrait être affûtée : " + blade);

			String lance = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				var level = p.level();
				BlockPos circle = new BlockPos(8, -60, 3);
				BlockPos below = circle.below();
				// Autour : deepslate, terre, deepslate, terre (nord, est, sud, ouest).
				level.setBlockAndUpdate(below.north(), Blocks.DEEPSLATE.defaultBlockState());
				level.setBlockAndUpdate(below.east(), Blocks.DIRT.defaultBlockState());
				level.setBlockAndUpdate(below.south(), Blocks.DEEPSLATE.defaultBlockState());
				level.setBlockAndUpdate(below.west(), Blocks.DIRT.defaultBlockState());
				FmabAttachments.ALCHEMIST.set(p, AlchemistData.NEW.withRank(Rank.ALCHEMIST)
						.grant("fmab:earth/stone_lance").learn("fmab:terre").learn("fmab:recomposer"));
				Transmutation.Result r = cast(p, circle, "fmab:terre", "fmab:recomposer");
				return r + " est=" + level.getBlockState(below.east()).is(Blocks.DIRT)
						+ " ouest=" + level.getBlockState(below.west()).is(Blocks.DIRT);
			});
			check(lance.equals("DONE est=true ouest=true"),
					"la lance ne devrait prendre que de la deepslate, et laisser la terre : " + lance);
		}
	}

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
		Concentration.set(p, Concentration.max(p));
		return Transmutation.activate(level, circle, state, drawing, p, CircleSize.NORMAL);
	}
}
