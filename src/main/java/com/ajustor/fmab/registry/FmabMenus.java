package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.gate.BodyMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** Les menus du mod. */
public final class FmabMenus {
	/** La page du corps : automails et gants, ouverte depuis l'inventaire. */
	public static final MenuType<BodyMenu> BODY = Registry.register(BuiltInRegistries.MENU, Fmab.id("body"),
			new MenuType<>(BodyMenu::new, FeatureFlags.VANILLA_SET));

	private FmabMenus() {
	}

	public static void register() {
	}
}
