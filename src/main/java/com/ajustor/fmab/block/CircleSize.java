package com.ajustor.fmab.block;

/**
 * La taille d'un cercle tracé, en blocs de côté. Un grand cercle porte plus loin et frappe plus
 * fort, mais coûte plus de concentration et use davantage la craie ; un cercle d'un bloc se cache
 * facilement.
 *
 * <p>La plus grande, {@link #GREAT}, est celle des cercles géants : seuls des cercles tracés à cette
 * taille, le centre et ses relais, se répondent pour en former un (voir
 * {@link com.ajustor.fmab.transmutation.GreatCircle}).
 */
public enum CircleSize {
	TINY(1, 0.5, 0.5, 1),
	NORMAL(3, 1, 1, 1),
	LARGE(5, 1.5, 1.75, 2),
	HUGE(7, 2, 3, 3),
	/** Sa portée lui vient de ses relais, jusqu'où ils sont tracés : elle frappe et coûte comme un cercle ordinaire. */
	GREAT(9, 1, 1, 4);

	private final int blocks;
	private final double power;
	private final double cost;
	private final int wear;

	CircleSize(int blocks, double power, double cost, int wear) {
		this.blocks = blocks;
		this.power = power;
		this.cost = cost;
		this.wear = wear;
	}

	/** Côté du cercle, en blocs. */
	public int blocks() {
		return blocks;
	}

	/** Multiplie la portée et les dégâts des effets. */
	public double power() {
		return power;
	}

	/** Multiplie la concentration dépensée. */
	public double cost() {
		return cost;
	}

	/** Usure de l'outil qui le trace. */
	public int wear() {
		return wear;
	}

	/** La taille des cercles géants : le centre et ses relais. */
	public boolean great() {
		return this == GREAT;
	}

	public CircleSize next() {
		return values()[(ordinal() + 1) % values().length];
	}

	/** La taille de ce côté, ou la taille normale si elle n'existe pas. */
	public static CircleSize ofBlocks(int blocks) {
		for (CircleSize size : values()) {
			if (size.blocks == blocks) {
				return size;
			}
		}
		return NORMAL;
	}
}
