package com.ajustor.fmab.gametest;

import com.ajustor.fmab.client.screen.SoulOfferScreen;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.SoulOffer;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.SoulArmor;
import com.ajustor.fmab.gate.SoulOffering;
import com.ajustor.fmab.network.SoulOfferAnswerPayload;
import com.ajustor.fmab.network.SoulOfferPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Le geste d'Edward, joué pour de vrai : le joueur a déjà vu sa Porte, une armure attend sur le
 * cercle, deux âmes errent devant la Porte (Nina, la plus ancienne, puis Al, deux faux joueurs). Le
 * joueur choisit son bras droit à l'écran ; Nina refuse, Al accepte ; le joueur renonce une première
 * fois (rien n'est pris), puis recommence et sacrifie : il perd son bras, Al habite l'armure. Une
 * seconde confirmation ne paie rien de plus. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=soul_offering}.
 */
public class SoulOfferingScenes implements FabricClientGameTest {
	private static final int Y = -60;
	private static final BlockPos CIRCLE = new BlockPos(0, Y, 3);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("soul_offering")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("tp @p 0.5 %d 0.5 0 0".formatted(Y));
			sp.getConnection().waitForChunksRender();

			// Prérequis : sans Porte vue, ou sans armure sur le cercle, pas de rituel.
			check(!server(sp, p -> SoulOffering.tryRitual(p, CIRCLE)), "sans Porte vue ni armure, pas de rituel");
			server(sp, p -> {
				FmabAttachments.GATE.set(p, new GateState(1, Set.of(), false, Optional.empty()));
				return null;
			});
			check(!server(sp, p -> SoulOffering.tryRitual(p, CIRCLE)), "sans armure sur le cercle, pas de rituel");
			standArmor(sp);

			// Deux âmes errantes : Nina attend depuis plus longtemps qu'Al.
			FakePlayer nina = soul(sp, "Nina");
			FakePlayer al = soul(sp, "Al");

			// 1. Le joueur choisit ce qu'il offre, à l'écran.
			choose(context, sp, nina, al);
			context.takeScreenshot("soul_offering_0_choose");
			pressScreen(context, "right_arm", true);
			check(stage(sp) == SoulOffer.Stage.ASKING, "le choix fait, une âme devrait être sollicitée");
			check(server(sp, p -> SoulOffering.pending(p).flatMap(SoulOffer::recipient)).equals(Optional.of(nina.getUUID())),
					"la plus ancienne âme devrait être sollicitée d'abord");

			// 2. Nina refuse : on passe à Al, qui accepte.
			answerAs(sp, nina, false);
			check(server(sp, p -> SoulOffering.pending(p).flatMap(SoulOffer::recipient)).equals(Optional.of(al.getUUID())),
					"après un refus, l'âme suivante devrait être sollicitée");
			answerAs(sp, al, true);
			check(stage(sp) == SoulOffer.Stage.CONFIRMING, "Al a accepté : le donneur devrait confirmer");
			context.waitFor(mc -> mc.gui.screen() instanceof SoulOfferScreen, 100);
			context.takeScreenshot("soul_offering_1_confirm");

			// 3. Il renonce : rien n'est pris.
			pressScreen(context, "", false);
			check(stage(sp) == null, "après avoir renoncé, plus de rituel");
			check(!server(sp, p -> FmabAttachments.GATE.getOrCreate(p).lost(BodyPart.RIGHT_ARM)), "renoncer ne coûte rien");
			check(server(sp, p -> FmabAttachments.GATE.getOrCreate(al).adrift()), "Al devrait errer toujours");
			check(armorStands(sp) == 1, "l'armure devrait attendre toujours sur le cercle");

			// 4. Il recommence, et sacrifie son bras droit.
			choose(context, sp, nina, al);
			pressScreen(context, "right_arm", true);
			answerAs(sp, nina, false);
			answerAs(sp, al, true);
			context.waitFor(mc -> mc.gui.screen() instanceof SoulOfferScreen, 100);
			int before = server(sp, p -> FmabAttachments.GATE.getOrCreate(p).openings());
			pressScreen(context, "", true);
			context.waitTicks(5);
			check(server(sp, p -> FmabAttachments.GATE.getOrCreate(p).lost(BodyPart.RIGHT_ARM)), "le donneur devrait perdre son bras droit");
			check(server(sp, p -> FmabAttachments.GATE.getOrCreate(p).openings()) == before + 1, "la Vérité a pris son péage : une ouverture");
			check(server(sp, p -> FmabAttachments.GATE.getOrCreate(al).inArmor()), "Al devrait habiter l'armure");
			check(server(sp, p -> !FmabAttachments.GATE.getOrCreate(nina).inArmor()), "Nina, qui a refusé, devrait errer toujours");
			check(armorStands(sp) == 0, "l'armure a quitté le porte-armure : Al la porte");
			check(server(sp, p -> SoulArmor.sealOf(al.getItemBySlot(EquipmentSlot.CHEST)).equals(Optional.of(al.getUUID()))),
					"Al porte le plastron, scellé à son nom");
			context.takeScreenshot("soul_offering_2_paid");

			// 5. Une seconde confirmation ne paie rien de plus.
			server(sp, p -> {
				SoulOffering.answer(p, new SoulOfferAnswerPayload(SoulOfferPayload.CONFIRM, "", true));
				return null;
			});
			check(server(sp, p -> FmabAttachments.GATE.getOrCreate(p).openings()) == before + 1, "un seul paiement");

			// 6. Ce qui est déjà pris ne s'offre plus.
			standArmor(sp);
			choose(context, sp, nina);
			server(sp, p -> {
				SoulOffering.answer(p, new SoulOfferAnswerPayload(SoulOfferPayload.CHOOSE, "right_arm", true));
				return null;
			});
			check(stage(sp) == SoulOffer.Stage.CHOOSING, "un bras déjà pris ne devrait pas pouvoir être offert");
			pressScreen(context, "", false);
			check(stage(sp) == null, "fermer l'écran devrait tout arrêter, sans rien prendre");

			// Ce que voit l'âme sollicitée (ici montré au joueur, pour la capture).
			context.runOnClient(mc -> mc.gui.setScreen(new SoulOfferScreen(
					new SoulOfferPayload(SoulOfferPayload.PROPOSE, "Edward", "right_arm", List.of()))));
			context.waitTicks(5);
			context.takeScreenshot("soul_offering_3_propose");
			context.runOnClient(mc -> mc.gui.setScreen(null));
		}
	}

	/** Le cercle s'éveille : l'écran du choix s'ouvre chez le joueur. */
	private static void choose(ClientGameTestContext context, TestSingleplayerContext sp, ServerPlayer... souls) {
		server(sp, p -> {
			SoulOffering.start(p, CIRCLE, List.of(souls));
			return null;
		});
		context.waitFor(mc -> mc.gui.screen() instanceof SoulOfferScreen, 100);
	}

	/** Le joueur répond à l'écran ouvert, comme avec ses boutons : le vrai paquet part au serveur. */
	private static void pressScreen(ClientGameTestContext context, String part, boolean accept) {
		context.runOnClient(mc -> ((SoulOfferScreen) mc.gui.screen()).answer(part, accept));
		context.waitTicks(5);
	}

	private static void answerAs(TestSingleplayerContext sp, ServerPlayer soul, boolean accept) {
		sp.getServer().runOnServer(server -> SoulOffering.answer(soul,
				new SoulOfferAnswerPayload(SoulOfferPayload.PROPOSE, "", accept)));
	}

	private static SoulOffer.Stage stage(TestSingleplayerContext sp) {
		return server(sp, p -> SoulOffering.pending(p).map(SoulOffer::stage).orElse(null));
	}

	private static FakePlayer soul(TestSingleplayerContext sp, String name) {
		return server(sp, p -> {
			FakePlayer fake = FakePlayer.get(p.level(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
			FmabAttachments.GATE.set(fake, new GateState(1, Set.of(BodyPart.BODY), true, Optional.empty()));
			return fake;
		});
	}

	private static void standArmor(TestSingleplayerContext sp) {
		server(sp, p -> {
			ArmorStand stand = EntityTypes.ARMOR_STAND.create(p.level(), EntitySpawnReason.TRIGGERED);
			stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
			stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			stand.snapTo(CIRCLE.getX() + 0.5, CIRCLE.getY(), CIRCLE.getZ() + 0.5, 180, 0);
			p.level().addFreshEntity(stand);
			return null;
		});
	}

	private static int armorStands(TestSingleplayerContext sp) {
		return server(sp, p -> p.level().getEntitiesOfClass(ArmorStand.class, new AABB(CIRCLE).inflate(2)).size());
	}

	private static <T> T server(TestSingleplayerContext sp, Function<ServerPlayer, T> action) {
		return sp.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
	}
}
