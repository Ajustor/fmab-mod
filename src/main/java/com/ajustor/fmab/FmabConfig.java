package com.ajustor.fmab;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.ajustor.fmab.gate.Restoration;
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
 * @param gateRestorationCost         les âmes de Pierre que coûte de rendre sa Porte à un joueur qui
 *                                    l'a donnée (voir SoulGifts), de 1 à {@link Restoration#STONE_SOULS}
 *                                    (une Pierre n'en tient pas plus)
 */
public record FmabConfig(boolean restartWipesProgress, boolean soulBindingRequiresSacrifice, int gateRestorationCost) {
	/** Par défaut, rendre une Porte coûte une Pierre pleine : plus que tout le reste. */
	public static final int GATE_RESTORATION_COST = Restoration.STONE_SOULS;
	public static final FmabConfig DEFAULT = new FmabConfig(true, true, GATE_RESTORATION_COST);

	public FmabConfig {
		gateRestorationCost = Math.clamp(gateRestorationCost, 1, Restoration.STONE_SOULS);
	}

	public FmabConfig withRestartWipesProgress(boolean value) {
		return new FmabConfig(value, soulBindingRequiresSacrifice, gateRestorationCost);
	}

	public FmabConfig withSoulBindingRequiresSacrifice(boolean value) {
		return new FmabConfig(restartWipesProgress, value, gateRestorationCost);
	}

	public FmabConfig withGateRestorationCost(int value) {
		return new FmabConfig(restartWipesProgress, soulBindingRequiresSacrifice, value);
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
				current = fromJson(JsonParser.parseString(Files.readString(file)).getAsJsonObject());
			}
		} catch (IOException | RuntimeException e) {
			Fmab.LOGGER.error("Configuration {} illisible : valeurs par défaut", file, e);
			current = DEFAULT;
		}
		save(current);
	}

	/** Les réglages d'un fichier : une clé absente reprend sa valeur par défaut. */
	public static FmabConfig fromJson(JsonObject json) {
		return new FmabConfig(
				json.has("restart_wipes_progress")
						? json.get("restart_wipes_progress").getAsBoolean() : DEFAULT.restartWipesProgress(),
				json.has("soul_binding_requires_sacrifice")
						? json.get("soul_binding_requires_sacrifice").getAsBoolean() : DEFAULT.soulBindingRequiresSacrifice(),
				json.has("gate_restoration_cost")
						? json.get("gate_restoration_cost").getAsInt() : DEFAULT.gateRestorationCost());
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
			out.addProperty("gate_restoration_cost", config.gateRestorationCost());
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
