package com.ajustor.fmab;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Les réglages du serveur, dans {@code config/fmab.json}. Le fichier est créé avec les valeurs par
 * défaut au premier lancement ; une clé absente reprend sa valeur par défaut.
 *
 * @param restartWipesProgress repartir de zéro devant la Vérité efface aussi toute la progression
 *                             d'alchimiste (rang, glyphes, maîtrise, épreuves, tatouages, karma) ;
 *                             sinon, seul le corps est rendu
 */
public record FmabConfig(boolean restartWipesProgress) {
	public static final FmabConfig DEFAULT = new FmabConfig(true);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static FmabConfig current = DEFAULT;

	public static FmabConfig get() {
		return current;
	}

	public static void load() {
		Path file = file();
		try {
			if (Files.exists(file)) {
				JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
				current = new FmabConfig(json.has("restart_wipes_progress")
						? json.get("restart_wipes_progress").getAsBoolean() : DEFAULT.restartWipesProgress());
			}
		} catch (IOException | RuntimeException e) {
			Fmab.LOGGER.error("Configuration {} illisible : valeurs par défaut", file, e);
			current = DEFAULT;
		}
		save(current);
	}

	/**
	 * Retient ces réglages et les écrit dans le fichier : depuis le menu des mods, ils valent pour les
	 * mondes solo ; un serveur dédié garde son propre fichier.
	 */
	public static void save(FmabConfig config) {
		current = config;
		Path file = file();
		try {
			JsonObject out = new JsonObject();
			out.addProperty("restart_wipes_progress", config.restartWipesProgress());
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(out));
		} catch (IOException e) {
			Fmab.LOGGER.error("Impossible d'écrire {}", file, e);
		}
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("fmab.json");
	}
}
