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
 * trouvent qu'à Resembool.
 */
public final class Tomes {
	/** Les bases : de quoi lever un mur, une pique ou une plateforme de glace. */
	public static final Tome RUDIMENTS = tome("rudiments", "fmab:terre", "fmab:eau", "fmab:fixer", "fmab:projeter");
	/** La pierre et le métal : décomposer, réparer, le fer et le cuivre. */
	public static final Tome STONE_AND_METAL = tome("stone_and_metal", "fmab:fer", "fmab:cuivre", "fmab:decomposer",
			"fmab:reparer");
	/** Les formes : recomposer la matière, orienter et intensifier un cercle. */
	public static final Tome FORMS = tome("forms", "fmab:recomposer", "fmab:direction", "fmab:intensite");
	/** Notes sur la combustion : le feu et l'air, l'alchimie de Mustang. */
	public static final Tome FLAME = tome("flame", "fmab:feu", "fmab:air");
	/** Les notes de recherche de Hohenheim, dans sa maison de Resembool. */
	public static final Tome HOHENHEIM = tome("hohenheim", "fmab:humain");

	public static final List<Tome> ALL = List.of(RUDIMENTS, STONE_AND_METAL, FORMS, FLAME, HOHENHEIM);

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
