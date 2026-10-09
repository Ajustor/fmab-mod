package com.ajustor.fmab.gametest;

import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.entity.WheelchairEntity;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.Wheelchairs;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabEntities;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Le fauteuil roulant en jeu : on rampe sans jambes, on s'assied, on roule à la force des bras ; un
 * bloc plein arrête, une dalle se franchit ; on ne saute pas ; un seul bras ne suffit pas, un
 * villageois payé pousse. Chaque règle est vérifiée, et capturée dans
 * {@code build/run/clientGameTest/screenshots}.
 */
public class WheelchairScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("wheelchair")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("difficulty peaceful");
			sp.getConnection().waitForChunksRender();
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			});

			crawl(context, sp);
			WheelchairEntity chair = sitDown(context, sp);
			roll(context, sp, chair);
			ramps(context, sp);
			noJump(context, sp);
			oneArm(context, sp);
			villager(context, sp);
		}
	}

	private static void onPlayer(ClientGameTestContext context, TestSingleplayerContext sp, Consumer<ServerPlayer> action) {
		sp.getServer().runOnServer(server -> action.accept(server.getPlayerList().getPlayers().getFirst()));
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(2);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private static Vec3 clientPosition(ClientGameTestContext context) {
		return context.computeOnClient(mc -> mc.player.position());
	}

	/** Sans jambes, debout c'est fini : on rampe. */
	private static void crawl(ClientGameTestContext context, TestSingleplayerContext sp) {
		sp.getServer().runCommand("tp @p 0 -60 0 0 0");
		onPlayer(context, sp, p -> p.setAttached(FmabAttachments.GATE,
				GateState.NONE.pay(BodyPart.LEFT_LEG).pay(BodyPart.RIGHT_LEG)));
		context.waitTicks(20);
		Pose pose = context.computeOnClient(mc -> mc.player.getPose());
		check(pose == Pose.SWIMMING, "sans jambes, le joueur devrait ramper, pas " + pose);
		context.takeScreenshot("wheelchair_00_crawling");
	}

	/** Un fauteuil devant soi ; on s'y assied. */
	private static WheelchairEntity sitDown(ClientGameTestContext context, TestSingleplayerContext sp) {
		WheelchairEntity chair = sp.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			WheelchairEntity c = FmabEntities.WHEELCHAIR.create(player.level(), EntitySpawnReason.COMMAND);
			c.snapTo(0.5, -60, 3.5, 0, 0);
			player.level().addFreshEntity(c);
			// Un second fauteuil, vide et de profil, pour la photo.
			WheelchairEntity shown = FmabEntities.WHEELCHAIR.create(player.level(), EntitySpawnReason.COMMAND);
			shown.snapTo(-1.2, -60, 3, 120, 0);
			shown.setYHeadRot(120);
			shown.yBodyRot = 120;
			shown.addTag("shown");
			player.level().addFreshEntity(shown);
			return c;
		});
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(5);
		context.takeScreenshot("wheelchair_01_empty");
		sp.getServer().runCommand("kill @e[tag=shown]");
		onPlayer(context, sp, p -> p.startRiding(chair));
		context.waitTicks(5);
		Pose pose = context.computeOnClient(mc -> mc.player.getPose());
		check(pose != Pose.SWIMMING, "assis dans le fauteuil, on ne rampe plus");
		check(context.computeOnClient(mc -> mc.player.getVehicle() instanceof WheelchairEntity), "le joueur devrait être assis");
		context.takeScreenshot("wheelchair_02_seated");
		context.waitTicks(20);
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(3);
		context.takeScreenshot("wheelchair_02_seated_front");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		return chair;
	}

	/** Les deux bras font avancer le fauteuil ; pendant ce temps, les mains sont prises. */
	private static void roll(ClientGameTestContext context, TestSingleplayerContext sp, WheelchairEntity chair) {
		Vec3 before = clientPosition(context);
		context.getInput().holdKey(options -> options.keyUp);
		context.waitTicks(15);
		boolean busy = context.computeOnClient(mc -> Wheelchairs.handsBusy(mc.player));
		context.takeScreenshot("wheelchair_03_rolling");
		context.getInput().releaseKey(options -> options.keyUp);
		context.waitTicks(10);
		Vec3 after = clientPosition(context);
		check(after.distanceTo(before) > 1, "le fauteuil devrait avancer : " + before + " -> " + after);
		check(busy, "en roulant, les mains devraient être prises");
		check(!context.computeOnClient(mc -> Wheelchairs.handsBusy(mc.player)), "à l'arrêt, les mains sont libres");
	}

	/** Un bloc plein arrête le fauteuil ; une dalle se franchit. */
	private static void ramps(ClientGameTestContext context, TestSingleplayerContext sp) {
		teleportChair(context, sp, 0.5, 0.5);
		sp.getServer().runCommand("fill -3 -60 3 3 -60 3 minecraft:stone");
		sp.getServer().runCommand("fill -3 -60 8 3 -60 8 minecraft:smooth_stone_slab");
		sp.getConnection().waitForChunksRender();
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(5);
		context.getInput().holdKey(options -> options.keyUp);
		context.waitTicks(40);
		context.getInput().releaseKey(options -> options.keyUp);
		context.waitTicks(5);
		Vec3 blocked = clientPosition(context);
		check(blocked.z < 3 && blocked.y < -59.5, "un bloc plein devrait arrêter le fauteuil : " + blocked);
		context.takeScreenshot("wheelchair_04_blocked_by_full_block");

		teleportChair(context, sp, 0.5, 5.5);
		context.getInput().holdKey(options -> options.keyUp);
		context.waitTicks(40);
		context.getInput().releaseKey(options -> options.keyUp);
		context.waitTicks(5);
		Vec3 climbed = clientPosition(context);
		check(climbed.z > 8, "une dalle devrait se franchir : " + climbed);
		context.takeScreenshot("wheelchair_05_over_a_slab");
		sp.getServer().runCommand("fill -3 -60 3 3 -60 8 minecraft:air");
	}

	/** Le fauteuil ne saute pas. */
	private static void noJump(ClientGameTestContext context, TestSingleplayerContext sp) {
		teleportChair(context, sp, 0.5, 0.5);
		double y = clientPosition(context).y;
		context.getInput().holdKeyFor(options -> options.keyJump, 10);
		double highest = y;
		for (int i = 0; i < 10; i++) {
			context.waitTick();
			highest = Math.max(highest, clientPosition(context).y);
		}
		check(highest - y < 0.05, "le fauteuil ne devrait pas sauter : " + y + " -> " + highest);
	}

	/** Un seul bras ne fait tourner qu'une roue : on n'avance pas. */
	private static void oneArm(ClientGameTestContext context, TestSingleplayerContext sp) {
		onPlayer(context, sp, p -> {
			p.setAttached(FmabAttachments.GATE, GateState.NONE.pay(BodyPart.LEFT_LEG).pay(BodyPart.RIGHT_LEG)
					.pay(BodyPart.LEFT_ARM));
			p.setAttached(FmabAttachments.AUTOMAIL, Automail.NONE);
		});
		Vec3 before = clientPosition(context);
		context.getInput().holdKeyFor(options -> options.keyUp, 20);
		context.waitTicks(5);
		Vec3 after = clientPosition(context);
		check(after.distanceTo(before) < 0.1, "avec un seul bras, le fauteuil ne devrait pas avancer : " + after);
		context.runOnClient(mc -> RenderShowcase.hideGui(mc, false));
		context.waitTicks(2);
		context.takeScreenshot("wheelchair_06_one_arm_needs_help");
		context.runOnClient(mc -> RenderShowcase.hideGui(mc, true));
	}

	/** Un villageois payé pousse : même sans deuxième bras, on avance. */
	private static void villager(ClientGameTestContext context, TestSingleplayerContext sp) {
		sp.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			Villager villager = EntityTypes.VILLAGER.create(player.level(), EntitySpawnReason.COMMAND);
			villager.snapTo(player.getX(), player.getY(), player.getZ() - 1.2, 0, 0);
			player.level().addFreshEntity(villager);
			((WheelchairEntity) player.getVehicle()).hire(villager);
		});
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(5);
		Vec3 before = clientPosition(context);
		context.getInput().holdKey(options -> options.keyUp);
		context.waitTicks(20);
		context.takeScreenshot("wheelchair_07_pushed_by_villager");
		context.getInput().releaseKey(options -> options.keyUp);
		context.waitTicks(5);
		Vec3 after = clientPosition(context);
		check(after.distanceTo(before) > 1, "poussé par un villageois, le fauteuil devrait avancer : " + after);
	}

	/**
	 * Remet le fauteuil (et son occupant) à cet endroit, face au sud. Un fauteuil piloté suit la
	 * position de son client : on en descend, on le déplace, on s'y rassied.
	 */
	private static void teleportChair(ClientGameTestContext context, TestSingleplayerContext sp, double x, double z) {
		WheelchairEntity chair = sp.getServer().computeOnServer(server ->
				(WheelchairEntity) server.getPlayerList().getPlayers().getFirst().getVehicle());
		onPlayer(context, sp, ServerPlayer::stopRiding);
		context.waitTicks(3);
		onPlayer(context, sp, p -> {
			chair.snapTo(x, -60, z, 0, 0);
			p.teleportTo(x, -60, z);
		});
		context.waitTicks(3);
		onPlayer(context, sp, p -> p.startRiding(chair));
		context.runOnClient(mc -> {
			mc.player.setYRot(0);
			mc.player.setXRot(20);
		});
		context.waitTicks(5);
		Vec3 at = clientPosition(context);
		check(Math.abs(at.z - z) < 0.3, "le fauteuil devrait être en z=" + z + " : " + at);
	}
}
