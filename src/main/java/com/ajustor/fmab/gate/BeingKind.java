package com.ajustor.fmab.gate;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;

import java.util.Locale;
import java.util.Optional;

/**
 * Les êtres qu'une transmutation humaine, Pierre en main, peut animer d'une âme : l'alchimiste choisit
 * la forme du corps. Un être humain, ou une bête qui s'attache à celui qui lui a donné la vie.
 */
public enum BeingKind {
	HUMAN(EntityTypes.VILLAGER),
	DOG(EntityTypes.WOLF),
	CAT(EntityTypes.CAT),
	HORSE(EntityTypes.HORSE),
	PARROT(EntityTypes.PARROT);

	private final EntityType<? extends Mob> type;

	BeingKind(EntityType<? extends Mob> type) {
		this.type = type;
	}

	public EntityType<? extends Mob> type() {
		return type;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String translationKey() {
		return "gate.fmab.being.kind." + id();
	}

	public static Optional<BeingKind> byId(String id) {
		for (BeingKind kind : values()) {
			if (kind.id().equals(id)) {
				return Optional.of(kind);
			}
		}
		return Optional.empty();
	}
}
