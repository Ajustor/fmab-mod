package com.ajustor.fmab.arts;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.tattoo.BodyArt;
import com.ajustor.fmab.tattoo.BodyCircles;
import com.ajustor.fmab.tattoo.TattooSlot;
import com.ajustor.fmab.transmutation.AlchemyRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Le motif d'Ishval : le tatouage du bras de Scar, que son frère avait mis au point en unissant
 * l'alchimie d'Amestris et l'alkahestry de Xing. Il ne s'accorde pas, il se tatoue : autour d'un bras
 * ({@link TattooSlot#sleeve}), dans la peau ou gravé dans l'automail qui le remplace.
 *
 * <p>Il a deux moitiés, chacune portée par un cercle du tour du bras :
 * <ul>
 * <li><b>la décomposition</b> (un cercle dont un étage décompose) : la moitié d'Amestris, la seule dont
 * Scar se sert. Main nue, ce bras défait ce qu'il frappe ({@link ScarArm}) ;</li>
 * <li><b>la recomposition</b> (un cercle dont un étage recompose) : la moitié de Xing, qui ne se lit
 * qu'à qui connaît l'alkahestry. Main nue, accroupi, ce bras recrée l'objet auquel on pense à partir de
 * la matière qu'il touche ({@link ArmRecomposition}).</li>
 * </ul>
 *
 * <p>Il faut avoir compris le motif dans les Notes d'Ishval : sans elles, des cercles autour d'un bras
 * ne sont que des cercles. Chaque bras porte son motif et commande la main de son côté.
 */
public final class IshvalArm {
	/** Les deux moitiés du motif. */
	public enum Half {
		DECOMPOSE, RECOMPOSE
	}

	/** Analyses déjà faites, par cercle et par alchimiste : le bras se lit à chaque coup. */
	private static final int CACHE_SIZE = 64;
	private static final Map<CacheKey, Analysis> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<CacheKey, Analysis> eldest) {
			return size() > CACHE_SIZE;
		}
	};

	private record CacheKey(Drawing circle, AlchemistData alchemist) {
	}

	private IshvalArm() {
	}

	/**
	 * Les moitiés que forment ces cercles analysés : un cercle qui marche et dont un étage décompose,
	 * ou recompose. La recomposition ne se lit qu'avec l'alkahestry.
	 */
	public static Set<Half> halves(List<Analysis> circles, boolean alkahestry) {
		Set<Half> out = EnumSet.noneOf(Half.class);
		for (Analysis a : circles) {
			if (a.outcome() != Analysis.Outcome.WORKS) {
				continue;
			}
			for (Analysis.StageEffect e : a.effects()) {
				switch (e.combination().action()) {
					case "decompose" -> out.add(Half.DECOMPOSE);
					case "recompose" -> {
						if (alkahestry) {
							out.add(Half.RECOMPOSE);
						}
					}
					default -> {
					}
				}
			}
		}
		return out;
	}

	/** A-t-il compris le motif dans les Notes d'Ishval ? */
	public static boolean knows(Player player) {
		return Boolean.TRUE.equals(FmabAttachments.ISHVAL_MOTIF.get(player));
	}

	/** Le bras ({@code leftArm}) porte-t-il cette moitié du motif ? Valable des deux côtés. */
	public static boolean has(Player player, boolean leftArm, Half half) {
		return of(player, leftArm).contains(half);
	}

	/** Les moitiés du motif que porte ce bras : rien sans le motif compris, ni bras pour le porter. */
	public static Set<Half> of(Player player, boolean leftArm) {
		if (!knows(player)) {
			return Set.of();
		}
		List<Drawing> circles = BodyCircles.circles(player, TattooSlot.sleeve(leftArm));
		if (circles.isEmpty()) {
			return Set.of();
		}
		AlchemistData me = FmabAttachments.ALCHEMIST.getOrElse(player, AlchemistData.NEW);
		AlchemyRules rules = AlchemyRules.of(player.level().registryAccess());
		List<Analysis> analyses = new ArrayList<>();
		for (Drawing circle : circles) {
			analyses.add(analysis(rules, circle, me));
		}
		return halves(analyses, Boolean.TRUE.equals(FmabAttachments.ALKAHESTRY.get(player)));
	}

	private static Analysis analysis(AlchemyRules rules, Drawing circle, AlchemistData me) {
		synchronized (CACHE) {
			return CACHE.computeIfAbsent(new CacheKey(circle, me), k -> rules.analyze(k.circle(), k.alchemist()));
		}
	}

	/** Le bras de la main principale : celui qui frappe. */
	public static boolean mainArmLeft(Player player) {
		return player.getMainArm() == HumanoidArm.LEFT;
	}

	/** Un bras du joueur porte-t-il la moitié de la décomposition ? (l'étape « le bras de son frère ») */
	public static boolean worn(Player player) {
		return has(player, false, Half.DECOMPOSE) || has(player, true, Half.DECOMPOSE);
	}

	// --- Les cercles du motif ---------------------------------------------------------------------

	/**
	 * Les deux cercles du motif, tels que les notes les donnent : Terre + Décomposer, la moitié
	 * d'Amestris ; Terre + Recomposer, celle de Xing. N'importe quel cercle qui décompose (ou recompose)
	 * fait l'affaire : ceux-ci sont les plus simples.
	 */
	public static List<Drawing> motif(AlchemyRules rules) {
		List<Drawing> out = new ArrayList<>();
		circle(rules, "decompose").ifPresent(out::add);
		circle(rules, "recompose").ifPresent(out::add);
		return out;
	}

	private static Optional<Drawing> circle(AlchemyRules rules, String action) {
		return rules.combinations().stream()
				.filter(c -> c.elements().equals(Set.of("earth")) && c.action().equals(action))
				.findFirst()
				.flatMap(rules::simpleCircle);
	}

	/** Les pages du carnet où les notes recopient le motif. */
	static final List<String> PAGES = List.of("@item.fmab.ishval_tattoo.page.decompose",
			"@item.fmab.ishval_tattoo.page.recompose");

	/** Le joueur comprend le motif : il le sait, et ses deux cercles passent dans son carnet. */
	public static void learn(ServerPlayer player) {
		FmabAttachments.ISHVAL_MOTIF.set(player, true);
		List<Drawing> circles = motif(AlchemyRules.of(player.level().registryAccess()));
		for (int i = 0; i < circles.size() && i < PAGES.size(); i++) {
			Notebooks.add(player, PAGES.get(i), circles.get(i));
		}
	}

	/**
	 * Un joueur qui avait reçu le bras de Scar d'un coup, avant qu'on le tatoue soi-même : il comprend le
	 * motif, et sa moitié de décomposition est tatouée (ou gravée) autour de son bras droit, s'il en a
	 * un et que la place y est.
	 */
	public static void migrate(ServerPlayer player) {
		if (!Boolean.TRUE.equals(FmabAttachments.SCAR_ARM.get(player))) {
			return;
		}
		FmabAttachments.SCAR_ARM.remove(player);
		learn(player);
		TattooSlot arm = TattooSlot.ARM_RIGHT;
		BodyCircles.Surface surface = BodyCircles.surface(player, arm);
		List<Drawing> circles = motif(AlchemyRules.of(player.level().registryAccess()));
		if (circles.isEmpty() || BodyCircles.full(player, arm)
				|| surface != BodyCircles.Surface.SKIN && surface != BodyCircles.Surface.STEEL) {
			return;
		}
		int ink = surface == BodyCircles.Surface.STEEL ? BodyArt.STEEL_INK : BodyArt.SKIN_INK;
		BodyCircles.setArt(player, arm, arm.stamp(BodyCircles.art(player, arm).orElse(BodyArt.EMPTY),
				circles.getFirst(), ink));
	}
}
