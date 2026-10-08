package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.NotebookContents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class FmabComponents {
	public static final DataComponentType<NotebookContents> NOTEBOOK = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Fmab.id("notebook"),
			DataComponentType.<NotebookContents>builder()
					.persistent(NotebookContents.CODEC)
					.networkSynchronized(NotebookContents.STREAM_CODEC)
					.build());

	private FmabComponents() {
	}

	public static void register() {
	}
}
