package com.ajustor.fmab.data;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Le Carnet de cercles de chaque joueur. Il ne s'égare pas : l'alchimiste le garde en tête (touche
 * du carnet), le garde à sa mort, et la craie, la table d'alchimiste et les mains jointes y prennent
 * le cercle sélectionné.
 */
public final class Notebooks {
	/** Ce qu'il advient d'un cercle qu'on recopie dans le carnet. */
	public enum Added {
		ADDED,
		/** Une page porte déjà ce tracé : elle devient la sélection. */
		ALREADY_THERE,
		FULL
	}

	/** Effets des cercles simples recopiés dans le carnet de départ. */
	private static final List<String> STARTER_CIRCLES = List.of("fmab:wall", "fmab:spike", "fmab:ice_platform");

	private Notebooks() {
	}

	public static NotebookContents of(Player player) {
		return player.getAttachedOrElse(FmabAttachments.NOTEBOOK, NotebookContents.EMPTY);
	}

	public static void set(Player player, NotebookContents contents) {
		player.setAttached(FmabAttachments.NOTEBOOK, contents);
	}

	/** Le cercle sélectionné, ou un tracé vide si le carnet l'est. */
	public static Drawing selected(Player player) {
		return of(player).selectedDrawing();
	}

	public static boolean contains(Player player, Drawing drawing) {
		return of(player).indexOf(drawing) >= 0;
	}

	/** Sélectionne la page {@code index}, si elle existe. */
	public static boolean select(Player player, int index) {
		NotebookContents contents = of(player);
		if (index < 0 || index >= contents.pages().size()) {
			return false;
		}
		set(player, contents.select(index));
		return true;
	}

	/**
	 * Recopie un cercle dans une nouvelle page, qui devient la sélection.
	 *
	 * @param name nom de la page ; {@code @clé} pour un nom traduit à l'affichage
	 */
	public static Added add(Player player, String name, Drawing drawing) {
		NotebookContents contents = of(player);
		int known = contents.indexOf(drawing);
		if (known >= 0) {
			set(player, contents.select(known));
			return Added.ALREADY_THERE;
		}
		if (contents.pages().size() >= NotebookContents.MAX_PAGES) {
			return Added.FULL;
		}
		String page = name.length() > NotebookContents.MAX_NAME_LENGTH
				? name.substring(0, NotebookContents.MAX_NAME_LENGTH) : name;
		set(player, contents.withPage(contents.pages().size(), new NotebookContents.Page(page, drawing)));
		return Added.ADDED;
	}

	/** Le message d'un carnet sans cercle sélectionné, avec la touche qui l'ouvre. */
	public static Component noCircle() {
		return Component.translatable("item.fmab.chalk.no_circle", Component.keybind("key.fmab.notebook"));
	}

	/** Le nom d'une page tel qu'on l'affiche : {@code @clé} est traduite dans la langue du joueur. */
	public static Component pageName(NotebookContents.Page page) {
		return page.name().startsWith("@") ? Component.translatable(page.name().substring(1))
				: Component.literal(page.name());
	}

	/**
	 * Un joueur sans carnet (nouveau venu, ou monde d'avant la touche du carnet) en reçoit un qui
	 * contient déjà quelques cercles simples, prêts à tracer à la craie.
	 */
	public static void ensure(ServerPlayer player) {
		if (player.hasAttached(FmabAttachments.NOTEBOOK)) {
			return;
		}
		AlchemyRules rules = AlchemyRules.of(player.level().registryAccess());
		List<NotebookContents.Page> pages = new ArrayList<>();
		for (String effect : STARTER_CIRCLES) {
			rules.combinations().stream()
					.filter(c -> c.effect().equals(effect))
					.findFirst()
					.flatMap(rules::simpleCircle)
					.ifPresent(d -> pages.add(new NotebookContents.Page("@effect." + effect.replace(':', '.'), d)));
		}
		set(player, new NotebookContents(pages, 0));
	}
}
