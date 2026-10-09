package com.ajustor.fmab.gametest;

import com.ajustor.fmab.client.screen.BodyScreen;
import com.ajustor.fmab.client.screen.GlovesScreen;
import com.ajustor.fmab.client.screen.NotebookScreen;
import com.ajustor.fmab.client.screen.TreatiseScreen;
import com.ajustor.fmab.client.screen.WinryScreen;
import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.TransmutationPose;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.network.OpenBodyPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;

/**
 * Une visite guidée des rendus du mod, en captures d'écran : le corps du joueur (membres perdus,
 * automails, gestes), chaque créature, les écrans et le HUD. Lancer avec {@code ./gradlew runClientGameTest} ;
 * les images arrivent dans {@code build/run/clientGameTest/screenshots}.
 */
public class RenderShowcase implements FabricClientGameTest {
	private static final List<String> ENTITIES = List.of("izumi", "state_examiner", "stone_golem", "truth", "winry",
			"lust", "gluttony", "envy", "greed", "sloth", "wrath", "pride", "father", "may_chang", "drachma_soldier",
			"olivier", "amestrian_soldier", "briggs_soldier", "immortal_soldier", "haunted_armor", "barry",
			"chimera_beast", "chimera_crawler", "cornello", "scar", "hohenheim", "marcoh", "gate_hand", "kunai",
			"throwing_knife");

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("weather clear");
			sp.getConnection().waitForChunksRender();
			context.runOnClient(mc -> hideGui(mc, true));

			body(context, sp);
			entities(context, sp);
			context.runOnClient(mc -> hideGui(mc, false));
			screens(context, sp);
			hud(context, sp);
		}
	}

	private static void hideGui(net.minecraft.client.Minecraft mc, boolean hidden) {
		if (mc.gui.hud.isHidden() != hidden) {
			mc.gui.hud.toggle();
		}
		// Les retours des commandes de mise en scène encombreraient les captures.
		mc.gui.hud.getChat().clearMessages(false);
	}

	/** Retire les créatures d'un coup, sans animation de mort ni butin. */
	private static void clear(ClientGameTestContext context, TestSingleplayerContext sp) {
		sp.getServer().runOnServer(server -> {
			List<Entity> all = new java.util.ArrayList<>();
			server.overworld().getAllEntities().forEach(all::add);
			all.stream().filter(e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
		});
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(2);
	}

	/**
	 * Montre le corps de trois quarts : la caméra de face suit la tête, qu'on tourne sans le corps (un
	 * joueur immobile ne tourne le corps qu'au-delà d'un certain angle).
	 */
	private static void sideCamera(ClientGameTestContext context, TestSingleplayerContext sp, boolean on) {
		float yaw = on ? 45 : 0;
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			mc.player.setYRot(yaw);
			mc.player.yRotO = yaw;
			mc.player.setYHeadRot(yaw);
			mc.player.yHeadRotO = yaw;
			mc.player.yBodyRot = on ? -15 : 0;
			mc.player.yBodyRotO = mc.player.yBodyRot;
		});
		context.waitTick();
	}

	private static void command(TestSingleplayerContext sp, String command) {
		sp.getServer().runCommand(command);
	}

	private static void onPlayer(ClientGameTestContext context, TestSingleplayerContext sp, java.util.function.Consumer<ServerPlayer> action) {
		sp.getServer().runOnServer(server -> action.accept(server.getPlayerList().getPlayers().getFirst()));
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(2);
	}

	private static void body(ClientGameTestContext context, TestSingleplayerContext sp) {
		command(sp, "tp @p 0 -60 0 0 0");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(5);
		context.takeScreenshot("body_00_intact");

		onPlayer(context, sp, p -> p.setAttached(FmabAttachments.GATE,
				GateState.NONE.pay(BodyPart.RIGHT_ARM).pay(BodyPart.LEFT_LEG)));
		context.takeScreenshot("body_01_lost_right_arm_left_leg");

		onPlayer(context, sp, p -> {
			p.setAttached(FmabAttachments.GATE, GateState.NONE.pay(BodyPart.RIGHT_ARM).pay(BodyPart.LEFT_ARM)
					.pay(BodyPart.RIGHT_LEG).pay(BodyPart.LEFT_LEG));
			p.setAttached(FmabAttachments.AUTOMAIL, Automail.NONE
					.with(BodyPart.RIGHT_ARM, new ItemStack(FmabItems.IRON_AUTOMAIL_ARM))
					.with(BodyPart.LEFT_ARM, new ItemStack(FmabItems.RUSH_VALLEY_AUTOMAIL_ARM))
					.with(BodyPart.RIGHT_LEG, new ItemStack(FmabItems.BRIGGS_AUTOMAIL_LEG)));
		});
		context.takeScreenshot("body_02_automail_iron_rush_briggs_leftleg_lost");

		onPlayer(context, sp, p -> p.setAttached(FmabAttachments.AUTOMAIL, Automail.NONE
				.with(BodyPart.RIGHT_ARM, new ItemStack(FmabItems.IRON_AUTOMAIL_ARM))
				.with(BodyPart.LEFT_ARM, new ItemStack(FmabItems.IRON_AUTOMAIL_ARM))
				.with(BodyPart.RIGHT_LEG, new ItemStack(FmabItems.IRON_AUTOMAIL_LEG))
				.with(BodyPart.LEFT_LEG, new ItemStack(FmabItems.IRON_AUTOMAIL_LEG))));
		context.takeScreenshot("body_03_full_iron");

		for (TransmutationPose.Kind kind : List.of(TransmutationPose.Kind.CLAP, TransmutationPose.Kind.PALM,
				TransmutationPose.Kind.REACH)) {
			onPlayer(context, sp, p -> TransmutationPose.strike(p, kind, 100000));
			context.waitTicks(5);
			context.takeScreenshot("body_04_pose_" + kind.name().toLowerCase());
			sideCamera(context, sp, true);
			context.takeScreenshot("body_04_pose_" + kind.name().toLowerCase() + "_side");
			sideCamera(context, sp, false);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		}
		onPlayer(context, sp, p -> TransmutationPose.strike(p, TransmutationPose.Kind.NONE, 0));
		sideCamera(context, sp, true);
		context.takeScreenshot("body_04_full_iron_side");
		onPlayer(context, sp, p -> {
			p.setAttached(FmabAttachments.GATE, GateState.NONE.pay(BodyPart.RIGHT_ARM).pay(BodyPart.LEFT_LEG));
			p.setAttached(FmabAttachments.AUTOMAIL, Automail.NONE);
		});
		context.takeScreenshot("body_04_lost_side");
		onPlayer(context, sp, p -> {
			p.setAttached(FmabAttachments.GATE, GateState.NONE.pay(BodyPart.RIGHT_ARM).pay(BodyPart.LEFT_ARM)
					.pay(BodyPart.RIGHT_LEG).pay(BodyPart.LEFT_LEG));
			p.setAttached(FmabAttachments.AUTOMAIL, Automail.NONE
					.with(BodyPart.RIGHT_ARM, new ItemStack(FmabItems.IRON_AUTOMAIL_ARM))
					.with(BodyPart.LEFT_ARM, new ItemStack(FmabItems.IRON_AUTOMAIL_ARM))
					.with(BodyPart.RIGHT_LEG, new ItemStack(FmabItems.IRON_AUTOMAIL_LEG))
					.with(BodyPart.LEFT_LEG, new ItemStack(FmabItems.IRON_AUTOMAIL_LEG)));
		});
		sideCamera(context, sp, false);

		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(5);
		context.takeScreenshot("body_05_back");
		// Vue à la première personne : le bras perdu, ou son automail.
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		context.runOnClient(mc -> {
			hideGui(mc, false);
			mc.gui.hud.getChat().clearMessages(false);
		});
		context.waitTicks(5);
		context.takeScreenshot("body_06_first_person_iron");
		onPlayer(context, sp, p -> p.setAttached(FmabAttachments.AUTOMAIL, Automail.NONE));
		context.takeScreenshot("body_07_first_person_lost");
		context.runOnClient(mc -> hideGui(mc, true));
		onPlayer(context, sp, p -> {
			p.setAttached(FmabAttachments.GATE, GateState.NONE);
			p.setAttached(FmabAttachments.AUTOMAIL, Automail.NONE);
		});
	}

	private static void entities(ClientGameTestContext context, TestSingleplayerContext sp) {
		int index = 0;
		for (String id : ENTITIES) {
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(com.ajustor.fmab.Fmab.id(id));
			float height = type.getDimensions().height();
			float width = type.getDimensions().width();
			double distance = 2.5 + Math.max(height, width) * 1.3;
			double centre = height / 2;
			float pitch = (float) Math.toDegrees(Math.atan2(1.62 - centre, distance));
			clear(context, sp);
			command(sp, "tp @p 0 -60 0 0 " + pitch);
			command(sp, "summon fmab:%s 0.5 -60 %.2f {NoAI:1b,NoGravity:1b,Silent:1b,Invulnerable:1b,Rotation:[150f,0f]}"
					.formatted(id, distance));
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(20);
			context.takeScreenshot("entity_%02d_%s".formatted(index++, id));
			if (type == FmabEntities.TRUTH) {
				sp.getServer().runOnServer(server -> server.overworld().getEntities(FmabEntities.TRUTH, e -> true)
						.forEach(t -> t.setStolen(EnumSet.of(BodyPart.RIGHT_ARM, BodyPart.LEFT_LEG, BodyPart.SIGHT))));
				context.waitTicks(10);
				context.takeScreenshot("entity_%02d_truth_stolen".formatted(index++));
			}
		}
		clear(context, sp);
	}

	private static void screens(ClientGameTestContext context, TestSingleplayerContext sp) {
		command(sp, "tp @p 0 -60 0 0 0");
		onPlayer(context, sp, p -> {
			p.setAttached(FmabAttachments.GATE, GateState.NONE.pay(BodyPart.RIGHT_ARM).pay(BodyPart.LEFT_LEG));
			p.setAttached(FmabAttachments.AUTOMAIL, Automail.NONE
					.with(BodyPart.RIGHT_ARM, new ItemStack(FmabItems.IRON_AUTOMAIL_ARM)));
		});
		context.runOnClient(mc -> {
			mc.gui.hud.getChat().clearMessages(false);
			ClientPlayNetworking.send(OpenBodyPayload.INSTANCE);
		});
		context.waitForScreen(BodyScreen.class);
		context.takeScreenshot("screen_body");
		context.setScreen(() -> null);

		context.setScreen(NotebookScreen::new);
		context.takeScreenshot("screen_notebook");
		context.setScreen(GlovesScreen::new);
		context.takeScreenshot("screen_gloves");
		context.setScreen(TreatiseScreen::new);
		context.takeScreenshot("screen_treatise");
		context.setScreen(() -> null);

		command(sp, "summon fmab:winry 0.5 -60 3 {NoAI:1b}");
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(5);
		int winry = context.computeOnClient(mc -> {
			for (Entity e : mc.level.entitiesForRendering()) {
				if (e.getType() == FmabEntities.WINRY) {
					return e.getId();
				}
			}
			return -1;
		});
		context.setScreen(() -> new WinryScreen(winry));
		context.takeScreenshot("screen_winry");
		context.setScreen(() -> null);
		clear(context, sp);
	}

	private static void hud(ClientGameTestContext context, TestSingleplayerContext sp) {
		command(sp, "gamemode survival @p");
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		context.waitTicks(5);
		context.takeScreenshot("hud_00_alchemist_panel");
		onPlayer(context, sp, p -> p.setAttached(FmabAttachments.ALCHEMIST,
				p.getAttachedOrCreate(FmabAttachments.ALCHEMIST).withConcentration(5)));
		onPlayer(context, sp, p -> p.setAttached(FmabAttachments.LIVING_STONE, 42));
		context.takeScreenshot("hud_01_concentration_living_stone");
		onPlayer(context, sp, p -> p.setAttached(FmabAttachments.GATE,
				p.getAttachedOrCreate(FmabAttachments.GATE).pay(BodyPart.SIGHT)));
		context.takeScreenshot("hud_02_lost_sight");
		onPlayer(context, sp, p -> p.setAttached(FmabAttachments.GATE, GateState.NONE));
		command(sp, "time set " + (7 * 24000 + 6000));
		context.waitTicks(20);
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		context.waitTick();
		context.takeScreenshot("hud_03_eclipse");
	}
}
