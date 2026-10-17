package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.ForbiddenCircles;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.data.Tattoos;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.tattoo.TattooSlot;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.item.ItemStack;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * FMAB-62 : les gants et les tatouages se dessinent sur le joueur, et tous les clients les reçoivent.
 * Capture en vue extérieure. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=worn}.
 */
public class WornScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("worn")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getServer().runCommand("time set noon");
			sp.getConnection().waitForChunksRender();
			sp.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				FmabAttachments.GLOVES.set(p, Gloves.NONE.with(false, new ItemStack(FmabItems.SPARK_GLOVES))
						.with(true, new ItemStack(FmabItems.IRON_GAUNTLETS)));
				FmabAttachments.TATTOOS.set(p, Tattoos.NONE.with(TattooSlot.CHEST, ForbiddenCircles.HUMAN_TRANSMUTATION)
						.with(TattooSlot.FOREARM_LEFT, ForbiddenCircles.HUMAN_TRANSMUTATION));
			});
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(5);
			String seen = context.computeOnClient(client -> {
				Gloves gloves = FmabAttachments.GLOVES.get(client.player);
				Tattoos tattoos = FmabAttachments.TATTOOS.get(client.player);
				return "gants=" + (gloves != null && !gloves.get(false).isEmpty() && !gloves.get(true).isEmpty())
						+ " tatouages=" + (tattoos == null ? 0 : tattoos.slots().size());
			});
			check(seen.equals("gants=true tatouages=2"), "le client devrait voir gants et tatouages : " + seen);
			context.runOnClient(client -> {
				client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
				client.gui.hud.getChat().clearMessages(false);
			});
			context.waitTicks(10);
			context.takeScreenshot("worn_gloves_and_tattoos");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}
}
