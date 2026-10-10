package com.ajustor.fmab.item;

import com.ajustor.fmab.data.Tome;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Les tomes d'alchimie du mod. Chacun enseigne un groupe de glyphes ; on les trouve dans le kit de
 * départ, auprès d'Izumi et de l'examinateur, et les bibliothécaires des villages les vendent comme
 * des livres enchantés ({@code data/fmab/villager_trade/librarian}). Les notes de Hohenheim ne se
 * trouvent qu'à Resembool (ou auprès de lui), l'alchimie de l'or que dans des ruines interdites.
 */
public final class Tomes {
	/** Les bases : de quoi lever un mur, une pique ou une plateforme de glace. */
	public static final Tome RUDIMENTS = tome("rudiments", "fmab:terre", "fmab:eau", "fmab:fixer", "fmab:projeter");
	/** La pierre et le métal : décomposer, réparer, le fer et le cuivre. */
	public static final Tome STONE_AND_METAL = tome("stone_and_metal", "fmab:fer", "fmab:cuivre", "fmab:decomposer",
			"fmab:reparer");
	/** Le bois et la fibre : de quoi recomposer coffres, bateaux, lits, arcs… */
	public static final Tome WOOD_AND_FIBER = tome("wood_and_fiber", "fmab:bois", "fmab:fibre");
	/** La matière vivante : les plantes et la chair. */
	public static final Tome LIVING_MATTER = tome("living_matter", "fmab:plante", "fmab:chair");
	/** Le carbone (du charbon au diamant) et les cristaux. */
	public static final Tome CARBON_AND_CRYSTAL = tome("carbon_and_crystal", "fmab:carbone", "fmab:cristal");
	/** Le soufre et la chaux. */
	public static final Tome SULFUR_AND_LIME = tome("sulfur_and_lime", "fmab:soufre", "fmab:chaux");
	/** Les métaux des planètes : l'argent de la Lune, l'étain de Jupiter, le plomb de Saturne, le mercure. */
	public static final Tome PLANETARY_METALS = tome("planetary_metals", "fmab:argent", "fmab:etain", "fmab:plomb",
			"fmab:mercure");
	/** Les métaux qu'on tire de plus loin : zinc, nickel, aluminium et nethérite. */
	public static final Tome DEEP_METALS = tome("deep_metals", "fmab:zinc", "fmab:nickel", "fmab:aluminium",
			"fmab:netherite");
	/** Les formes : recomposer la matière, orienter et intensifier un cercle. */
	public static final Tome FORMS = tome("forms", "fmab:recomposer", "fmab:direction", "fmab:intensite");
	/** Notes sur la combustion : le feu et l'air, l'alchimie de Mustang. */
	public static final Tome FLAME = tome("flame", "fmab:feu", "fmab:air");
	/** Les notes de recherche de Hohenheim, dans sa maison de Resembool. */
	public static final Tome HOHENHEIM = tome("hohenheim", "fmab:humain");
	/**
	 * L'alchimie de l'or, interdite par la loi d'État : on ne la trouve que dans les ruines de Xerxès
	 * (le royaume en vivait) et dans les archives du Laboratoire 5.
	 */
	public static final Tome GOLD = tome("gold", "fmab:or");

	public static final List<Tome> ALL = List.of(RUDIMENTS, STONE_AND_METAL, WOOD_AND_FIBER, LIVING_MATTER,
			CARBON_AND_CRYSTAL, SULFUR_AND_LIME, PLANETARY_METALS, DEEP_METALS, FORMS, FLAME, HOHENHEIM, GOLD);

	private Tomes() {
	}

	private static Tome tome(String name, String... glyphs) {
		return new Tome("tome.fmab." + name, List.of(glyphs));
	}

	public static ItemStack stack(Tome tome) {
		ItemStack stack = new ItemStack(FmabItems.ALCHEMY_TOME);
		stack.set(FmabComponents.TOME, tome);
		return stack;
	}
}
