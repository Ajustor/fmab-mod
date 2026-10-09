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
		Path file = FabricLoader.getInstance().getConfigDir().resolve("fmab.json");
		try {
			if (Files.exists(file)) {
				JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
				current = new FmabConfig(json.has("restart_wipes_progress")
						? json.get("restart_wipes_progress").getAsBoolean() : DEFAULT.restartWipesProgress());
			}
			JsonObject out = new JsonObject();
			out.addProperty("restart_wipes_progress", current.restartWipesProgress());
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(out));
		} catch (IOException | RuntimeException e) {
			Fmab.LOGGER.error("Configuration {} illisible : valeurs par défaut", file, e);
			current = DEFAULT;
		}
	}
}
