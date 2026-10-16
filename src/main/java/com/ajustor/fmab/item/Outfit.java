package com.ajustor.fmab.item;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Les tenues d'Amestris, à porter : le manteau rouge d'alchimiste à la manière d'Edward (et son
 * pantalon, ses bottes), l'armure d'acier à la manière d'Alphonse, l'uniforme bleu d'officier et le
 * long manteau d'officier. Chaque tenue a son matériau ; ses volumes (capuche, pans, épaulières,
 * plumet, visière) se dessinent par-dessus l'armure ordinaire.
 */
public enum Outfit {
	/** Le manteau rouge d'Edward, son pantalon et ses bottes noirs : du cuir, guère plus. */
	ALCHEMIST(new ArmorMaterial(9, Map.of(ArmorType.BOOTS, 1, ArmorType.LEGGINGS, 2, ArmorType.CHESTPLATE, 3,
			ArmorType.HELMET, 1, ArmorType.BODY, 3), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0, 0,
			ItemTags.REPAIRS_LEATHER_ARMOR, asset("alchemist"))),
	/** L'armure d'Alphonse : un acier plus épais que le fer, plus lourd à mouvoir. */
	STEEL(new ArmorMaterial(22, Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 5, ArmorType.CHESTPLATE, 7,
			ArmorType.HELMET, 2, ArmorType.BODY, 6), 9, SoundEvents.ARMOR_EQUIP_IRON, 1, 0.05F,
			ItemTags.REPAIRS_IRON_ARMOR, asset("steel_armor"))),
	/** L'uniforme d'officier : du drap bleu, une protection de cuir. */
	OFFICER(new ArmorMaterial(9, Map.of(ArmorType.BOOTS, 1, ArmorType.LEGGINGS, 2, ArmorType.CHESTPLATE, 3,
			ArmorType.HELMET, 1, ArmorType.BODY, 3), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0, 0,
			ItemTags.REPAIRS_LEATHER_ARMOR, asset("officer"))),
	/** Le long manteau d'officier, à la Mustang : un drap épais, un peu plus que la vareuse. */
	GREATCOAT(new ArmorMaterial(11, Map.of(ArmorType.BOOTS, 1, ArmorType.LEGGINGS, 2, ArmorType.CHESTPLATE, 4,
			ArmorType.HELMET, 1, ArmorType.BODY, 3), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.5F, 0,
			ItemTags.REPAIRS_LEATHER_ARMOR, asset("officer_greatcoat")));

	private final ArmorMaterial material;

	Outfit(ArmorMaterial material) {
		this.material = material;
	}

	public ArmorMaterial material() {
		return material;
	}

	private static ResourceKey<EquipmentAsset> asset(String name) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, Fmab.id(name));
	}

	/** Les volumes qu'une tenue ajoute au corps, au-delà de l'armure peinte. */
	public enum Extra {
		/** La capuche du manteau d'Edward, rabattue dans le dos. */
		HOOD(ALCHEMIST),
		/** Les pans du manteau d'Edward, jusqu'aux genoux. */
		TAILS(ALCHEMIST),
		/** Les pans du manteau d'officier, jusqu'aux chevilles. */
		LONG_TAILS(GREATCOAT),
		/** Les grandes épaulières rondes de l'armure d'Alphonse. */
		PAULDRONS(STEEL),
		/** La pointe du heaume d'Alphonse et son plumet blanc. */
		PLUME(STEEL),
		/** La visière de la casquette d'officier. */
		VISOR(OFFICER);

		private final Outfit outfit;

		Extra(Outfit outfit) {
			this.outfit = outfit;
		}

		public Outfit outfit() {
			return outfit;
		}
	}

	/** Les volumes à dessiner pour ce qui est porté à la tête et sur le torse. */
	public static Set<Extra> extras(Item head, Item chest) {
		Set<Extra> extras = EnumSet.noneOf(Extra.class);
		if (chest == FmabItems.ALCHEMIST_COAT) {
			extras.add(Extra.HOOD);
			extras.add(Extra.TAILS);
		} else if (chest == FmabItems.OFFICER_GREATCOAT) {
			extras.add(Extra.LONG_TAILS);
		} else if (chest == FmabItems.STEEL_CHESTPLATE) {
			extras.add(Extra.PAULDRONS);
		}
		if (head == FmabItems.STEEL_HELMET) {
			extras.add(Extra.PLUME);
		} else if (head == FmabItems.OFFICER_CAP) {
			extras.add(Extra.VISOR);
		}
		return extras;
	}
}
