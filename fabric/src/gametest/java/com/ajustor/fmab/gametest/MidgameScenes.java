package com.ajustor.fmab.gametest;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.ForbiddenCircles;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.data.Tattoos;
import com.ajustor.fmab.data.Training;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.tattoo.TattooSlot;
import com.ajustor.fmab.training.Trainings;
import com.ajustor.fmab.training.Trial;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Le milieu de partie se jalonne : une épreuve d'Izumi récompensée, un gant brodé, un tatouage, un
 * premier savoir, la montre d'État ont chacun leur progrès ; et la récompense d'Izumi qui fait passer
 * la barre de maîtrise rend Alchimiste sur-le-champ. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=midgame}.
 */
public class MidgameScenes implements FabricClientGameTest {
	private static final List<String> EXPECTED = List.of("trial_first_wall", "first_node", "gloves_worn", "tattoo",
			"state_watch");

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("midgame")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			// Un Apprenti à cinq points de la barre, qui rend compte de son premier mur à Izumi (+10 en Terre).
			Rank rank = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				FmabAttachments.ALCHEMIST.set(p, AlchemistData.NEW.addMastery("earth", 55));
				FmabAttachments.TRAINING.set(p, Training.NONE.meet().achieve(Trial.FIRST_WALL.id()));
				Trainings.claim(p, Trial.FIRST_WALL);
				ItemStack glove = new ItemStack(FmabItems.CLOTH_GLOVES);
				glove.set(FmabComponents.GLOVE_CIRCLE, ForbiddenCircles.HUMAN_TRANSMUTATION);
				FmabAttachments.GLOVES.set(p, Gloves.NONE.with(false, glove));
				FmabAttachments.TATTOOS.set(p, Tattoos.NONE.with(TattooSlot.FOREARM_LEFT, ForbiddenCircles.HUMAN_TRANSMUTATION));
				p.getInventory().add(new ItemStack(FmabItems.STATE_WATCH));
				return FmabAttachments.ALCHEMIST.getOrCreate(p).rank();
			});
			check(rank == Rank.ALCHEMIST, "la récompense d'Izumi qui passe la barre devrait rendre Alchimiste : " + rank);

			context.waitTicks(60);
			List<String> missing = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				List<String> out = new ArrayList<>();
				for (String name : EXPECTED) {
					var holder = server.getAdvancements().get(Fmab.id("alchemist/" + name));
					if (holder == null || !p.getAdvancements().getOrStartProgress(holder).isDone()) {
						out.add(name);
					}
				}
				return out;
			});
			check(missing.isEmpty(), "progrès de milieu de partie non obtenus : " + missing);
		}
	}
}
