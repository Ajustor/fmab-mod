package com.ajustor.fmab.gametest;

import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Tome;
import com.ajustor.fmab.item.Notes;
import com.ajustor.fmab.item.Tomes;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les tomes s'usent : lu, le Manuel des formes enseigne ses glyphes et tombe en poussière ; un tome
 * qui n'apprend rien reste intact. Un alchimiste consigne ensuite deux glyphes qu'il comprend (et pas
 * un troisième qu'il ignore) dans un livre vierge, qui devient des notes à son nom. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=tomes}.
 */
public class TomeScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("tomes")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getConnection().waitForChunksRender();
			String read = onServer(sp, p -> {
				p.getInventory().clearContent();
				FmabAttachments.ALCHEMIST.set(p, AlchemistData.NEW);
				p.setItemInHand(InteractionHand.MAIN_HAND, Tomes.stack(Tomes.FORMS));
				FmabItems.ALCHEMY_TOME.use(p.level(), p, InteractionHand.MAIN_HAND);
				boolean learned = FmabAttachments.ALCHEMIST.getOrCreate(p).known().contains("fmab:direction");
				return "appris=" + learned + " reste=" + !p.getMainHandItem().isEmpty();
			});
			check(read.equals("appris=true reste=false"), "le tome devrait enseigner puis tomber en poussière : " + read);

			boolean kept = onServer(sp, p -> {
				p.setItemInHand(InteractionHand.MAIN_HAND, Tomes.stack(Tomes.FORMS));
				FmabItems.ALCHEMY_TOME.use(p.level(), p, InteractionHand.MAIN_HAND);
				return p.getMainHandItem().is(FmabItems.ALCHEMY_TOME);
			});
			check(kept, "un tome qui n'apprend rien de neuf devrait rester intact");

			String notes = onServer(sp, p -> {
				p.getInventory().clearContent();
				p.getInventory().add(new ItemStack(Items.BOOK, 2));
				Notes.Result result = Notes.write(p, List.of("fmab:direction", "fmab:cibles", "fmab:humain"));
				Tome tome = null;
				for (ItemStack stack : p.getInventory()) {
					if (stack.has(FmabComponents.TOME)) {
						tome = stack.get(FmabComponents.TOME);
					}
				}
				int books = p.getInventory().countItem(Items.BOOK);
				return result + " " + (tome == null ? "aucun" : tome.glyphs() + " " + tome.author().orElse("?")) + " livres=" + books;
			});
			String name = onServer(sp, p -> p.getName().getString());
			check(notes.equals("WRITTEN [fmab:direction, fmab:cibles] " + name + " livres=1"),
					"les notes devraient porter les deux glyphes compris, à son nom, pour un livre : " + notes);

			Notes.Result empty = onServer(sp, p -> {
				p.getInventory().clearContent();
				return Notes.write(p, List.of("fmab:direction"));
			});
			check(empty == Notes.Result.NO_BOOK, "sans livre vierge, pas de notes : " + empty);
			check(onServer(sp, p -> Optional.ofNullable(p.getInventory().getItem(0)).map(ItemStack::isEmpty).orElse(true)),
					"rien ne devrait être écrit sans livre");
		}
	}

	private static <T> T onServer(TestSingleplayerContext sp, Function<ServerPlayer, T> action) {
		return sp.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
	}
}
