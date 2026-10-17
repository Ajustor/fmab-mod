package com.ajustor.fmab.platform.data;

import net.minecraft.resources.Identifier;

/** Le port des données d'entité : la plateforme sait les stocker, les sauver et les synchroniser. */
public interface DataSlots {
	<T> DataSlot<T> register(Identifier id, DataSlot.Spec<T> spec);
}
