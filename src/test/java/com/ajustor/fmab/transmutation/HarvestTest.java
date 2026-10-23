package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.TestBootstrap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** La Moisson ne prend que ce qui est mûr, replante d'une graine, et laisse le pied de ce qui repousse. */
class HarvestTest {
	private static final BlockState SOIL = Blocks.FARMLAND.defaultBlockState();

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void ripeCropsAreReplantedAndGreenOnesLeftAlone() {
		CropBlock wheat = (CropBlock) Blocks.WHEAT;
		assertEquals(Works.Ripeness.REPLANT, Works.ripeness(wheat.getStateForAge(7), SOIL));
		assertEquals(Works.Ripeness.GREEN, Works.ripeness(wheat.getStateForAge(3), SOIL));
		assertEquals(0, Works.replanted(wheat.getStateForAge(7)).getValue(CropBlock.AGE), "replanté jeune");
		BlockState wart = Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE);
		assertEquals(Works.Ripeness.REPLANT, Works.ripeness(wart, Blocks.SOUL_SAND.defaultBlockState()));
	}

	@Test
	void cocoaIsReplantedOnTheSameSideOfItsTrunk() {
		BlockState ripe = Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.AGE, CocoaBlock.MAX_AGE)
				.setValue(CocoaBlock.FACING, Direction.EAST);
		assertEquals(Works.Ripeness.REPLANT, Works.ripeness(ripe, Blocks.AIR.defaultBlockState()));
		BlockState again = Works.replanted(ripe);
		assertEquals(0, again.getValue(CocoaBlock.AGE));
		assertEquals(Direction.EAST, again.getValue(CocoaBlock.FACING));
	}

	@Test
	void caneIsCutAboveItsFootAndMelonsOffTheirStem() {
		BlockState cane = Blocks.SUGAR_CANE.defaultBlockState();
		assertEquals(Works.Ripeness.CUT, Works.ripeness(cane, cane), "au-dessus du pied");
		assertEquals(Works.Ripeness.GREEN, Works.ripeness(cane, Blocks.SAND.defaultBlockState()), "le pied reste");
		assertEquals(Works.Ripeness.CUT, Works.ripeness(Blocks.MELON.defaultBlockState(), Blocks.DIRT.defaultBlockState()));
		assertEquals(Works.Ripeness.GREEN, Works.ripeness(Blocks.MELON_STEM.defaultBlockState(), SOIL), "la tige reste");
	}

	@Test
	void berriesArePickedAndTheBushStays() {
		BlockState bush = Blocks.SWEET_BERRY_BUSH.defaultBlockState()
				.setValue(SweetBerryBushBlock.AGE, SweetBerryBushBlock.MAX_AGE);
		assertEquals(Works.Ripeness.PICK, Works.ripeness(bush, Blocks.GRASS_BLOCK.defaultBlockState()));
		assertEquals(Works.Ripeness.GREEN, Works.ripeness(Blocks.STONE.defaultBlockState(), SOIL), "rien à récolter");
	}
}
