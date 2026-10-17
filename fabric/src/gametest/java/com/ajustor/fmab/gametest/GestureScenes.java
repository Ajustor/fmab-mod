package com.ajustor.fmab.gametest;

import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.TransmutationPose;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les gestes de transmutation du joueur, image par image : les mains qui claquent, les paumes qui
 * plongent sur le cercle. Vus de trois quarts, à la troisième personne, puis de ses propres yeux
 * (avec aussi le bras tendu vers une âme). Lancer avec
 * {@code ./gradlew runClientGameTest -PfmabGametest=gestures}.
 */
public class GestureScenes implements FabricClientGameTest {
	/** Les instants photographiés, en ticks depuis le début du geste. */
	private static final int[] FRAMES = {1, 3, 5, 7, 11};

	/** Un geste vu de ses propres yeux, aux mêmes instants qu'à la troisième personne. */
	private static void firstPerson(ClientGameTestContext context, TestSingleplayerContext sp,
			TransmutationPose.Kind kind, String name) {
		context.waitTicks(20);
		sp.getServer().runOnServer(server -> TransmutationPose.strike(server.getPlayerList().getPlayers().getFirst(),
				kind, 14));
		int at = 0;
		for (int frame : FRAMES) {
			context.waitTicks(frame - at);
			at = frame;
			context.runOnClient(mc -> {
				mc.gui.hud.getChat().clearMessages(false);
				mc.gui.toastManager().clear();
			});
			context.takeScreenshot("%s_%02d".formatted(name, frame));
		}
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("gestures")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("clear @p");
			sp.getServer().runCommand("tp @p 0 -60 0 0 0");
			sp.getConnection().waitForChunksRender();
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			});
			for (TransmutationPose.Kind kind : new TransmutationPose.Kind[]{TransmutationPose.Kind.CLAP,
					TransmutationPose.Kind.PALM}) {
				context.waitTicks(10);
				sp.getServer().runOnServer(server -> TransmutationPose.strike(server.getPlayerList().getPlayers().getFirst(),
						kind, 14));
				sp.getConnection().waitForClientboundPackets();
				check(context.computeOnClient(mc -> FmabAttachments.POSE.getOrElse(mc.player, TransmutationPose.NONE)
						.kind()) == kind, "le client devrait recevoir le geste " + kind);
				int at = 0;
				for (int frame : FRAMES) {
					context.waitTicks(frame - at);
					at = frame;
					// La caméra de face suit le regard : la tête tournée, on voit le corps de trois quarts.
					context.runOnClient(mc -> {
						mc.player.setYRot(45);
						mc.player.yRotO = 45;
						mc.player.setYHeadRot(45);
						mc.player.yHeadRotO = 45;
						mc.player.yBodyRot = -15;
						mc.player.yBodyRotO = -15;
					});
					context.takeScreenshot("gesture_%s_%02d".formatted(kind.name().toLowerCase(), frame));
				}
			}
			// De ses propres yeux, un peu penché vers le sol : les mains doivent paraître devant soi. La
			// main ne se dessine qu'avec l'interface.
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				RenderShowcase.hideGui(mc, false);
				mc.player.setXRot(20);
				mc.player.xRotO = 20;
			});
			for (TransmutationPose.Kind kind : new TransmutationPose.Kind[]{TransmutationPose.Kind.CLAP,
					TransmutationPose.Kind.PALM, TransmutationPose.Kind.REACH}) {
				firstPerson(context, sp, kind, "gesture_fp_" + kind.name().toLowerCase());
			}
			// Le bras gauche d'automail suit le geste comme le bras de chair.
			sp.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				FmabAttachments.GATE.set(p, GateState.NONE.pay(BodyPart.LEFT_ARM));
				FmabAttachments.AUTOMAIL.set(p, Automail.NONE
						.with(BodyPart.LEFT_ARM, new ItemStack(FmabItems.IRON_AUTOMAIL_ARM)));
			});
			firstPerson(context, sp, TransmutationPose.Kind.CLAP, "gesture_fp_automail_clap");
		}
	}
}
