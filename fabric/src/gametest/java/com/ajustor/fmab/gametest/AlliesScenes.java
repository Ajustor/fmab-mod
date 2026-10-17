package com.ajustor.fmab.gametest;

import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.Allies;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * L'alchimie épargne son auteur et ses alliés (son loup, les villageois) mais pas un zombie ; et un
 * alchimiste qui a perdu la vue ne court plus, quoi que montre son écran. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=allies}.
 */
public class AlliesScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("allies")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getConnection().waitForChunksRender();
			String spared = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				var level = p.level();
				var wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
				wolf.snapTo(p.getX() + 2, p.getY(), p.getZ(), 0, 0);
				level.addFreshEntity(wolf);
				wolf.tame(p);
				var villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
				villager.snapTo(p.getX() - 2, p.getY(), p.getZ(), 0, 0);
				level.addFreshEntity(villager);
				var zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
				zombie.snapTo(p.getX(), p.getY(), p.getZ() + 3, 0, 0);
				level.addFreshEntity(zombie);
				return "soi=" + Allies.spares(p, p) + " loup=" + Allies.spares(p, wolf)
						+ " villageois=" + Allies.spares(p, villager) + " zombie=" + Allies.spares(p, zombie);
			});
			check(spared.equals("soi=true loup=true villageois=true zombie=false"),
					"l'alchimie devrait épargner l'auteur, son loup et les villageois, pas le zombie : " + spared);

			// Sans yeux, plus de course : le serveur l'arrête au tick suivant.
			sp.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				FmabAttachments.GATE.set(p, GateState.NONE.pay(BodyPart.SIGHT));
				p.setSprinting(true);
			});
			context.waitTicks(3);
			boolean sprinting = sp.getServer().computeOnServer(server ->
					server.getPlayerList().getPlayers().getFirst().isSprinting());
			check(!sprinting, "un alchimiste aveugle ne devrait pas pouvoir courir");
		}
	}
}
