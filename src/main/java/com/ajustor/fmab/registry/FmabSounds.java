package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Les sons du mod ({@code assets/fmab/sounds.json}). Tous viennent de banques libres de droits (CC0) :
 * Kenney (Sci-Fi Sounds, Impact Sounds, RPG Audio) et « gunshots » de kurt sur OpenGameArt, retouchés
 * (hauteur, découpe, mixage). Voir {@code assets/fmab/sounds/credits.txt}.
 */
public final class FmabSounds {
	public static final SoundEvent CLAP = register("clap");
	public static final SoundEvent TRANSMUTE = register("transmute");
	public static final SoundEvent REBOUND = register("rebound");
	public static final SoundEvent GATE_OPEN = register("gate_open");
	public static final SoundEvent GATE_HANDS = register("gate_hands");
	public static final SoundEvent GATE_KNOWLEDGE = register("gate_knowledge");
	public static final SoundEvent PISTOL = register("pistol");
	public static final SoundEvent RIFLE = register("rifle");
	public static final SoundEvent THROW = register("throw");
	public static final SoundEvent SEAL_ERASE = register("seal_erase");
	public static final SoundEvent ARMOR_COLLAPSE = register("armor_collapse");
	public static final SoundEvent FATHER_SUN = register("father_sun");
	public static final SoundEvent DECOMPOSE = register("decompose");
	public static final SoundEvent MINE_ARM = register("mine_arm");
	public static final SoundEvent ALKAHESTRY = register("alkahestry");

	private FmabSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = Fmab.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void register() {
		// Le chargement de la classe suffit.
	}
}
