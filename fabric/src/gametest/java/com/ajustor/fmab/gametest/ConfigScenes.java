package com.ajustor.fmab.gametest;

import com.ajustor.fmab.FmabClientConfig;
import com.ajustor.fmab.client.ItemHints;
import com.ajustor.fmab.client.config.ConfigScreen;
import com.ajustor.fmab.client.config.ModLanguage;
import com.ajustor.fmab.registry.FmabItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les réglages du mod : l'icône déclarée, l'écran du menu des mods, et la langue du mod choisie à part
 * de celle du jeu (le jeu reste en anglais, le mod passe en français, puis revient).
 * Lancer avec {@code ./gradlew runClientGameTest -PfmabGametest=config}.
 */
public class ConfigScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("config")) {
			return;
		}
		// L'icône des listes de mods.
		check(FabricLoader.getInstance().getModContainer("fmab").flatMap(mod -> mod.getMetadata().getIconPath(128)
						.flatMap(mod::findPath)).filter(Files::exists).isPresent(), "le mod devrait avoir une icône");

		check(context.computeOnClient(mc -> mc.getLanguageManager().getSelected()).equals("en_us"),
				"le jeu de test devrait être en anglais");
		check(context.computeOnClient(mc -> ModLanguage.available(mc.getResourceManager())).containsAll(List.of("en_us", "fr_fr")),
				"le mod devrait proposer l'anglais et le français");
		check(context.computeOnClient(mc -> I18n.get("config.fmab.title")).equals("FMAB — Settings"),
				"le mod devrait suivre la langue du jeu par défaut");

		// Le mod en français, le jeu en anglais.
		context.runOnClient(mc -> ModLanguage.apply("fr_fr"));
		check(context.computeOnClient(mc -> I18n.get("config.fmab.title")).equals("FMAB — Réglages"),
				"les textes du mod devraient passer en français");
		check(context.computeOnClient(mc -> I18n.get("item.fmab.state_watch")).equals(frenchName("item.fmab.state_watch")),
				"les objets du mod aussi");
		check(context.computeOnClient(mc -> I18n.get("gui.done")).equals("Done"), "le jeu devrait rester en anglais");
		check(FmabClientConfig.get().language().equals("fr_fr"), "le choix devrait être retenu");

		context.runOnClient(mc -> mc.gui.setScreen(new ConfigScreen(mc.gui.screen())));
		context.waitTicks(5);
		context.takeScreenshot("config_0_screen_fr");

		// Retour à la langue du jeu.
		context.runOnClient(mc -> {
			mc.gui.setScreen(null);
			ModLanguage.apply("");
		});
		check(context.computeOnClient(mc -> I18n.get("config.fmab.title")).equals("FMAB — Settings"),
				"revenu à la langue du jeu, le mod devrait reparler anglais");

		// Les infobulles d'explication se coupent (les objets n'existent qu'une fois un monde chargé).
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			context.runOnClient(mc -> FmabClientConfig.save(FmabClientConfig.get().withItemHints(false)));
			check(context.computeOnClient(mc -> hints().isEmpty()), "sans infobulles, la montre ne devrait rien expliquer");
			context.runOnClient(mc -> FmabClientConfig.save(FmabClientConfig.get().withItemHints(true)));
			check(context.computeOnClient(mc -> !hints().isEmpty()), "avec les infobulles, la montre devrait s'expliquer");
		}
		context.runOnClient(mc -> FmabClientConfig.save(FmabClientConfig.DEFAULT));
	}

	private static List<Component> hints() {
		List<Component> lines = new ArrayList<>();
		ItemHints.append(new ItemStack(FmabItems.STATE_WATCH), lines);
		return lines;
	}

	/** Le nom français, lu dans le fichier de langue du mod. */
	private static String frenchName(String key) {
		try (var in = ConfigScenes.class.getResourceAsStream("/assets/fmab/lang/fr_fr.json")) {
			return com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
					.getAsJsonObject().get(key).getAsString();
		} catch (java.io.IOException e) {
			throw new AssertionError(e);
		}
	}
}
