package com.ajustor.fmab.alchemy.exchange;

/**
 * Valeur d'échange d'une matière.
 *
 * @param mass masse en unités entières ; pour les métaux, la pépite vaut 1 et le lingot 9
 */
public record ExchangeValue(Family family, int mass) {
	/**
	 * Échange équivalent : on ne change pas de famille et on ne produit jamais plus de masse qu'on
	 * n'en a donné.
	 */
	public static boolean allows(Family given, int givenMass, Family produced, int producedMass) {
		return given == produced && producedMass <= givenMass;
	}
}
