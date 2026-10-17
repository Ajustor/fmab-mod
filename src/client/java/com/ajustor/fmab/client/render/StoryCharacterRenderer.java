package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.character.StoryCharacterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import java.util.Map;
import java.util.Set;

/**
 * Les personnages de l'histoire : la silhouette d'un joueur (fine pour les femmes), leur peau
 * ({@code textures/entity/<id>.png}, voir tools/skins/characters.py) et leur tempérament.
 */
public class StoryCharacterRenderer extends FigureRenderer.Simple<StoryCharacterEntity> {
	private static final Set<String> SLIM = Set.of("hawkeye", "lan_fan", "pinako");
	private static final Map<String, Temper> TEMPERS = Map.ofEntries(
			Map.entry("mustang", Temper.OFFICER), Map.entry("hawkeye", Temper.SOLDIER),
			Map.entry("armstrong", Temper.IZUMI), Map.entry("hughes", Temper.OFFICER),
			Map.entry("ling", Temper.MAY), Map.entry("lan_fan", Temper.SOLDIER),
			Map.entry("alphonse", Temper.HOHENHEIM), Map.entry("pinako", Temper.MARCOH),
			Map.entry("basque_grand", Temper.OFFICER), Map.entry("mcdougal", Temper.OFFICER),
			Map.entry("tattooist", Temper.IZUMI), Map.entry("ishvalan", Temper.CORNELLO),
			Map.entry("tucker", Temper.MARCOH));

	public StoryCharacterRenderer(EntityRendererProvider.Context context, String id) {
		super(context, FigureModels.plain(SLIM.contains(id)), Fmab.id("textures/entity/" + id + ".png"),
				TEMPERS.getOrDefault(id, Temper.HOHENHEIM));
	}
}
