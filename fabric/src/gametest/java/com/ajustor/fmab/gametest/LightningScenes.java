package com.ajustor.fmab.gametest;

import com.ajustor.fmab.entity.DischargeEntity;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les éclairs d'une transmutation sont de vrais éclairs : le serveur pose une décharge, le client la
 * reçoit et la dessine (bleue, ou rouge quand une Pierre la nourrit), puis elle s'éteint d'elle-même.
 * Lancer avec {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=lightning}.
 */
public class LightningScenes implements FabricClientGameTest {
	private static final BlockPos BLUE = new BlockPos(-2, -60, 5);
	private static final BlockPos RED = new BlockPos(2, -60, 5);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("lightning")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getServer().runCommand("time set night");
			sp.getServer().runCommand("tp @p 0.5 -60 0.5 0 10");
			sp.getConnection().waitForChunksRender();
			context.waitTicks(20);

			sp.getServer().runOnServer(server -> {
				var level = server.overworld();
				TransmutationLightning.discharge(level, BLUE, 1.5, 1);
				TransmutationLightning.discharge(level, RED, 1.5, 1, true);
			});
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			int seen = context.computeOnClient(client -> client.level.getEntitiesOfClass(DischargeEntity.class,
					new AABB(BLUE).minmax(new AABB(RED)).inflate(2), d -> d.bolts() > 0).size());
			check(seen == 2, "le client devrait recevoir les deux décharges à dessiner : " + seen);
			boolean colours = context.computeOnClient(client -> client.level.getEntitiesOfClass(DischargeEntity.class,
					new AABB(RED).inflate(1), DischargeEntity::red).size() == 1);
			check(colours, "la décharge nourrie d'une Pierre devrait être rouge");
			context.takeScreenshot("lightning_discharges");

			// Une décharge ne s'attarde pas : le monde ne garde pas d'éclairs.
			context.waitTicks(20);
			boolean gone = sp.getServer().computeOnServer(server -> server.overworld().getEntitiesOfClass(
					DischargeEntity.class, new AABB(BLUE).minmax(new AABB(RED)).inflate(2)).isEmpty());
			check(gone, "les décharges devraient s'éteindre d'elles-mêmes");
		}
	}
}
