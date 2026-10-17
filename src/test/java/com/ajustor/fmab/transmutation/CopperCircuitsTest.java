package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.TestBootstrap;
import com.ajustor.fmab.alchemy.TestGlyphs;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le cuivre touche aux circuits ; Eau + Air + Fixer donne la brume, le mur de glace n'existe plus. */
class CopperCircuitsTest {
	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void theCutUndoesWhatCarriesTheSignal() {
		assertTrue(CopperCircuits.conducts(Blocks.REDSTONE_WIRE.defaultBlockState()));
		assertTrue(CopperCircuits.conducts(Blocks.REPEATER.defaultBlockState()));
		assertTrue(CopperCircuits.conducts(Blocks.REDSTONE_TORCH.defaultBlockState()));
		assertFalse(CopperCircuits.conducts(Blocks.STONE.defaultBlockState()), "la pierre ne porte rien");
		assertFalse(CopperCircuits.conducts(Blocks.REDSTONE_BLOCK.defaultBlockState()), "la charge n'est pas coupée");
	}

	@Test
	void waterAndAirMakeMistNowAndCopperHasItsCircuits() throws IOException {
		var combos = TestGlyphs.resources().resolve("data/fmab/fmab/combination");
		assertFalse(Files.exists(combos.resolve("ice_wall.json")), "plus de mur de glace");
		assertTrue(Files.readString(combos.resolve("mist.json")).contains("fmab:mist"));
		assertTrue(Files.readString(combos.resolve("copper_power.json")).contains("fmab:redstone_power"));
		assertTrue(Files.readString(combos.resolve("copper_cut.json")).contains("fmab:redstone_cut"));
	}
}
