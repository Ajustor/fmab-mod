package com.ajustor.fmab.gametest;

import com.ajustor.fmab.client.screen.SoulGiftScreen;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.SoulGift;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.gate.SoulArmor;
import com.ajustor.fmab.gate.SoulGifts;
import com.ajustor.fmab.transmutation.CircleFrame;
import com.ajustor.fmab.transmutation.Transmutation;
import com.ajustor.fmab.item.PhilosopherStoneItem;
import com.ajustor.fmab.network.SoulGiftAnswerPayload;
import com.ajustor.fmab.network.SoulGiftPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Rendre à un allié ce que la Porte lui a pris, joué pour de vrai : le joueur a déjà vu sa Porte et
 * tient une Pierre ; Winry (un faux joueur) a perdu une jambe et un bras et se tient dans le cercle.
 * Une offre refusée ne coûte rien ; avec la Pierre, sa jambe lui revient pour vingt âmes, une seule
 * fois ; puis le joueur donne sa propre âme pour son bras, confirme une seconde fois, et se réveille
 * dans une armure. Enfin Al, une âme dans une armure, se tient dans le cercle : on ne lui rend que son
 * corps, et le joueur paie de sa Porte, comme Edward ; l'alchimie ne lui répond plus. Lancer avec {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=soul_gift}.
 */
public class SoulGiftScenes implements FabricClientGameTest {
	private static final int Y = -60;
	private static final BlockPos CIRCLE = new BlockPos(0, Y, 3);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("soul_gift")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("tp @p 0.5 %d 0.5 0 0".formatted(Y));
			sp.getConnection().waitForChunksRender();

			// Sans Porte vue, pas de rituel.
			check(!server(sp, p -> SoulGifts.tryRitual(p, CIRCLE)), "sans Porte vue, pas de rituel");
			server(sp, p -> {
				FmabAttachments.GATE.set(p, new GateState(1, Set.of(), false, Optional.empty()));
				p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(FmabItems.PHILOSOPHER_STONE));
				return null;
			});
			FakePlayer winry = server(sp, p -> {
				FakePlayer fake = FakePlayer.get(p.level(), new GameProfile(UUID.nameUUIDFromBytes("winry".getBytes()), "Winry"));
				fake.snapTo(CIRCLE.getX() + 0.5, CIRCLE.getY(), CIRCLE.getZ() + 0.5, 180, 0);
				FmabAttachments.GATE.set(fake, GateState.NONE.pay(BodyPart.LEFT_LEG).pay(BodyPart.RIGHT_ARM));
				return fake;
			});
			int souls = server(sp, SoulGiftScenes::stoneSouls);

			// 1. Le joueur choisit : la jambe de Winry, avec la Pierre. Winry refuse : rien n'est pris.
			choose(context, sp, winry);
			context.takeScreenshot("soul_gift_0_choose");
			pressScreen(context, "left_leg", "stone", true);
			check(stage(sp) == SoulGift.Stage.ASKING, "le choix fait, Winry devrait être sollicitée");
			answerAs(sp, winry, false);
			check(stage(sp) == null, "refusé, le rituel s'éteint");
			check(server(sp, SoulGiftScenes::stoneSouls) == souls, "un refus ne coûte rien à la Pierre");
			check(server(sp, p -> FmabAttachments.GATE.getOrCreate(winry).lost(BodyPart.LEFT_LEG)), "Winry n'a rien retrouvé");

			// 2. De nouveau ; Winry accepte : sa jambe lui revient, la Pierre paie vingt âmes, une fois.
			choose(context, sp, winry);
			pressScreen(context, "left_leg", "stone", true);
			answerAs(sp, winry, true);
			context.waitTicks(2);
			check(server(sp, p -> !FmabAttachments.GATE.getOrCreate(winry).lost(BodyPart.LEFT_LEG)), "Winry devrait retrouver sa jambe");
			check(server(sp, SoulGiftScenes::stoneSouls) == souls - 20, "une jambe coûte vingt âmes à la Pierre");
			answerAs(sp, winry, true);
			check(server(sp, SoulGiftScenes::stoneSouls) == souls - 20, "une seule fois");

			// 3. Le joueur donne sa propre âme pour le bras de Winry : il confirme une seconde fois.
			choose(context, sp, winry);
			pressScreen(context, "right_arm", "own_soul", true);
			answerAs(sp, winry, true);
			check(stage(sp) == SoulGift.Stage.CONFIRMING, "avec son âme, le donneur confirme une seconde fois");
			context.waitFor(mc -> mc.gui.screen() instanceof SoulGiftScreen, 100);
			context.takeScreenshot("soul_gift_1_confirm");
			// Renoncer d'abord : rien n'est pris.
			pressScreen(context, "", "", false);
			check(server(sp, p -> FmabAttachments.GATE.getOrCreate(winry).lost(BodyPart.RIGHT_ARM)), "renoncer ne rend rien");
			check(server(sp, p -> !FmabAttachments.GATE.getOrCreate(p).soulBound()), "renoncer ne prend rien");
			// Puis donner son âme pour de bon.
			choose(context, sp, winry);
			pressScreen(context, "right_arm", "own_soul", true);
			answerAs(sp, winry, true);
			context.waitFor(mc -> mc.gui.screen() instanceof SoulGiftScreen, 100);
			pressScreen(context, "", "", true);
			context.waitTicks(10);
			check(server(sp, p -> !FmabAttachments.GATE.getOrCreate(winry).lost(BodyPart.RIGHT_ARM)), "Winry devrait retrouver son bras");
			check(server(sp, p -> FmabAttachments.GATE.getOrCreate(p).inArmor()),
					"le donneur devrait se réveiller dans une armure, comme Alphonse");
			check(server(sp, SoulGiftScenes::stoneSouls) == souls - 20, "son âme, pas celles de la Pierre");
			context.takeScreenshot("soul_gift_2_soul_given");

			// 4. Winry n'a plus rien à retrouver : plus de rituel.
			choose(context, sp, winry, false);
			check(stage(sp) == null, "rien à rendre, pas de rituel");

			// 5. Al, une âme dans une armure (une jambe perdue avant son corps), se tient dans le cercle : on ne
			// lui rend que son corps. Le joueur, déjà une âme, ne peut plus donner la sienne ; il donne sa Porte.
			FakePlayer al = server(sp, p -> {
				FakePlayer fake = FakePlayer.get(p.level(), new GameProfile(UUID.nameUUIDFromBytes("al".getBytes()), "Al"));
				fake.snapTo(CIRCLE.getX() + 0.5, CIRCLE.getY(), CIRCLE.getZ() + 0.5, 180, 0);
				FmabAttachments.GATE.set(fake, GateState.NONE.pay(BodyPart.LEFT_LEG).pay(BodyPart.BODY));
				SoulArmor.bindFirstTime(fake);
				return fake;
			});
			choose(context, sp, al);
			check(context.computeOnClient(mc -> ((SoulGiftScreen) mc.gui.screen()).offers()).equals(List.of("body")),
					"à une âme en armure, on ne rend que son corps");
			context.takeScreenshot("soul_gift_4_body");
			// (Son bouton est grisé ; un paquet forgé ne passe pas davantage.)
			server(sp, p -> {
				SoulGifts.answer(p, new SoulGiftAnswerPayload(SoulGiftPayload.CHOOSE, "body", "own_soul", true));
				return null;
			});
			check(stage(sp) == SoulGift.Stage.CHOOSING, "une âme n'a plus d'âme à donner");
			pressScreen(context, "body", "gate", true);
			answerAs(sp, al, true);
			check(stage(sp) == SoulGift.Stage.CONFIRMING, "donner sa Porte se confirme une seconde fois");
			context.waitFor(mc -> mc.gui.screen() instanceof SoulGiftScreen, 100);
			context.takeScreenshot("soul_gift_5_confirm_gate");
			pressScreen(context, "", "", true);
			context.waitTicks(5);
			check(server(sp, p -> !FmabAttachments.GATE.getOrCreate(al).soulBound()), "Al devrait retrouver son corps");
			check(server(sp, p -> FmabAttachments.GATE.getOrCreate(al).lost(BodyPart.LEFT_LEG)),
					"la jambe perdue avant le corps reste perdue");
			check(server(sp, Transmutation::gateless), "le donneur a donné sa Porte");
			check(server(sp, p -> Transmutation.activate(p.level(), CIRCLE, CircleFrame.forFace(Direction.UP, Direction.NORTH),
					Drawing.EMPTY, p, true, 9) == Transmutation.Result.INERT), "sans Porte, aucune transmutation ne répond");
			check(server(sp, SoulGiftScenes::stoneSouls) == souls - 20, "sa Porte, pas les âmes de la Pierre");
			context.takeScreenshot("soul_gift_6_gate_given");

			// Ce que voit l'allié sollicité (montré ici au joueur, pour la capture).
			context.runOnClient(mc -> mc.gui.setScreen(new SoulGiftScreen(new SoulGiftPayload(SoulGiftPayload.PROPOSE,
					"Edward", "right_arm", "own_soul", List.of(), 0, true, true))));
			context.waitTicks(5);
			context.takeScreenshot("soul_gift_3_propose");
			context.runOnClient(mc -> mc.gui.setScreen(null));
		}
	}

	private static void choose(ClientGameTestContext context, TestSingleplayerContext sp, ServerPlayer ally) {
		choose(context, sp, ally, true);
	}

	/** Le cercle s'éveille : l'écran du choix s'ouvre chez le joueur (s'il y a de quoi rendre). */
	private static void choose(ClientGameTestContext context, TestSingleplayerContext sp, ServerPlayer ally, boolean opens) {
		server(sp, p -> {
			SoulGifts.start(p, CIRCLE, ally);
			return null;
		});
		if (opens) {
			context.waitFor(mc -> mc.gui.screen() instanceof SoulGiftScreen, 100);
		} else {
			context.waitTicks(5);
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

	private static int stoneSouls(ServerPlayer p) {
		return PhilosopherStoneItem.souls(p.getOffhandItem());
	}

	private static <T> T server(TestSingleplayerContext sp, Function<ServerPlayer, T> action) {
		return sp.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
	}
}
