package com.ajustor.fmab.gametest;

import com.ajustor.fmab.entity.BarryEntity;
import com.ajustor.fmab.entity.Butcher;
import com.ajustor.fmab.entity.HomunculusEntity;
import com.ajustor.fmab.entity.LustEntity;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.stone.Karma;
import com.ajustor.fmab.stone.LivingStone;
import com.ajustor.fmab.stone.Recruitment;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les adversaires qui ont leurs règles à eux, joués pour de vrai : l'offre de Père à une âme noire
 * (Lust vient sans combattre ; accepter ou refuser change la suite), Barry le Boucher (le hachoir
 * levé avant de balayer, le sceau qui tient au premier effondrement). Chaque règle est vérifiée, et
 * capturée dans {@code build/run/clientGameTest/screenshots}.
 */
public class FoeScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("foes")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("gamerule doMobSpawning false");
			sp.getServer().runCommand("difficulty easy");
			sp.getServer().runCommand("tp @p 0 -60 0 0 0");
			sp.getConnection().waitForChunksRender();
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			});

			offerAccepted(context, sp);
			offerRefused(context, sp);
			barry(context, sp);
		}
	}

	private static void onPlayer(ClientGameTestContext context, TestSingleplayerContext sp, Consumer<ServerPlayer> action) {
		sp.getServer().runOnServer(server -> action.accept(server.getPlayerList().getPlayers().getFirst()));
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(2);
	}

	private static List<HomunculusEntity> homunculi(ServerPlayer p) {
		return p.level().getEntitiesOfClass(HomunculusEntity.class, p.getBoundingBox().inflate(64));
	}

	/** Une âme noire reçoit la visite de Lust : une offre, pas un combat. */
	private static LustEntity[] envoy(ClientGameTestContext context, TestSingleplayerContext sp) {
		LustEntity[] lust = new LustEntity[1];
		onPlayer(context, sp, p -> {
			homunculi(p).forEach(HomunculusEntity::discard);
			p.removeAttached(FmabAttachments.RECRUITMENT);
			p.setHealth(p.getMaxHealth());
			Karma.set(p, -80);
			LivingStone.visit(p);
			List<HomunculusEntity> came = homunculi(p);
			check(came.size() == 1 && came.getFirst() instanceof LustEntity,
					"une âme noire qui n'a pas répondu devrait recevoir Lust seule, pas " + came);
			lust[0] = (LustEntity) came.getFirst();
			check(lust[0].envoy() && lust[0].getTarget() == null, "Lust devrait venir porter l'offre, sans viser le joueur");
			lust[0].snapTo(p.getX() - 2.5, p.getY(), p.getZ() + 4, 150, 0);
		});
		context.waitTicks(40);
		onPlayer(context, sp, p -> {
			check(lust[0].isAlive() && lust[0].envoy(), "Lust devrait encore attendre la réponse");
			check(lust[0].getTarget() == null, "Lust ne devrait pas attaquer avant la réponse");
			check(p.getHealth() == p.getMaxHealth(), "le joueur ne devrait pas avoir été blessé avant de répondre");
		});
		return lust;
	}

	/** Oui : un noyau en acompte, Lust s'en va, et plus aucune visite ensuite. */
	private static void offerAccepted(ClientGameTestContext context, TestSingleplayerContext sp) {
		LustEntity[] lust = envoy(context, sp);
		context.takeScreenshot("foes_00_lust_offer");
		onPlayer(context, sp, p -> {
			p.getInventory().clearContent();
			p.interactOn(lust[0], InteractionHand.MAIN_HAND, Vec3.ZERO);
			check(Recruitment.ACCEPTED.equals(p.getAttached(FmabAttachments.RECRUITMENT)), "l'offre devrait être acceptée");
			check(lust[0].isRemoved(), "Lust devrait s'en aller une fois l'offre acceptée");
			check(p.getInventory().countItem(FmabItems.PHILOSOPHER_STONE_CORE) == 1,
					"l'acompte de Père devrait être un noyau de Pierre");
			check(Karma.of(p) == -80 + Recruitment.ACCEPT_KARMA, "accepter devrait encore faire baisser le karma");
			LivingStone.visit(p);
			check(homunculi(p).isEmpty(), "une recrue ne devrait plus recevoir de visite");
		});
	}

	/** Non : Lust attaque aussitôt, et les visites suivantes sont des traques. */
	private static void offerRefused(ClientGameTestContext context, TestSingleplayerContext sp) {
		LustEntity[] lust = envoy(context, sp);
		onPlayer(context, sp, p -> {
			p.attack(lust[0]);
			check(Recruitment.REFUSED.equals(p.getAttached(FmabAttachments.RECRUITMENT)), "frapper Lust devrait refuser l'offre");
			check(!lust[0].envoy() && lust[0].getTarget() == p, "Lust devrait attaquer le joueur qui a refusé");
			check(Karma.of(p) == -80 + Recruitment.REFUSE_KARMA, "refuser devrait faire remonter le karma");
		});
		context.waitTicks(10);
		context.takeScreenshot("foes_01_lust_refused");
		onPlayer(context, sp, p -> {
			homunculi(p).forEach(HomunculusEntity::discard);
			LivingStone.visit(p);
			List<HomunculusEntity> came = homunculi(p);
			check(came.size() == 1 && came.getFirst().getTarget() == p
					&& !(came.getFirst() instanceof LustEntity l && l.envoy()),
					"après un refus, la visite devrait être une traque hostile");
			came.forEach(HomunculusEntity::discard);
			p.setHealth(p.getMaxHealth());
		});
	}

	/** Barry lève son hachoir avant de frapper ; son sceau tient au premier effondrement. */
	private static void barry(ClientGameTestContext context, TestSingleplayerContext sp) {
		BarryEntity[] barry = new BarryEntity[1];
		onPlayer(context, sp, p -> {
			p.setHealth(p.getMaxHealth());
			barry[0] = FmabEntities.BARRY.create(p.level(), EntitySpawnReason.COMMAND);
			barry[0].snapTo(p.getX() - 2.2, p.getY(), p.getZ() + 3, 145, 0);
			p.level().addFreshEntity(barry[0]);
			barry[0].setTarget(p);
		});
		boolean raised = false;
		for (int t = 0; t < 80 && !raised; t += 2) {
			context.waitTicks(2);
			raised = sp.getServer().computeOnServer(server -> barry[0].windingUp());
		}
		check(raised, "Barry devrait lever son hachoir avant de frapper, son joueur à portée");
		float[] before = new float[1];
		onPlayer(context, sp, p -> before[0] = p.getHealth());
		context.takeScreenshot("foes_02_barry_windup");
		context.waitTicks(Butcher.SWEEP_WINDUP + 4);
		onPlayer(context, sp, p -> {
			check(p.getHealth() < before[0], "le coup de hachoir devrait toucher le joueur resté dans l'arc");
			p.setHealth(p.getMaxHealth());
			barry[0].hurtServer(p.level(), p.damageSources().playerAttack(p), 1000);
			check(barry[0].collapsed() && !barry[0].sealExposed(), "au premier effondrement, le sceau de Barry devrait tenir");
			p.interactOn(barry[0], InteractionHand.MAIN_HAND, Vec3.ZERO);
			check(barry[0].isAlive() && barry[0].collapsed(), "effacer le sceau ne devrait pas marcher au premier effondrement");
		});
		context.takeScreenshot("foes_03_barry_seal_guarded");
		context.waitTicks(Butcher.FIRST_REFORM + 10);
		onPlayer(context, sp, p -> {
			check(!barry[0].collapsed() && barry[0].getHealth() == barry[0].getMaxHealth(),
					"Barry devrait se relever vite, toute sa vie revenue");
			p.setHealth(p.getMaxHealth());
			barry[0].hurtServer(p.level(), p.damageSources().playerAttack(p), 1000);
			check(barry[0].collapsed() && barry[0].sealExposed(), "au second effondrement, le sceau devrait pouvoir s'effacer");
			p.interactOn(barry[0], InteractionHand.MAIN_HAND, Vec3.ZERO);
			check(!barry[0].isAlive(), "le sceau effacé, Barry devrait être vaincu");
		});
	}
}
