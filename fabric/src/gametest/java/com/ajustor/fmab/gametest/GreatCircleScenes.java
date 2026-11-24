package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.ForbiddenCircles;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.client.GreatCirclePlanner;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.entity.FailedTransmutationEntity;
import com.ajustor.fmab.entity.GreatCircleEntity;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.stone.StoneRite;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.GreatCircle;
import com.ajustor.fmab.transmutation.Transmutation;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Des cercles qui se répondent. Le cercle de la Pierre tracé au centre, et le même tracé sur six
 * relais à vingt-quatre blocs, forment un cercle géant : il s'allume (le tracé central projeté jusqu'aux
 * relais), puis consume tout ce qui vit jusqu'aux relais, et rien au-delà, sans demander de rayon.
 * Ailleurs, un cercle de mur : en le traçant, le plan des relais s'affiche (où tracer les suivants) ;
 * complet, il lève un mur tout autour du cercle géant. Enfin, ce qui naît d'une transmutation humaine.
 * Lancer avec {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=great_circle}.
 */
public class GreatCircleScenes implements FabricClientGameTest {
	private static final BlockPos CENTER = new BlockPos(0, -60, 0);
	private static final int RADIUS = 24;
	private static final int RELAYS = 6;
	private static final BlockPos WALL_CENTER = new BlockPos(0, -60, 110);
	private static final int WALL_RADIUS = 16;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("great_circle")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("gamerule doMobSpawning false");
			sp.getServer().runCommand("time set midnight");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("gamemode spectator @p");
			context.runOnClient(mc -> RenderShowcase.hideGui(mc, true));
			sp.getServer().runCommand("tp @p 0.5 -15 0.5 0 90");
			sp.getConnection().waitForChunksRender();
			onServer(sp, p -> {
				AlchemistData data = FmabAttachments.ALCHEMIST.getOrCreate(p).withRank(Rank.ALCHEMIST);
				for (Glyph g : AlchemyRules.of(p.level().registryAccess()).glyphs()) {
					data = data.learn(g.id());
				}
				FmabAttachments.ALCHEMIST.set(p, data);
				return null;
			});

			// 1. Le cercle de la Pierre au centre, six relais autour : un cercle géant.
			Drawing drawing = ForbiddenCircles.PHILOSOPHER_STONE;
			BlockState state = inscribe(sp, CENTER, drawing);
			for (BlockPos relay : relays(CENTER, RADIUS, RELAYS)) {
				inscribe(sp, relay, drawing);
			}
			// Un cercle qui n'a pas le même tracé n'est pas un relais.
			inscribe(sp, CENTER.offset(0, 0, -12), ForbiddenCircles.HUMAN_TRANSMUTATION);
			// Les mêmes cercles à une autre taille que la géante ne se répondent pas.
			BlockPos small = new BlockPos(0, -60, -110);
			inscribe(sp, small, drawing, CircleSize.HUGE);
			for (BlockPos relay : relays(small, RADIUS, RELAYS)) {
				inscribe(sp, relay, drawing, CircleSize.HUGE);
			}
			check(onServer(sp, p -> GreatCircle.around(p.level(), small)).isEmpty(),
					"des cercles de 7 blocs ne forment pas de cercle géant : seule la taille géante le fait");
			Optional<GreatCircle.Ring> ring = onServer(sp, p -> GreatCircle.around(p.level(), CENTER));
			check(ring.isPresent(), "six relais au même tracé, tout autour, devraient former un cercle géant");
			check(ring.get().relays().size() == RELAYS, "six relais : " + ring.get().relays().size());
			check(Math.abs(ring.get().radius() - RADIUS) < 1, "le cercle géant passe par ses relais : " + ring.get().radius());
			context.waitTicks(10);

			// 2. Il s'allume : le tracé central projeté jusqu'aux relais, une colonne sur chacun.
			for (boolean red : new boolean[]{false, true}) {
				onServer(sp, p -> GreatCircleEntity.spawn(p.level(), ring.get(), red));
				sp.getServer().runCommand("tp @p 0.5 -15 0.5 0 90");
				context.waitTicks(32);
				String name = "great_circle_" + (red ? "red" : "blue");
				context.takeScreenshot(name + "_aerial");
				sp.getServer().runCommand("tp @p 0.5 -48 -38.5 0 22");
				context.waitTicks(4);
				context.takeScreenshot(name + "_side");
				sp.getServer().runCommand("kill @e[type=fmab:great_circle]");
				context.waitTicks(2);
			}

			// 3. Le sacrifice : tout ce qui vit dans le cercle géant, rien au-delà.
			for (int i = 0; i < 8; i++) {
				double a = 2 * Math.PI * i / 8 + 0.3;
				int d = 8 + (i % 3) * 5;
				sp.getServer().runCommand(String.format("summon minecraft:villager %d -60 %d {NoAI:1b}",
						(int) Math.round(Math.cos(a) * d), (int) Math.round(Math.sin(a) * d)));
			}
			sp.getServer().runCommand("summon minecraft:villager 36 -60 0 {NoAI:1b}");
			sp.getServer().runCommand("summon minecraft:villager -36 -60 0 {NoAI:1b}");
			onServer(sp, p -> {
				ItemEntity e = new ItemEntity(p.level(), CENTER.getX() + 0.5, CENTER.getY() + 0.1, CENTER.getZ() + 0.5,
						new ItemStack(FmabItems.CRYSTALLIZED_BLOOD));
				e.setDeltaMovement(0, 0, 0);
				e.setPickUpDelay(32767);
				p.level().addFreshEntity(e);
				return null;
			});
			Transmutation.Result cast = activate(sp, CENTER, state, drawing);
			check(cast == Transmutation.Result.DONE, "le cercle géant de la Pierre devrait partir sans demander de rayon : " + cast);
			context.setScreen(() -> null);
			context.waitTicks(StoneRite.AWAKENING);
			context.takeScreenshot("great_circle_stone_rite");
			context.waitTicks(StoneRite.LENGTH - StoneRite.AWAKENING + 10);
			check(villagers(sp, 30) == 0, "tout ce qui vit jusqu'aux relais devrait être consumé : " + villagers(sp, 30));
			check(villagers(sp, 40) == 2, "au-delà des relais, on vit encore : " + villagers(sp, 40));

			// 4. L'aide au placement : la craie en main, le cercle de mur choisi dans le carnet.
			sp.getServer().runCommand("time set day");
			Drawing wall = onServer(sp, p -> {
				AlchemyRules rules = AlchemyRules.of(p.level().registryAccess());
				Drawing d = rules.combinations().stream().filter(c -> c.effect().equals("fmab:wall")).findFirst()
						.flatMap(rules::simpleCircle).orElseThrow();
				Notebooks.add(p, "mur", d);
				Notebooks.select(p, Notebooks.of(p).indexOf(d));
				ItemStack chalk = new ItemStack(FmabItems.CHALK);
				chalk.set(FmabComponents.CIRCLE_SIZE, CircleSize.GREAT.blocks());
				p.setItemInHand(InteractionHand.MAIN_HAND, chalk);
				return d;
			});
			List<BlockPos> wallRelays = relays(WALL_CENTER, WALL_RADIUS, RELAYS);
			sp.getServer().runCommand(String.format("tp @p %d -59 %d 0 30", WALL_CENTER.getX(), WALL_CENTER.getZ() - 30));
			sp.getConnection().waitForChunksRender();
			BlockState wallState = inscribe(sp, WALL_CENTER, wall);
			// Le centre est tracé le premier : les relais le sont plus tard.
			context.waitTicks(2);
			inscribe(sp, wallRelays.get(0), wall);
			inscribe(sp, wallRelays.get(1), wall);
			sp.getServer().runCommand(String.format("tp @p %d -46 %d 0 35", WALL_CENTER.getX(), WALL_CENTER.getZ() - 30));
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(25);
			GreatCircle.Plan plan = context.computeOnClient(mc -> GreatCirclePlanner.centeredOn(WALL_CENTER));
			check(plan != null, "la craie en main, le plan du cercle géant devrait s'afficher autour du centre");
			check(plan.relays().size() == 2, "deux relais déjà tracés : " + plan.relays().size());
			check(plan.spots().stream().filter(s -> !s.filled()).count() == RELAYS - 2,
					"quatre emplacements restent à tracer : " + plan.spots());
			check(!plan.ready(), "deux relais ne ferment pas un cercle");
			context.runOnClient(mc -> RenderShowcase.hideGui(mc, false));
			context.waitTicks(10);
			context.takeScreenshot("great_circle_plan");
			context.runOnClient(mc -> RenderShowcase.hideGui(mc, true));
			// Les relais suivent le plan : là où il les montre.
			for (GreatCircle.Spot spot : plan.spots()) {
				if (!spot.filled()) {
					inscribe(sp, spot.pos(), wall);
				}
			}
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(25);
			GreatCircle.Plan done = context.computeOnClient(mc -> GreatCirclePlanner.centeredOn(WALL_CENTER));
			check(done != null && done.ready(), "les relais tracés aux emplacements du plan ferment le cercle géant");
			check(onServer(sp, p -> GreatCircle.around(p.level(), WALL_CENTER)).isPresent(),
					"le serveur devrait reconnaître le cercle géant que le plan a fait tracer");
			onServer(sp, p -> {
				p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				return null;
			});

			// 5. Le mur d'un cercle géant se lève tout autour, en passant par les relais.
			Transmutation.Result raised = activate(sp, WALL_CENTER, wallState, wall);
			check(raised == Transmutation.Result.DONE, "le cercle géant devrait lever son mur : " + raised);
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(30);
			long standing = onServer(sp, p -> GreatCircle.circumference(WALL_CENTER, WALL_RADIUS).stream()
					.filter(q -> !p.level().getBlockState(q.above()).isAir()).count());
			int around = GreatCircle.circumference(WALL_CENTER, WALL_RADIUS).size();
			check(standing >= around * 0.8, "le mur devrait faire le tour du cercle géant : " + standing + "/" + around);
			check(onServer(sp, p -> p.level().getBlockState(WALL_CENTER.offset(0, 1, 0)).isAir()),
					"le mur se lève sur le tour, pas au centre");
			sp.getServer().runCommand(String.format("tp @p %d -38 %d 0 50", WALL_CENTER.getX(), WALL_CENTER.getZ() - 26));
			context.waitTicks(6);
			context.takeScreenshot("great_circle_wall");

			// 6. Ce qui naît d'une transmutation humaine : la masse aux six bras.
			BlockPos stage = new BlockPos(0, -60, 200);
			sp.getServer().runCommand(String.format(java.util.Locale.ROOT, "tp @p 2.6 -58.3 %.1f 35 22", stage.getZ() - 3.1));
			sp.getConnection().waitForChunksRender();
			onServer(sp, p -> {
				FailedTransmutationEntity being = FmabEntities.FAILED_TRANSMUTATION.create(p.level(), EntitySpawnReason.COMMAND);
				being.setNoAi(true);
				being.setPos(stage.getX() + 0.5, stage.getY(), stage.getZ() + 0.5);
				being.setYRot(180);
				being.yBodyRot = 180;
				being.yHeadRot = 180;
				p.level().addFreshEntity(being);
				return null;
			});
			context.waitTicks(20);
			check(onServer(sp, p -> p.level().getEntitiesOfClass(FailedTransmutationEntity.class,
					new AABB(stage).inflate(4)).size()) == 1, "la créature devrait être là");
			context.takeScreenshot("failed_transmutation_front");
			sp.getServer().runCommand(String.format(java.util.Locale.ROOT, "tp @p -3.4 -58.8 %.1f -90 12", stage.getZ() + 0.5));
			context.waitTicks(4);
			context.takeScreenshot("failed_transmutation_profile");
			context.runOnClient(mc -> RenderShowcase.hideGui(mc, false));
		}
	}

	/** Des relais régulièrement répartis sur un cercle. */
	private static List<BlockPos> relays(BlockPos center, int radius, int count) {
		List<BlockPos> out = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			double a = 2 * Math.PI * i / count;
			out.add(center.offset((int) Math.round(Math.cos(a) * radius), 0, (int) Math.round(Math.sin(a) * radius)));
		}
		return out;
	}

	private static Transmutation.Result activate(TestSingleplayerContext sp, BlockPos circle, BlockState state,
			Drawing drawing) {
		return onServer(sp, p -> {
			Concentration.set(p, Concentration.max(p));
			return Transmutation.activate(p.level(), circle, state, drawing, p, CircleSize.GREAT);
		});
	}

	/** Un cercle tracé à la taille des cercles géants. */
	private static BlockState inscribe(TestSingleplayerContext sp, BlockPos pos, Drawing drawing) {
		return inscribe(sp, pos, drawing, CircleSize.GREAT);
	}

	private static BlockState inscribe(TestSingleplayerContext sp, BlockPos pos, Drawing drawing, CircleSize size) {
		return onServer(sp, p -> {
			ServerLevel level = p.level();
			BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
					.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
			level.setBlockAndUpdate(pos, state);
			if (level.getBlockEntity(pos) instanceof TransmutationCircleBlockEntity be) {
				be.setDrawing(drawing);
				be.setSize(size);
			}
			return state;
		});
	}

	private static int villagers(TestSingleplayerContext sp, int reach) {
		return onServer(sp, p -> p.level().getEntitiesOfClass(Villager.class, new AABB(CENTER).inflate(reach, 4, reach),
				Villager::isAlive).size());
	}

	private static <T> T onServer(TestSingleplayerContext sp, Function<ServerPlayer, T> action) {
		return sp.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
	}
}
