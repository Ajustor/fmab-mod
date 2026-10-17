package com.ajustor.fmab.gametest;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.SoulArmor;
import com.ajustor.fmab.gate.Wheelchairs;
import com.ajustor.fmab.registry.FmabAttachments;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Function;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Une âme dans une armure sans jambières va comme un joueur sans jambes : moins vite, sans sauter, en
 * rampant. Des jambières lui rendent ses jambes ; les retirer les lui reprend. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=soul_legs}.
 */
public class SoulLegsScenes implements FabricClientGameTest {
	private static final int Y = -60;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("soul_legs")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("tp @p 0.5 %d 0.5 0 0".formatted(Y));
			sp.getConnection().waitForChunksRender();
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			});
			// Le joueur devient une âme dans une armure de fer complète (jambières comprises).
			server(sp, p -> {
				GateState everything = GateState.NONE;
				for (BodyPart part : BodyPart.values()) {
					everything = everything.pay(part);
				}
				FmabAttachments.GATE.set(p, everything);
				SoulArmor.bindFirstTime(p);
				return null;
			});
			context.waitTicks(10);
			double full = server(sp, p -> p.getAttributeValue(Attributes.MOVEMENT_SPEED));
			check(server(sp, SoulLegsScenes::walks), "avec ses jambières, l'âme devrait marcher normalement");
			context.takeScreenshot("soul_legs_0_with_leggings");

			// Sans jambières : sans jambes.
			server(sp, p -> {
				p.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
				return null;
			});
			context.waitTicks(10);
			check(server(sp, SoulLegsScenes::legless), "sans jambières, l'âme devrait aller comme un joueur sans jambes");
			check(server(sp, p -> p.getAttributeValue(Attributes.MOVEMENT_SPEED) < full),
					"sans jambières, l'âme devrait avancer moins vite");
			check(server(sp, p -> p.getAttributeValue(Attributes.JUMP_STRENGTH) == 0), "sans jambières, l'âme ne devrait plus sauter");
			check(context.computeOnClient(mc -> Wheelchairs.mustCrawl(mc.player)), "le client devrait aussi la faire ramper");
			// Pas de modificateur en double : un seul par jambe, malgré les ticks passés.
			check(server(sp, p -> p.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers().stream()
					.filter(m -> m.id().getNamespace().equals(Fmab.MOD_ID)).count() == 2), "un modificateur par jambe, pas plus");
			context.takeScreenshot("soul_legs_1_no_leggings");

			// Des jambières lui rendent ses jambes.
			server(sp, p -> {
				p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
				return null;
			});
			context.waitTicks(10);
			check(server(sp, SoulLegsScenes::walks), "des jambières devraient lui rendre ses jambes");
			check(server(sp, p -> p.getAttributeValue(Attributes.MOVEMENT_SPEED) == full), "toute sa vitesse devrait revenir");
			context.takeScreenshot("soul_legs_2_leggings_back");

			// Les retirer de nouveau les lui reprend.
			server(sp, p -> {
				p.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
				return null;
			});
			context.waitTicks(10);
			check(server(sp, SoulLegsScenes::legless), "sans jambières, de nouveau sans jambes");
		}
	}

	private static boolean legless(ServerPlayer p) {
		return Wheelchairs.legless(p) && Wheelchairs.mustCrawl(p);
	}

	private static boolean walks(ServerPlayer p) {
		return !Wheelchairs.legless(p) && !Wheelchairs.mustCrawl(p) && p.getAttributeValue(Attributes.JUMP_STRENGTH) > 0;
	}

	private static <T> T server(TestSingleplayerContext sp, Function<ServerPlayer, T> action) {
		return sp.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
	}
}
