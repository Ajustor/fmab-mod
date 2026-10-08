package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.item.ChalkItem;
import com.ajustor.fmab.item.ScreenItem;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public final class FmabItems {
	public static final Item CHALK = register("chalk", ChalkItem::new, new Item.Properties().durability(64));
	public static final Item CIRCLE_NOTEBOOK = register("circle_notebook",
			p -> new ScreenItem(ScreenItem.Kind.NOTEBOOK, p),
			new Item.Properties().stacksTo(1).component(FmabComponents.NOTEBOOK, NotebookContents.EMPTY));
	public static final Item ALCHEMY_TREATISE = register("alchemy_treatise",
			p -> new ScreenItem(ScreenItem.Kind.TREATISE, p),
			new Item.Properties().stacksTo(1));

	public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Fmab.id("alchemy"));

	private FmabItems() {
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
					output.accept(CHALK);
				})
				.build());
	}
}
