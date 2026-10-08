package com.ajustor.fmab.alchemy.circle;

/** Comment l'énergie passe d'un étage à l'étage extérieur suivant. */
public enum LinkKind {
	/** Aucun trait ne relie l'étage au précédent : il ne reçoit rien. */
	NONE,
	/** Trait simple : l'étage agit après le précédent, s'il a réussi, avec la matière qu'il a produite. */
	SERIES,
	/** Trait double : les deux étages agissent indépendamment. */
	PARALLEL,
	/** Trait brisé : l'étage n'agit que si le précédent n'a rien pu transmuter. */
	CONDITIONAL
}
