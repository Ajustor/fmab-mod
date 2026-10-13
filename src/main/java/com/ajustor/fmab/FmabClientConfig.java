package com.ajustor.fmab;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Les réglages du joueur, dans {@code config/fmab-client.json}, modifiables depuis le menu des mods
 * (Mod Menu). Le fichier est créé avec les valeurs par défaut au premier lancement ; une clé absente
 * reprend sa valeur par défaut.
 *
 * @param language  la langue des textes du mod seuls (« fr_fr », « en_us »…), indépendante de celle
 *                  du jeu ; vide : celle du jeu
 * @param circleHud le cercle sélectionné s'affiche en haut à gauche de l'écran
 * @param itemHints les infobulles d'explication des objets clés
 */
public record FmabClientConfig(String language, boolean circleHud, boolean itemHints) {
	public static final FmabClientConfig DEFAULT = new FmabClientConfig("", true, true);
	/** La langue de référence du mod : la seule complète, sous toutes les autres. */
	public static final String BASE_LANGUAGE = "en_us";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static FmabClientConfig current = DEFAULT;

	public FmabClientConfig {
		language = language == null ? "" : language;
	}

	public static FmabClientConfig get() {
		return current;
	}

	public FmabClientConfig withLanguage(String value) {
		return new FmabClientConfig(value, circleHud, itemHints);
	}

	public FmabClientConfig withCircleHud(boolean value) {
		return new FmabClientConfig(language, value, itemHints);
	}

	public FmabClientConfig withItemHints(boolean value) {
		return new FmabClientConfig(language, circleHud, value);
	}

	/**
	 * Les fichiers de langue du mod à poser par-dessus ceux du jeu, dans l'ordre : rien si le mod suit
	 * la langue du jeu ; sinon la langue de référence (les clés qui manqueraient à l'autre), puis celle
	 * choisie.
	 */
	public List<String> overlay() {
		if (language.isEmpty()) {
			return List.of();
		}
		return language.equals(BASE_LANGUAGE) ? List.of(BASE_LANGUAGE) : List.of(BASE_LANGUAGE, language);
	}

	public static FmabClientConfig fromJson(JsonObject json) {
		return new FmabClientConfig(
				json.has("language") ? json.get("language").getAsString() : DEFAULT.language(),
				json.has("circle_hud") ? json.get("circle_hud").getAsBoolean() : DEFAULT.circleHud(),
				json.has("item_hints") ? json.get("item_hints").getAsBoolean() : DEFAULT.itemHints());
	}

	public JsonObject toJson() {
		JsonObject out = new JsonObject();
		out.addProperty("language", language);
		out.addProperty("circle_hud", circleHud);
		out.addProperty("item_hints", itemHints);
		return out;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("fmab-client.json");
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

	/** Retient ces réglages et les écrit dans le fichier. */
	public static void save(FmabClientConfig config) {
		current = config;
		Path file = file();
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(config.toJson()));
		} catch (IOException e) {
			Fmab.LOGGER.error("Impossible d'écrire {}", file, e);
		}
	}
}
