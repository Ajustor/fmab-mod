package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.rules.Combination;
import com.ajustor.fmab.data.ExchangeGroup;
import com.ajustor.fmab.data.FmabCodecs;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Registres de données : tout ce qu'un pack peut étendre sans recompiler le mod. Les glyphes et
 * les combinaisons sont synchronisés vers le client, qui en a besoin pour analyser les cercles dans
 * le carnet ; la valeur d'échange reste côté serveur.
 */
public final class FmabRegistries {
	/** {@code data/<ns>/glyph/<nom>.json} */
	public static final ResourceKey<Registry<Glyph>> GLYPH = ResourceKey.createRegistryKey(Fmab.id("glyph"));
	/** {@code data/<ns>/combination/<nom>.json} */
	public static final ResourceKey<Registry<Combination>> COMBINATION =
			ResourceKey.createRegistryKey(Fmab.id("combination"));
	/** {@code data/<ns>/exchange/<nom>.json} */
	public static final ResourceKey<Registry<ExchangeGroup>> EXCHANGE = ResourceKey.createRegistryKey(Fmab.id("exchange"));

	private FmabRegistries() {
	}

	public static void register() {
		DynamicRegistries.registerSynced(GLYPH, FmabCodecs.GLYPH);
		DynamicRegistries.registerSynced(COMBINATION, FmabCodecs.COMBINATION);
		DynamicRegistries.register(EXCHANGE, ExchangeGroup.CODEC);
	}
}
