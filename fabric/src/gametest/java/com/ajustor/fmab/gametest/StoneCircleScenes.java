package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.ForbiddenCircles;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.entity.SoulEyeEntity;
import com.ajustor.fmab.item.PhilosopherStoneItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.stone.LivingStone;
import com.ajustor.fmab.stone.StoneCircle;
import com.ajustor.fmab.stone.StoneRite;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.Effects;
import com.ajustor.fmab.transmutation.Transmutation;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Function;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les deux cercles interdits. Le cercle de la Pierre, du sang cristallisé posé dessus, prend les vies
 * d'une ferme entière sur le rayon que l'alchimiste choisit, et les condense en Pierres. Sans sang, celui
 * qui se tient en son centre devient la Pierre : une Pierre vivante, qui transmute sans cercle. Le cercle
 * de transmutation humaine, lui, ne fait plus de Pierre. Le tout à la portée d'un simple Alchimiste. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=stone_circle}.
 */
public class StoneCircleScenes implements FabricClientGameTest {
	private static final BlockPos CIRCLE = new BlockPos(0, -60, 3);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("stone_circle")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("gamerule doMobSpawning false");
			sp.getConnection().waitForChunksRender();
			onServer(sp, p -> {
				AlchemistData data = FmabAttachments.ALCHEMIST.getOrCreate(p).withRank(Rank.ALCHEMIST);
				for (Glyph g : AlchemyRules.of(p.level().registryAccess()).glyphs()) {
					data = data.learn(g.id());
				}
				FmabAttachments.ALCHEMIST.set(p, data);
				p.getInventory().clearContent();
				return null;
			});

			// 1. Une ferme de dix villageois à vingt blocs : le rayon demandé les atteint tous.
			sp.getServer().runCommand("tp @p 0.5 -60 -2.5 0 30");
			villagers(sp, 10, 18);
			blood(sp);
			check(cast(context, sp, ForbiddenCircles.PHILOSOPHER_STONE), "le cercle de la Pierre devrait s'éveiller");
			Effects.Result farm = onServer(sp, p -> StoneCircle.answer(p, 24));
			context.setScreen(() -> null);
			context.waitTicks(StoneRite.AWAKENING - 5);
			check(farm == Effects.Result.DONE, "le sacrifice devrait avoir lieu : " + farm);
			// Le rite se joue comme à Xerxès : la lumière gagne le cercle avant que les vies tombent.
			check(alive(sp) == 10, "pendant l'éveil du cercle, la ferme est figée mais encore en vie : " + alive(sp));
			check(eyes(sp) == 10, "un œil s'ouvre dans le sol sous chaque villageois que la lumière atteint : " + eyes(sp));
			context.takeScreenshot("stone_circle_rite_awakening");
			// Vu du dessus : les yeux ouverts dans le sol sous la ferme.
			sp.getServer().runCommand("tp @p 0.5 -52 18.5 0 90");
			context.waitTicks(3);
			context.takeScreenshot("stone_circle_rite_eyes");
			sp.getServer().runCommand("tp @p 0.5 -60 -2.5 0 30");
			context.waitTicks(22);
			context.takeScreenshot("stone_circle_rite_harvest");
			context.waitTicks(StoneRite.LENGTH - StoneRite.AWAKENING + 5);
			check(alive(sp) == 0, "toute la ferme devrait être consumée, à vingt blocs");
			check(eyes(sp) == 0, "les yeux se referment sur les morts : " + eyes(sp));
			List<Integer> stones = onServer(sp, p -> p.level().getEntitiesOfClass(ItemEntity.class, new AABB(CIRCLE).inflate(3),
					e -> e.getItem().is(FmabItems.PHILOSOPHER_STONE)).stream()
					.map(e -> PhilosopherStoneItem.souls(e.getItem())).sorted().toList());
			check(stones.equals(List.of(20, 100)), "cent vingt âmes : une Pierre pleine et une de vingt : " + stones);
			context.takeScreenshot("stone_circle_farm");

			// 2. Sans sang, l'alchimiste au centre du cercle devient la Pierre.
			sp.getServer().runCommand("kill @e[type=item]");
			sp.getServer().runCommand("tp @p 0.5 -60 3.5 0 30");
			villagers(sp, 3, 4);
			check(cast(context, sp, ForbiddenCircles.PHILOSOPHER_STONE), "sans sang, le joueur au centre devrait suffire");
			Effects.Result self = onServer(sp, p -> StoneCircle.answer(p, 8));
			context.setScreen(() -> null);
			context.waitTicks(StoneRite.AWAKENING + 25);
			check(self == Effects.Result.DONE, "le sacrifice devrait avoir lieu : " + self);
			context.takeScreenshot("stone_circle_rite_vessel");
			context.waitTicks(StoneRite.LENGTH - StoneRite.AWAKENING - 15);
			check(onServer(sp, LivingStone::souls) == 36, "trois villageois, trente-six âmes en lui : " + onServer(sp, LivingStone::souls));
			check(onServer(sp, p -> p.level().getEntitiesOfClass(ItemEntity.class, new AABB(CIRCLE).inflate(3),
					e -> e.getItem().is(FmabItems.PHILOSOPHER_STONE)).isEmpty()), "aucune Pierre en objet : c'est lui, la Pierre");
			context.takeScreenshot("stone_circle_living");
			context.waitTicks(30);
			check(!onServer(sp, p -> p.hasEffect(MobEffects.GLOWING)), "une Pierre vivante ne luit pas : elle se cache dans un corps");
			onServer(sp, p -> {
				p.getFoodData().setFoodLevel(2);
				return null;
			});
			context.waitTicks(30);
			check(onServer(sp, p -> p.getFoodData().getFoodLevel()) == 20,
					"une Pierre vivante n'a jamais faim : " + onServer(sp, p -> p.getFoodData().getFoodLevel()));

			// 3. Le cercle de transmutation humaine, du sang dessus, ne fait plus de Pierre.
			sp.getServer().runCommand("tp @p 0.5 -60 -2.5 0 30");
			villagers(sp, 2, 3);
			blood(sp);
			cast(context, sp, ForbiddenCircles.HUMAN_TRANSMUTATION);
			check(alive(sp) == 2, "le cercle humain ne devrait consumer personne");
		}
	}

	private static void villagers(TestSingleplayerContext sp, int count, int distance) {
		for (int i = 0; i < count; i++) {
			sp.getServer().runCommand(String.format("summon minecraft:villager %d -60 %d {NoAI:1b}", i - count / 2, 3 + distance));
		}
		sp.getConnection().waitForClientboundPackets();
	}

	private static int alive(TestSingleplayerContext sp) {
		return onServer(sp, p -> p.level().getEntitiesOfClass(Villager.class, new AABB(CIRCLE).inflate(40), Villager::isAlive).size());
	}

	private static int eyes(TestSingleplayerContext sp) {
		return onServer(sp, p -> p.level().getEntitiesOfClass(SoulEyeEntity.class, new AABB(CIRCLE).inflate(40)).size());
	}

	private static void blood(TestSingleplayerContext sp) {
		onServer(sp, p -> {
			ServerLevel level = p.level();
			ItemEntity e = new ItemEntity(level, CIRCLE.getX() + 0.5, CIRCLE.getY() + 0.1, CIRCLE.getZ() + 0.5,
					new ItemStack(FmabItems.CRYSTALLIZED_BLOOD));
			e.setDeltaMovement(0, 0, 0);
			e.setPickUpDelay(32767);
			level.addFreshEntity(e);
			return null;
		});
	}

	private static boolean cast(ClientGameTestContext context, TestSingleplayerContext sp, Drawing drawing) {
		Transmutation.Result result = onServer(sp, p -> {
			ServerLevel level = p.level();
			BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
					.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
			level.setBlockAndUpdate(CIRCLE, state);
			if (level.getBlockEntity(CIRCLE) instanceof TransmutationCircleBlockEntity be) {
				be.setDrawing(drawing);
			}
			Concentration.set(p, Concentration.max(p));
			return Transmutation.activate(level, CIRCLE, state, drawing, p, CircleSize.NORMAL);
		});
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(10);
		return result == Transmutation.Result.DONE;
	}

	private static <T> T onServer(TestSingleplayerContext sp, Function<ServerPlayer, T> action) {
		return sp.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
	}
}
