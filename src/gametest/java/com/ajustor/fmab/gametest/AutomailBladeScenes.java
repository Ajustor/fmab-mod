package com.ajustor.fmab.gametest;

import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Le bras d'automail transmuté en lame, comme celui d'Ed : avant, la lame sortie (de face, de
 * profil, à la première personne), puis rentrée. Vérifie au passage que la lame tranche plus fort
 * qu'un poing. Lancer avec {@code ./gradlew runClientGameTest -PfmabGametest=automail_blade}.
 */
public class AutomailBladeScenes implements FabricClientGameTest {
	private static void onPlayer(ClientGameTestContext context, TestSingleplayerContext sp, Consumer<ServerPlayer> action) {
		sp.getServer().runOnServer(server -> action.accept(server.getPlayerList().getPlayers().getFirst()));
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(3);
	}

	private static double attack(TestSingleplayerContext sp) {
		return sp.getServer().computeOnServer(server ->
				server.getPlayerList().getPlayers().getFirst().getAttributeValue(Attributes.ATTACK_DAMAGE));
	}

	/** Tourne la tête de trois quarts (ou de profil), sans le corps. */
	private static void turn(ClientGameTestContext context, float yaw) {
		context.runOnClient(mc -> {
			mc.player.setYRot(yaw);
			mc.player.yRotO = yaw;
			mc.player.setYHeadRot(yaw);
			mc.player.yHeadRotO = yaw;
			mc.player.yBodyRot = 0;
			mc.player.yBodyRotO = 0;
		});
		context.waitTick();
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("automail_blade")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("tp @p 0 -60 0 0 0");
			// Main nue : la lame ne tranche qu'ainsi (on reçoit un traité en arrivant).
			sp.getServer().runCommand("clear @p");
			sp.getConnection().waitForChunksRender();
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			});
			onPlayer(context, sp, p -> {
				p.setAttached(FmabAttachments.GATE, GateState.NONE.pay(BodyPart.RIGHT_ARM));
				p.setAttached(FmabAttachments.AUTOMAIL, Automail.NONE
						.with(BodyPart.RIGHT_ARM, new ItemStack(FmabItems.IRON_AUTOMAIL_ARM)));
			});
			context.waitTicks(10);
			double fist = attack(sp);
			context.takeScreenshot("automail_blade_0_before");

			onPlayer(context, sp, Automails::transmuteArm);
			context.waitTicks(5);
			double blade = attack(sp);
			if (blade <= fist) {
				String state = sp.getServer().computeOnServer(server -> {
					ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
					ItemStack arm = p.getAttachedOrCreate(FmabAttachments.AUTOMAIL).get(BodyPart.RIGHT_ARM);
					return "lame " + Automails.bladed(arm) + ", en état " + Automails.working(p, BodyPart.RIGHT_ARM)
							+ ", main tenue " + p.getMainHandItem();
				});
				throw new AssertionError("La lame du bras ne tranche pas plus fort qu'un poing : " + blade + " <= " + fist
						+ " (" + state + ")");
			}
			context.takeScreenshot("automail_blade_1_out_front");
			turn(context, 60);
			context.takeScreenshot("automail_blade_2_out_three_quarter");
			turn(context, 90);
			context.takeScreenshot("automail_blade_3_out_side");
			// Le bras levé pour frapper.
			context.runOnClient(mc -> mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND));
			context.waitTicks(2);
			context.takeScreenshot("automail_blade_4_out_swing");
			turn(context, 0);
			// À la première personne, la main ne se dessine qu'avec l'interface.
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				RenderShowcase.hideGui(mc, false);
			});
			context.waitTicks(10);
			context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
			context.takeScreenshot("automail_blade_5_out_first_person");
			context.runOnClient(mc -> RenderShowcase.hideGui(mc, true));

			onPlayer(context, sp, Automails::transmuteArm);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(5);
			if (attack(sp) != fist) {
				throw new AssertionError("La lame rentrée, le poing devrait frapper comme avant");
			}
			context.takeScreenshot("automail_blade_6_back_in");
		}
	}
}
