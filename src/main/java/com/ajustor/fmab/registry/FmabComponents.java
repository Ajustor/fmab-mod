package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.data.FmabCodecs;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.data.Tome;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;

public final class FmabComponents {
	public static final DataComponentType<NotebookContents> NOTEBOOK = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Fmab.id("notebook"),
			DataComponentType.<NotebookContents>builder()
					.persistent(NotebookContents.CODEC)
					.networkSynchronized(NotebookContents.STREAM_CODEC)
					.build());

	/** Cercle brodé ou gravé sur un gant. */
	public static final DataComponentType<Drawing> GLOVE_CIRCLE = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Fmab.id("glove_circle"),
			DataComponentType.<Drawing>builder()
					.persistent(FmabCodecs.DRAWING)
					.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(FmabCodecs.DRAWING))
					.build());

	/** Cercle brodé sur un vêtement : il agit tant qu'on le porte. */
	public static final DataComponentType<Drawing> EMBROIDERY = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Fmab.id("embroidery"),
			DataComponentType.<Drawing>builder()
					.persistent(FmabCodecs.DRAWING)
					.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(FmabCodecs.DRAWING))
					.build());

	/** Heure de jeu (ticks) à laquelle une arme transmutée se défait. */
	public static final DataComponentType<Long> EXPIRES = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Fmab.id("expires"),
			DataComponentType.<Long>builder()
					.persistent(Codec.LONG)
					.networkSynchronized(ByteBufCodecs.VAR_LONG)
					.build());

	/** La matière qui revient quand l'arme se défait : rien ne se perd. */
	public static final DataComponentType<ItemStack> REMAINS = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Fmab.id("remains"),
			DataComponentType.<ItemStack>builder()
					.persistent(ItemStack.CODEC)
					.networkSynchronized(ItemStack.STREAM_CODEC)
					.build());

	/** Les glyphes qu'enseigne un tome d'alchimie. */
	public static final DataComponentType<Tome> TOME = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Fmab.id("tome"),
			DataComponentType.<Tome>builder()
					.persistent(Tome.CODEC)
					.networkSynchronized(Tome.STREAM_CODEC)
					.build());

	private FmabComponents() {
	}

	public static void register() {
	}
}
