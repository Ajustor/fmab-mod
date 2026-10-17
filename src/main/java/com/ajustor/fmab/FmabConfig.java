package com.ajustor.fmab;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.ajustor.fmab.platform.Platform;

/**
 * Les réglages du serveur, dans {@code config/fmab.json}. Le fichier est créé avec les valeurs par
 * défaut au premier lancement ; une clé absente reprend sa valeur par défaut.
 *
 * @param restartWipesProgress        repartir de zéro devant la Vérité efface aussi toute la
 *                                    progression d'alchimiste (rang, glyphes, maîtrise, épreuves,
 *                                    tatouages, karma) ; sinon, seul le corps est rendu
 * @param soulBindingRequiresSacrifice lier l'âme d'un autre joueur à une armure coûte un membre
 *                                    (transmutation humaine, voir SoulOffering) ; sinon, le cercle
 *                                    d'âme l'appelle gratuitement
 */
public record FmabConfig(boolean restartWipesProgress, boolean soulBindingRequiresSacrifice) {
	public static final FmabConfig DEFAULT = new FmabConfig(true, true);

	public FmabConfig withRestartWipesProgress(boolean value) {
		return new FmabConfig(value, soulBindingRequiresSacrifice);
	}

	public FmabConfig withSoulBindingRequiresSacrifice(boolean value) {
		return new FmabConfig(restartWipesProgress, value);
	}
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
				current = new FmabConfig(
						json.has("restart_wipes_progress")
								? json.get("restart_wipes_progress").getAsBoolean() : DEFAULT.restartWipesProgress(),
						json.has("soul_binding_requires_sacrifice")
								? json.get("soul_binding_requires_sacrifice").getAsBoolean() : DEFAULT.soulBindingRequiresSacrifice());
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
			out.addProperty("soul_binding_requires_sacrifice", config.soulBindingRequiresSacrifice());
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(out));
		} catch (IOException e) {
			Fmab.LOGGER.error("Impossible d'écrire {}", file, e);
		}
	}

	private static Path file() {
		return Platform.get().configDir().resolve("fmab.json");
	}
}
