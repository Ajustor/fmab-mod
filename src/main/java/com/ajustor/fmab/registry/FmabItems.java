package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.gate.SoulArmor;
import com.ajustor.fmab.item.AlchemicalInkItem;
import com.ajustor.fmab.item.AutomailItem;
import com.ajustor.fmab.item.GloveItem;
import com.ajustor.fmab.item.InscriptionItem;
import com.ajustor.fmab.item.PhilosopherStoneItem;
import com.ajustor.fmab.item.ScreenItem;
import com.ajustor.fmab.item.TomeItem;
import com.ajustor.fmab.item.Tomes;
import com.ajustor.fmab.item.TransmutedWeaponItem;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.function.Function;

public final class FmabItems {
	public static final Item CHALK = register("chalk", p -> new InscriptionItem(CircleMedium.CHALK, p),
			new Item.Properties().durability(64));
	public static final Item ALCHEMICAL_PAINT = register("alchemical_paint",
			p -> new InscriptionItem(CircleMedium.PAINT, p), new Item.Properties().durability(32));
	public static final Item ALCHEMIST_CHISEL = register("alchemist_chisel",
			p -> new InscriptionItem(CircleMedium.ENGRAVING, p), new Item.Properties().durability(128));
	public static final Item CIRCLE_NOTEBOOK = register("circle_notebook",
			p -> new ScreenItem(ScreenItem.Kind.NOTEBOOK, p),
			new Item.Properties().stacksTo(1).component(FmabComponents.NOTEBOOK, NotebookContents.EMPTY));
	public static final Item ALCHEMY_TREATISE = register("alchemy_treatise",
			p -> new ScreenItem(ScreenItem.Kind.TREATISE, p),
			new Item.Properties().stacksTo(1));

	/** Tome d'alchimie : enseigne les glyphes de son composant {@link FmabComponents#TOME}. */
	public static final Item ALCHEMY_TOME = register("alchemy_tome", TomeItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	public static final Item ALCHEMICAL_THREAD = register("alchemical_thread", Item::new, new Item.Properties());
	public static final Item ALCHEMICAL_INK = register("alchemical_ink", AlchemicalInkItem::new,
			new Item.Properties().stacksTo(16));
	public static final Item ALCHEMIST_TABLE = register("alchemist_table",
			p -> new BlockItem(FmabBlocks.ALCHEMIST_TABLE, p), new Item.Properties().useBlockDescriptionPrefix());
	public static final Item CLOTH_GLOVES = glove("cloth_gloves", GloveItem.Kind.CLOTH, 64);
	public static final Item LEATHER_GLOVES = glove("leather_gloves", GloveItem.Kind.LEATHER, 128);
	public static final Item SPARK_GLOVES = glove("spark_gloves", GloveItem.Kind.SPARK, 160);
	public static final Item IRON_GAUNTLETS = glove("iron_gauntlets", GloveItem.Kind.GAUNTLET, 256);
	public static final Item STATE_GLOVES = glove("state_gloves", GloveItem.Kind.STATE, 384);

	public static final Item IZUMI_SPAWN_EGG = register("izumi_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(FmabEntities.IZUMI));

	public static final Item STATE_EXAMINER_SPAWN_EGG = register("state_examiner_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(FmabEntities.STATE_EXAMINER));
	/** Montre d'Alchimiste d'État : portée et concentration améliorées pour son titulaire. */
	public static final Item STATE_WATCH = register("state_watch", Item::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

	/** Lance de pierre : les réglages de la lance de pierre vanilla, mais peu de durabilité. */
	public static final Item STONE_LANCE = register("stone_lance", TransmutedWeaponItem::new,
			new Item.Properties().spear(ToolMaterial.STONE, 0.75F, 0.82F, 0.7F, 4.5F, 13.0F, 9.0F, 5.1F, 13.75F, 4.6F)
					.durability(40));
	/** Lame-bras : l'avant-bras transmuté en lame, plus tranchant qu'une épée de fer. */
	public static final Item ARM_BLADE = register("arm_blade", TransmutedWeaponItem::new,
			new Item.Properties().sword(ToolMaterial.IRON, 4.0F, -2.2F).durability(60));

	/**
	 * Le plastron d'âme : il porte le sceau de sang d'une âme qui a perdu son corps. Les autres pièces
	 * de son armure sont des pièces ordinaires.
	 */
	public static final Item SOUL_CHESTPLATE = register("soul_chestplate", Item::new,
			new Item.Properties().humanoidArmor(SoulArmor.MATERIAL, ArmorType.CHESTPLATE));

	public static final Item AUTOMAIL_BENCH = register("automail_bench",
			p -> new BlockItem(FmabBlocks.AUTOMAIL_BENCH, p), new Item.Properties().useBlockDescriptionPrefix());
	public static final Item IRON_AUTOMAIL_ARM = automail("iron_automail_arm", AutomailItem.Model.IRON, true, 400);
	public static final Item IRON_AUTOMAIL_LEG = automail("iron_automail_leg", AutomailItem.Model.IRON, false, 400);
	public static final Item RUSH_VALLEY_AUTOMAIL_ARM = automail("rush_valley_automail_arm",
			AutomailItem.Model.RUSH_VALLEY, true, 260);
	public static final Item RUSH_VALLEY_AUTOMAIL_LEG = automail("rush_valley_automail_leg",
			AutomailItem.Model.RUSH_VALLEY, false, 260);
	public static final Item BRIGGS_AUTOMAIL_ARM = automail("briggs_automail_arm", AutomailItem.Model.BRIGGS, true, 600);
	public static final Item BRIGGS_AUTOMAIL_LEG = automail("briggs_automail_leg", AutomailItem.Model.BRIGGS, false, 600);

	public static final Item WINRY_SPAWN_EGG = register("winry_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(FmabEntities.WINRY));

	/**
	 * Le noyau de Pierre philosophale d'un homonculus détruit. Une ressource de l'endgame : la
	 * Pierre elle-même viendra plus tard.
	 */
	public static final Item PHILOSOPHER_STONE_CORE = register("philosopher_stone_core", Item::new,
			new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
	/** La Pierre philosophale : fondue de quatre noyaux, elle rend son corps à une âme. */
	public static final Item PHILOSOPHER_STONE = register("philosopher_stone", PhilosopherStoneItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	public static final Item CRYSTALLIZED_BLOOD = register("crystallized_blood",
			p -> new BlockItem(FmabBlocks.CRYSTALLIZED_BLOOD, p), new Item.Properties().useBlockDescriptionPrefix());
	public static final Item LUST_SPAWN_EGG = register("lust_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(FmabEntities.LUST));
	public static final Item GLUTTONY_SPAWN_EGG = register("gluttony_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(FmabEntities.GLUTTONY));

	public static final Item ENVY_SPAWN_EGG = register("envy_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(FmabEntities.ENVY));

	public static final Item GREED_SPAWN_EGG = register("greed_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(FmabEntities.GREED));

	public static final Item TRUTH_SPAWN_EGG = register("truth_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(FmabEntities.TRUTH));

	public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Fmab.id("alchemy"));

	private FmabItems() {
	}

	private static Item automail(String name, AutomailItem.Model model, boolean arm, int durability) {
		return register(name, p -> new AutomailItem(model, arm, p), new Item.Properties().durability(durability));
	}

	private static Item glove(String name, GloveItem.Kind kind, int durability) {
		return register(name, p -> new GloveItem(kind, p), new Item.Properties().durability(durability));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Fmab.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void register() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.fmab.alchemy"))
				.icon(() -> new ItemStack(ALCHEMY_TREATISE))
				.displayItems((parameters, output) -> {
					output.accept(ALCHEMY_TREATISE);
					output.accept(CIRCLE_NOTEBOOK);
					Tomes.ALL.forEach(t -> output.accept(Tomes.stack(t)));
					output.accept(CHALK);
					output.accept(ALCHEMICAL_PAINT);
					output.accept(ALCHEMIST_CHISEL);
					output.accept(ALCHEMIST_TABLE);
					output.accept(ALCHEMICAL_THREAD);
					output.accept(ALCHEMICAL_INK);
					output.accept(CLOTH_GLOVES);
					output.accept(LEATHER_GLOVES);
					output.accept(SPARK_GLOVES);
					output.accept(IRON_GAUNTLETS);
					output.accept(STATE_GLOVES);
					output.accept(IZUMI_SPAWN_EGG);
					output.accept(STATE_EXAMINER_SPAWN_EGG);
					output.accept(AUTOMAIL_BENCH);
					output.accept(IRON_AUTOMAIL_ARM);
					output.accept(IRON_AUTOMAIL_LEG);
					output.accept(RUSH_VALLEY_AUTOMAIL_ARM);
					output.accept(RUSH_VALLEY_AUTOMAIL_LEG);
					output.accept(BRIGGS_AUTOMAIL_ARM);
					output.accept(BRIGGS_AUTOMAIL_LEG);
					output.accept(WINRY_SPAWN_EGG);
					output.accept(TRUTH_SPAWN_EGG);
					output.accept(PHILOSOPHER_STONE_CORE);
					output.accept(PHILOSOPHER_STONE);
					output.accept(CRYSTALLIZED_BLOOD);
					output.accept(LUST_SPAWN_EGG);
					output.accept(GLUTTONY_SPAWN_EGG);
					output.accept(ENVY_SPAWN_EGG);
					output.accept(GREED_SPAWN_EGG);
				})
				.build());
	}
}
