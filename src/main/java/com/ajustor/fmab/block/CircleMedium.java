package com.ajustor.fmab.block;

import net.minecraft.util.StringRepresentable;

/**
 * Ce avec quoi le cercle est tracé. La craie s'efface sous la pluie ; la peinture alchimique
 * tient ; la gravure au burin est permanente mais ne prend que sur la pierre et le métal.
 *
 * @param color couleur ARGB du trait
 */
public enum CircleMedium implements StringRepresentable {
	CHALK("chalk", 0xF0EEEAE0),
	PAINT("paint", 0xF0B0322A),
	ENGRAVING("engraving", 0xE0303038);

	private final String name;
	private final int color;

	CircleMedium(String name, int color) {
		this.name = name;
		this.color = color;
	}

	public int color() {
		return color;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
