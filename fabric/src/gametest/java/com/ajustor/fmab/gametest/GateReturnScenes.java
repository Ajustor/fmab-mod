package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.client.screen.SoulGiftScreen;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.SoulGift;
import com.ajustor.fmab.gate.SoulGifts;
import com.ajustor.fmab.item.PhilosopherStoneItem;
import com.ajustor.fmab.network.SoulGiftAnswerPayload;
import com.ajustor.fmab.network.SoulGiftPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.Transmutation;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * FMAB-63 : rendre sa Porte à qui l'a donnée. Edward (un faux joueur), Alchimiste d'État, a donné sa
 * Porte et se tient dans le cercle ; le joueur a vu la sienne et tient une Pierre. Sa Porte est la seule
 * chose à lui rendre, et seule la Pierre la paie : une Pierre entamée n'y suffit pas, une Pierre pleine
 * (le prix par défaut) la lui rend, une seule fois. Edward transmute de nouveau, mais repart Apprenti,
 * sans rien savoir. Lancer avec {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=gate_return}.
 */
public class GateReturnScenes implements FabricClientGameTest {
	private static final int Y = -60;
	private static final BlockPos CIRCLE = new BlockPos(0, Y, 3);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("gate_return")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("tp @p 0.5 %d 0.5 0 0".formatted(Y));
			sp.getConnection().waitForChunksRender();

			int cost = server(sp, p -> SoulGifts.gateCost());
			check(cost == 100, "par défaut, une Porte coûte une Pierre pleine : " + cost);
			FakePlayer edward = server(sp, p -> {
				FmabAttachments.GATE.set(p, new GateState(1, Set.of(), false, Optional.empty()));
				// Une Pierre entamée d'une âme.
				ItemStack stone = new ItemStack(FmabItems.PHILOSOPHER_STONE);
				stone.setDamageValue(1);
				p.setItemInHand(InteractionHand.OFF_HAND, stone);
				FakePlayer fake = FakePlayer.get(p.level(), new GameProfile(UUID.nameUUIDFromBytes("edward".getBytes()), "Edward"));
				fake.snapTo(CIRCLE.getX() + 0.5, CIRCLE.getY(), CIRCLE.getZ() + 0.5, 180, 0);
				FmabAttachments.GATELESS.set(fake, true);
				FmabAttachments.ALCHEMIST.set(fake, new AlchemistData(Rank.STATE, Set.of("cuivre"), Map.of("metal", 300),
						Set.of(), Map.of()));
				return fake;
			});

			// 1. Edward dans le cercle : sa Porte est la seule chose à lui rendre.
			check(server(sp, p -> SoulGifts.tryRitual(p, CIRCLE)), "un allié sans Porte dans le cercle éveille le rituel");
			context.waitFor(mc -> mc.gui.screen() instanceof SoulGiftScreen, 100);
			check(context.computeOnClient(mc -> ((SoulGiftScreen) mc.gui.screen()).offers()).equals(List.of(SoulGift.GATE_ID)),
					"à qui a donné sa Porte, on rend sa Porte");
			context.takeScreenshot("gate_return_0_choose");

			// 2. La Pierre entamée n'y suffit pas (son bouton est grisé ; un paquet forgé ne passe pas), et sa
			// Porte ne se paie ni d'une âme ni d'une autre Porte.
			for (String payment : List.of("stone", "own_soul", "gate")) {
				server(sp, p -> {
					SoulGifts.answer(p, new SoulGiftAnswerPayload(SoulGiftPayload.CHOOSE, SoulGift.GATE_ID, payment, true));
					return null;
				});
				check(stage(sp) == SoulGift.Stage.CHOOSING, "sa Porte ne se paie pas ainsi : " + payment);
			}
			pressScreen(context, "", "", false);
			check(stage(sp) == null, "renoncer éteint le rituel");

			// 3. Une Pierre pleine : Edward accepte, retrouve sa Porte, et la Pierre paie tout.
			server(sp, p -> {
				p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(FmabItems.PHILOSOPHER_STONE));
				SoulGifts.start(p, CIRCLE, edward);
				return null;
			});
			context.waitFor(mc -> mc.gui.screen() instanceof SoulGiftScreen, 100);
			pressScreen(context, SoulGift.GATE_ID, "stone", true);
			check(stage(sp) == SoulGift.Stage.ASKING, "le choix fait, Edward devrait être sollicité");
			answerAs(sp, edward, true);
			context.waitTicks(2);
			check(server(sp, p -> !Transmutation.gateless(edward)), "Edward devrait retrouver sa Porte");
			check(server(sp, p -> PhilosopherStoneItem.souls(p.getOffhandItem()) == 0), "la Pierre a tout payé");
			AlchemistData after = server(sp, p -> FmabAttachments.ALCHEMIST.getOrCreate(edward));
			check(after.equals(AlchemistData.NEW), "Edward repart de zéro : " + after);
			answerAs(sp, edward, true);
			check(stage(sp) == null, "une seule fois");
			context.takeScreenshot("gate_return_1_restored");

			// 4. Edward n'a plus rien à retrouver : plus de rituel.
			check(!server(sp, p -> SoulGifts.tryRitual(p, CIRCLE)), "rien à rendre, pas de rituel");
		}
	}

	/** Le joueur répond à l'écran ouvert, comme avec ses boutons : le vrai paquet part au serveur. */
	private static void pressScreen(ClientGameTestContext context, String part, String payment, boolean accept) {
		context.runOnClient(mc -> ((SoulGiftScreen) mc.gui.screen()).answer(part, payment, accept));
		context.waitTicks(5);
	}

	private static void answerAs(TestSingleplayerContext sp, ServerPlayer ally, boolean accept) {
		sp.getServer().runOnServer(server -> SoulGifts.answer(ally,
				new SoulGiftAnswerPayload(SoulGiftPayload.PROPOSE, "", "", accept)));
	}

	private static SoulGift.Stage stage(TestSingleplayerContext sp) {
		return server(sp, p -> SoulGifts.pending(p).map(SoulGift::stage).orElse(null));
	}

	private static <T> T server(TestSingleplayerContext sp, Function<ServerPlayer, T> action) {
		return sp.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
	}
}
