package com.ajustor.fmab.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les données du mod, toutes chargées par le jeu : chaque progrès, recette, table de butin et échange
 * de villageois écrit dans {@code data/fmab/} doit exister une fois le monde ouvert. Le jeu ne fait
 * qu'écrire dans son journal un fichier qu'il ne sait pas lire ; ici, il fait échouer la scène. C'est le
 * garde-fou des formats de données qui changent d'une version de Minecraft à l'autre (voir
 * tools/data/migrate.py). Lancer avec {@code ./gradlew :fabric:<minecraft>:runClientGameTest -PfmabGametest=data}.
 */
public class DataScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("data")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			expect(sp, "advancement", server -> server.getAdvancements().getAllAdvancements().stream()
					.map(holder -> holder.id()).collect(Collectors.toSet()));
			expect(sp, "recipe", server -> server.getRecipeManager().getRecipes().stream()
					.map(holder -> holder.id().identifier()).collect(Collectors.toSet()));
			expect(sp, "loot_table", server -> server.reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE)
					.listElementIds().map(key -> key.identifier()).collect(Collectors.toSet()));
			expect(sp, "villager_trade", server -> server.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE)
					.listElementIds().map(key -> key.identifier()).collect(Collectors.toSet()));
		}
	}

	/** Tous les fichiers de {@code data/fmab/<dossier>} sont chargés. */
	private static void expect(TestSingleplayerContext sp, String folder, Function<MinecraftServer, Set<Identifier>> loaded) {
		Set<Identifier> written = written(folder);
		check(!written.isEmpty(), "le mod devrait avoir des " + folder);
		Set<Identifier> present = sp.getServer().computeOnServer(loaded::apply);
		Set<Identifier> missing = new TreeSet<>(written);
		missing.removeAll(present);
		check(missing.isEmpty(), folder + " écrits mais pas chargés par le jeu : " + missing);
	}

	/** Les identifiants des fichiers JSON de {@code data/fmab/<dossier>}, dans les ressources du mod. */
	private static Set<Identifier> written(String folder) {
		Set<Identifier> ids = new TreeSet<>();
		for (Path root : FabricLoader.getInstance().getModContainer("fmab").orElseThrow().getRootPaths()) {
			Path dir = root.resolve("data").resolve("fmab").resolve(folder);
			if (!Files.isDirectory(dir)) {
				continue;
			}
			try (Stream<Path> files = Files.walk(dir)) {
				files.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
					String rel = dir.relativize(p).toString().replace('\\', '/');
					ids.add(Identifier.fromNamespaceAndPath("fmab", rel.substring(0, rel.length() - ".json".length())));
				});
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return ids;
	}
}
