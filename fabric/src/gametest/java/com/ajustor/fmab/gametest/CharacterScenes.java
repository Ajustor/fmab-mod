package com.ajustor.fmab.gametest;

import com.ajustor.fmab.entity.character.StoryCharacter;
import com.ajustor.fmab.entity.character.StoryCharacterEntity;
import com.ajustor.fmab.entity.character.StoryCharacters;
import com.ajustor.fmab.registry.FmabEntities;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.inventory.MerchantMenu;

import java.util.ArrayList;
import java.util.List;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les personnages de l'histoire, alignés face à la caméra (capture), puis abordés un à un : chacun
 * parle, et ceux qui tiennent boutique ouvrent l'écran d'échange. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=characters}.
 */
public class CharacterScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("characters")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getServer().runCommand("time set noon");
			sp.getServer().runCommand("tp @p 0.5 -60 -5.5 0 10");
			sp.getConnection().waitForChunksRender();
			List<String> problems = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				var level = p.level();
				List<String> out = new ArrayList<>();
				int i = 0;
				for (StoryCharacter c : StoryCharacters.ALL) {
					StoryCharacterEntity npc = FmabEntities.character(c).create(level, EntitySpawnReason.COMMAND);
					npc.snapTo(-5.5 + i % 6 * 2, -60, i / 6 * 2 + 1.5, 180, 0);
					npc.setNoAi(true);
					npc.setYBodyRot(180);
					npc.setYHeadRot(180);
					level.addFreshEntity(npc);
					i++;
				}
				for (StoryCharacterEntity npc : level.getEntitiesOfClass(StoryCharacterEntity.class,
						p.getBoundingBox().inflate(16))) {
					npc.interact(p, InteractionHand.MAIN_HAND, npc.position());
					boolean shop = p.containerMenu instanceof MerchantMenu;
					if (shop == npc.character().trades().isEmpty()) {
						out.add(npc.character().id() + (shop ? " ouvre une boutique vide" : " n'ouvre pas sa boutique"));
					}
					p.closeContainer();
				}
				return out;
			});
			check(problems.isEmpty(), "personnages mal abordés : " + problems);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
			context.waitTicks(20);
			context.takeScreenshot("characters_lineup");
		}
	}
}
