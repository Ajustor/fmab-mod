package com.ajustor.fmab.item;

import com.ajustor.fmab.TestBootstrap;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les tenues d'Amestris : leurs volumes, et l'armure d'Alphonse plus solide que le fer. */
class OutfitTest {
	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void edwardsCoatHasItsHoodAndTails() {
		assertEquals(EnumSet.of(Outfit.Extra.HOOD, Outfit.Extra.TAILS),
				Outfit.extras(Items.AIR, FmabItems.ALCHEMIST_COAT));
	}

	@Test
	void theGreatcoatFallsToTheAnkles() {
		assertEquals(EnumSet.of(Outfit.Extra.LONG_TAILS),
				Outfit.extras(Items.AIR, FmabItems.OFFICER_GREATCOAT));
	}

	@Test
	void alphonsesArmorHasPauldronsAndPlume() {
		assertEquals(EnumSet.of(Outfit.Extra.PAULDRONS, Outfit.Extra.PLUME),
				Outfit.extras(FmabItems.STEEL_HELMET, FmabItems.STEEL_CHESTPLATE));
	}

	@Test
	void piecesMixAcrossOutfits() {
		Set<Outfit.Extra> mixed = Outfit.extras(FmabItems.OFFICER_CAP,
				FmabItems.ALCHEMIST_COAT);
		assertEquals(EnumSet.of(Outfit.Extra.VISOR, Outfit.Extra.HOOD, Outfit.Extra.TAILS), mixed);
	}

	@Test
	void ordinaryArmorHasNoExtras() {
		assertTrue(Outfit.extras(Items.IRON_HELMET, Items.IRON_CHESTPLATE).isEmpty());
		assertTrue(Outfit.extras(Items.AIR, FmabItems.SOUL_CHESTPLATE).isEmpty());
	}

	@Test
	void alphonsesSteelOutweighsIron() {
		int steel = 0, iron = 0;
		for (ArmorType type : new ArmorType[]{ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS,
				ArmorType.BOOTS}) {
			steel += Outfit.STEEL.material().defense().get(type);
			iron += net.minecraft.world.item.equipment.ArmorMaterials.IRON.defense().get(type);
		}
		assertTrue(steel > iron, "l'acier d'Alphonse (" + steel + ") devrait protéger mieux que le fer (" + iron + ")");
	}
}
