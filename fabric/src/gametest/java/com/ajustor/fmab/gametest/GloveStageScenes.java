package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les gants d'État portent cinq étages, mais un Alchimiste d'État n'en tient que trois : leur
 * infobulle le dit, et se tait pour un Initié de la Porte. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=glove_stages}.
 */
public class GloveStageScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("glove_stages")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			String state = tooltip(context, sp, Rank.STATE);
			String limit = Integer.toString(Rank.STATE.maxStages());
			check(state.lines().anyMatch(line -> line.contains(limit) && !line.contains("5")),
					"un Alchimiste d'État devrait lire qu'il n'en tient que " + limit + " :\n" + state);
			String gate = tooltip(context, sp, Rank.GATE);
			check(gate.lines().count() < state.lines().count(),
					"un Initié de la Porte tient les cinq étages, rien à signaler :\n" + gate);
		}
	}

	/** L'infobulle des gants d'État, lue par un joueur de ce rang. */
	private static String tooltip(ClientGameTestContext context, TestSingleplayerContext sp, Rank rank) {
		sp.getServer().runOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst();
			FmabAttachments.ALCHEMIST.set(p, AlchemistData.NEW.withRank(rank));
		});
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(5);
		return context.computeOnClient(client -> {
			StringBuilder text = new StringBuilder();
			new ItemStack(FmabItems.STATE_GLOVES)
					.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL)
					.forEach(line -> text.append(line.getString()).append('\n'));
			return text.toString();
		});
	}
}
