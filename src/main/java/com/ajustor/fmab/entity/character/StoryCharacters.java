package com.ajustor.fmab.entity.character;

import com.ajustor.fmab.item.Tomes;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;

import java.util.List;

import static com.ajustor.fmab.entity.character.StoryCharacter.sells;

/**
 * Les personnages de l'histoire qu'on croise en chemin (voir BRIEF.md) : les militaires de Central,
 * les princes de Xing, les Rockbell, les Alchimistes d'État d'Ishval, le tatoueur de Rush Valley et
 * Shou Tucker. Chacun parle selon son rôle dans l'anime, et la plupart vendent ce qui leur ressemble.
 */
public final class StoryCharacters {
	public static final StoryCharacter MUSTANG = new StoryCharacter("mustang", 4, List.of(
			sells(() -> new ItemStack(FmabItems.SPARK_GLOVES), () -> Items.EMERALD, 12),
			sells(() -> Tomes.stack(Tomes.FLAME), () -> Items.EMERALD, 8),
			sells(() -> new ItemStack(Items.BLAZE_POWDER, 4), () -> Items.EMERALD, 2)));
	public static final StoryCharacter HAWKEYE = new StoryCharacter("hawkeye", 3, List.of(
			sells(() -> new ItemStack(FmabItems.PISTOL), () -> Items.EMERALD, 6),
			sells(() -> new ItemStack(FmabItems.RIFLE), () -> Items.EMERALD, 10),
			sells(() -> new ItemStack(FmabItems.CARTRIDGE, 8), () -> Items.EMERALD, 1)));
	public static final StoryCharacter ARMSTRONG = new StoryCharacter("armstrong", 4, List.of(
			sells(() -> new ItemStack(FmabItems.IRON_GAUNTLETS), () -> Items.EMERALD, 10),
			sells(() -> new ItemStack(FmabItems.STEEL_CHESTPLATE), () -> Items.EMERALD, 14),
			sells(() -> new ItemStack(Items.COOKED_BEEF, 6), () -> Items.EMERALD, 1)));
	public static final StoryCharacter HUGHES = new StoryCharacter("hughes", 4, List.of(
			sells(() -> new ItemStack(Items.COMPASS), () -> Items.EMERALD, 3),
			sells(() -> new ItemStack(Items.WRITABLE_BOOK), () -> Items.EMERALD, 2),
			sells(() -> new ItemStack(FmabItems.OFFICER_CAP), () -> Items.EMERALD, 4)));
	public static final StoryCharacter LING = new StoryCharacter("ling", 3, List.of(
			sells(() -> new ItemStack(FmabItems.KUNAI, 6), () -> Items.EMERALD, 1),
			sells(() -> new ItemStack(FmabItems.XING_SWORD), () -> Items.EMERALD, 12),
			sells(() -> new ItemStack(Items.COOKED_PORKCHOP, 4), () -> Items.EMERALD, 1)));
	public static final StoryCharacter LAN_FAN = new StoryCharacter("lan_fan", 3, List.of(
			sells(() -> new ItemStack(FmabItems.THROWING_KNIFE, 4), () -> Items.EMERALD, 1),
			sells(() -> new ItemStack(FmabItems.KUNAI, 8), () -> Items.EMERALD, 2)));
	public static final StoryCharacter ALPHONSE = new StoryCharacter("alphonse", 4, List.of(
			sells(() -> Tomes.stack(Tomes.RUDIMENTS), () -> Items.EMERALD, 4),
			sells(() -> Tomes.stack(Tomes.FORMS), () -> Items.EMERALD, 8),
			sells(() -> new ItemStack(FmabItems.CHALK, 4), () -> Items.EMERALD, 1)));
	public static final StoryCharacter PINAKO = new StoryCharacter("pinako", 3, List.of(
			sells(() -> new ItemStack(FmabItems.IRON_AUTOMAIL_ARM), () -> Items.EMERALD, 10),
			sells(() -> new ItemStack(FmabItems.IRON_AUTOMAIL_LEG), () -> Items.EMERALD, 10),
			sells(() -> new ItemStack(FmabItems.AUTOMAIL_BENCH), () -> Items.EMERALD, 4)));
	public static final StoryCharacter BASQUE_GRAND = new StoryCharacter("basque_grand", 3, List.of(
			sells(() -> new ItemStack(Items.IRON_INGOT, 4), () -> Items.EMERALD, 1),
			sells(() -> new ItemStack(Items.IRON_SWORD), () -> Items.EMERALD, 3)));
	public static final StoryCharacter MCDOUGAL = new StoryCharacter("mcdougal", 3, List.of(
			sells(() -> new ItemStack(Items.PACKED_ICE, 8), () -> Items.EMERALD, 1),
			sells(() -> new ItemStack(Items.BLUE_ICE, 4), () -> Items.EMERALD, 2)));
	public static final StoryCharacter TATTOOIST = new StoryCharacter("tattooist", 3, List.of(
			sells(() -> new ItemStack(FmabItems.ALCHEMICAL_INK), () -> Items.EMERALD, 3),
			sells(() -> new ItemStack(FmabItems.ALCHEMICAL_THREAD), () -> Items.EMERALD, 2),
			sells(() -> new ItemStack(FmabItems.ALCHEMICAL_PAINT), () -> Items.EMERALD, 2)));
	/** Les Ishvaliens revenus dans les ruines : ils échangent le savoir de leur alchimie, et ce qu'ils fabriquent. */
	public static final StoryCharacter ISHVALAN = new StoryCharacter("ishvalan", 4, List.of(
			sells(() -> Tomes.stack(Tomes.LIVING_MATTER), () -> Items.EMERALD, 6),
			sells(() -> Tomes.stack(Tomes.SULFUR_AND_LIME), () -> Items.EMERALD, 6),
			sells(() -> new ItemStack(Items.TERRACOTTA, 8), () -> Items.EMERALD, 1),
			sells(() -> new ItemStack(Items.DYE.pick(DyeColor.RED), 4), () -> Items.EMERALD, 1),
			sells(() -> new ItemStack(Items.EMERALD), () -> Items.BREAD, 6)));
	public static final StoryCharacter TUCKER = new StoryCharacter("tucker", 3, List.of(), Tucker::unmasked);

	public static final List<StoryCharacter> ALL = List.of(MUSTANG, HAWKEYE, ARMSTRONG, HUGHES, LING, LAN_FAN,
			ALPHONSE, PINAKO, BASQUE_GRAND, MCDOUGAL, TATTOOIST, ISHVALAN, TUCKER);

	private StoryCharacters() {
	}
}
