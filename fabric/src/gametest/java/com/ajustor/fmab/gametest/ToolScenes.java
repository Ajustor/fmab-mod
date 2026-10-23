package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.Treatise;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.Transmutation;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * L'alchimie comme outil, avec les cercles mêmes du Traité : le bûcheron abat un arbre mais pas une
 * poutre, la moisson récolte et replante, l'engrais fait pousser contre de la poudre d'os, le tunnel
 * traverse une paroi, le pont franchit un fossé, le terrassement rabote une bosse et comble un trou.
 * Lancer avec {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=tools}.
 */
public class ToolScenes implements FabricClientGameTest {
	private static final BlockPos CIRCLE = new BlockPos(0, -60, 3);
	private static final int GROUND = -61;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("tools")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("time set 6000");
			sp.getConnection().waitForChunksRender();
			context.waitTicks(20);
			// Un Alchimiste d'État qui comprend tous les glyphes, les mains et le sac vides.
			sp.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				AlchemistData data = AlchemistData.NEW.withRank(Rank.STATE);
				for (Glyph g : AlchemyRules.of(p.level().registryAccess()).glyphs()) {
					data = data.learn(g.id());
				}
				FmabAttachments.ALCHEMIST.set(p, data);
				p.getInventory().clearContent();
			});
			sp.getServer().runCommand("tp @p 0.5 -60 -2.5 0 35");
			context.runOnClient(mc -> RenderShowcase.hideGui(mc, true));

			felling(context, sp);
			harvest(context, sp);
			tunnel(context, sp);
			bridge(context, sp);
			leveling(context, sp);
			context.runOnClient(mc -> RenderShowcase.hideGui(mc, false));
		}
	}

	/** Un chêne à trois blocs du cercle tombe tout entier ; une poutre de rondins, sans feuilles, reste. */
	private static void felling(ClientGameTestContext context, TestSingleplayerContext sp) {
		clear(context, sp);
		sp.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
			for (int y = -60; y <= -56; y++) {
				level.setBlockAndUpdate(new BlockPos(3, y, 5), Blocks.OAK_LOG.defaultBlockState());
				level.setBlockAndUpdate(new BlockPos(-3, y, 5), Blocks.OAK_LOG.defaultBlockState());
			}
			for (BlockPos p : BlockPos.betweenClosed(new BlockPos(2, -57, 4), new BlockPos(4, -55, 6))) {
				if (level.getBlockState(p).isAir()) {
					level.setBlockAndUpdate(p, leaves);
				}
			}
		});
		check(cast(context, sp, Direction.UP, CIRCLE, "treatise.fmab.everyday.recycling"), "le bûcheron devrait partir");
		boolean felled = sp.getServer().computeOnServer(server -> server.overworld()
				.getBlockState(new BlockPos(3, -58, 5)).isAir() && server.overworld().getBlockState(new BlockPos(3, -56, 5)).isAir());
		check(felled, "le chêne devrait être abattu, cime comprise");
		boolean beam = sp.getServer().computeOnServer(server -> server.overworld()
				.getBlockState(new BlockPos(-3, -58, 5)).is(Blocks.OAK_LOG));
		check(beam, "la poutre sans feuilles devrait rester debout");
		check(count(sp, Items.OAK_LOG) >= 5, "les bûches devraient tomber sur le cercle : " + count(sp, Items.OAK_LOG));
		context.takeScreenshot("tools_felling");
	}

	/** Le blé mûr se récolte et se replante, le blé vert reste ; puis l'engrais le fait pousser. */
	private static void harvest(ClientGameTestContext context, TestSingleplayerContext sp) {
		clear(context, sp);
		CropBlock wheat = (CropBlock) Blocks.WHEAT;
		sp.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			level.setBlockAndUpdate(new BlockPos(0, GROUND, 6), Blocks.WATER.defaultBlockState());
			for (int x = -2; x <= 2; x++) {
				BlockPos soil = new BlockPos(x, GROUND, 5);
				level.setBlockAndUpdate(soil, Blocks.FARMLAND.defaultBlockState());
				level.setBlockAndUpdate(soil.above(), wheat.getStateForAge(x == 2 ? 3 : 7));
			}
		});
		check(cast(context, sp, Direction.UP, CIRCLE, "treatise.fmab.everyday.harvest"), "la moisson devrait partir");
		String field = sp.getServer().computeOnServer(server -> {
			ServerLevel level = server.overworld();
			StringBuilder ages = new StringBuilder();
			for (int x = -2; x <= 2; x++) {
				BlockState s = level.getBlockState(new BlockPos(x, -60, 5));
				ages.append(s.is(Blocks.WHEAT) ? String.valueOf(s.getValue(CropBlock.AGE)) : "-");
			}
			return ages.toString();
		});
		check(field.equals("00003"), "le blé mûr devrait être replanté et le vert laissé : " + field);
		check(count(sp, Items.WHEAT) >= 4, "la moisson devrait tomber sur le cercle");
		context.takeScreenshot("tools_harvest");

		sp.getServer().runCommand("kill @e[type=item]");
		drop(sp, new ItemStack(Items.BONE_MEAL, 2));
		context.waitTicks(5);
		check(cast(context, sp, Direction.UP, CIRCLE, "treatise.fmab.everyday.growth"), "l'engrais devrait partir");
		int grown = sp.getServer().computeOnServer(server -> {
			int n = 0;
			for (int x = -2; x <= 2; x++) {
				BlockState s = server.overworld().getBlockState(new BlockPos(x, -60, 5));
				n += s.is(Blocks.WHEAT) && s.getValue(CropBlock.AGE) > (x == 2 ? 3 : 0) ? 1 : 0;
			}
			return n;
		});
		check(grown == 2, "deux poudres d'os, deux plants qui poussent : " + grown);
		check(count(sp, Items.BONE_MEAL) == 0, "la poudre d'os devrait être consommée");
		context.takeScreenshot("tools_growth");
	}

	/** Un cercle sur une paroi de pierre la traverse d'un tunnel de trois sur trois. */
	private static void tunnel(ClientGameTestContext context, TestSingleplayerContext sp) {
		clear(context, sp);
		sp.getServer().runCommand("fill -3 -60 -2 3 -55 2 stone");
		sp.getServer().runCommand("fill -1 -59 -1 -1 -59 -1 iron_ore");
		BlockPos circle = new BlockPos(0, -59, 3);
		check(cast(context, sp, Direction.SOUTH, circle, "treatise.fmab.everyday.tunnel"), "le tunnel devrait partir");
		boolean bored = sp.getServer().computeOnServer(server -> {
			ServerLevel level = server.overworld();
			for (BlockPos p : BlockPos.betweenClosed(new BlockPos(-1, -60, -2), new BlockPos(1, -58, 2))) {
				if (!level.getBlockState(p).isAir()) {
					return false;
				}
			}
			return level.getBlockState(new BlockPos(2, -59, 0)).is(Blocks.STONE);
		});
		check(bored, "la paroi devrait être percée de part en part, sur trois de large seulement");
		check(count(sp, Items.RAW_IRON) >= 1, "le minerai pris dans la paroi devrait tomber sur le cercle");
		context.takeScreenshot("tools_tunnel");
	}

	/** Le pont franchit un fossé, fait des pierres posées sur le cercle. */
	private static void bridge(ClientGameTestContext context, TestSingleplayerContext sp) {
		clear(context, sp);
		sp.getServer().runCommand("fill -3 -63 5 3 -61 12 air");
		drop(sp, new ItemStack(Items.COBBLESTONE, 30));
		context.waitTicks(5);
		check(cast(context, sp, Direction.UP, CIRCLE, "treatise.fmab.everyday.bridge"), "le pont devrait partir");
		boolean deck = sp.getServer().computeOnServer(server -> {
			ServerLevel level = server.overworld();
			for (int z = 5; z <= 11; z++) {
				for (int x = -1; x <= 1; x++) {
					if (!level.getBlockState(new BlockPos(x, GROUND, z)).is(Blocks.COBBLESTONE)) {
						return false;
					}
				}
			}
			return true;
		});
		check(deck, "un tablier de trois de large devrait franchir le fossé");
		check(count(sp, Items.COBBLESTONE) <= 30 - 21, "le pont devrait être fait des pierres du cercle");
		context.takeScreenshot("tools_bridge");
	}

	/** Le terrassement rabote une bosse de pierre et en comble un trou. */
	private static void leveling(ClientGameTestContext context, TestSingleplayerContext sp) {
		clear(context, sp);
		sp.getServer().runCommand("fill 2 -60 5 2 -58 5 stone");
		sp.getServer().runCommand("fill -2 -62 5 -2 -61 5 air");
		check(cast(context, sp, Direction.UP, CIRCLE, "treatise.fmab.everyday.leveling"), "le terrassement devrait partir");
		String ground = sp.getServer().computeOnServer(server -> {
			ServerLevel level = server.overworld();
			return "bosse=" + level.getBlockState(new BlockPos(2, -59, 5)).isAir()
					+ " trou=" + !level.getBlockState(new BlockPos(-2, GROUND, 5)).isAir();
		});
		check(ground.equals("bosse=true trou=true"), "le terrain devrait être de niveau : " + ground);
		context.takeScreenshot("tools_leveling");
	}

	/** Le cercle d'exemple de cette page du Traité. */
	private static Drawing example(String key) {
		try (Reader in = new InputStreamReader(Objects.requireNonNull(
				ToolScenes.class.getResourceAsStream("/assets/fmab/treatise/treatise.json")), StandardCharsets.UTF_8)) {
			return Treatise.parse(JsonParser.parseReader(in).getAsJsonObject()).chapters().stream()
					.flatMap(c -> c.pages().stream())
					.filter(p -> p instanceof Treatise.ExamplePage e && e.textKey().equals(key))
					.map(p -> ((Treatise.ExamplePage) p).drawing())
					.findFirst().orElseThrow(() -> new AssertionError("pas d'exemple " + key));
		} catch (java.io.IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
	}

	/** Trace le cercle à la craie (au sol face au sud, ou sur la face {@code clicked} d'une paroi) et pose la paume dessus. */
	private static boolean cast(ClientGameTestContext context, TestSingleplayerContext sp, Direction clicked, BlockPos at,
			String page) {
		Drawing drawing = example(page);
		Transmutation.Result result = sp.getServer().computeOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
			ServerLevel level = p.level();
			BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
					.stateFor(clicked, Direction.SOUTH, CircleMedium.CHALK);
			level.setBlockAndUpdate(at, state);
			if (level.getBlockEntity(at) instanceof TransmutationCircleBlockEntity be) {
				be.setDrawing(drawing);
			}
			Concentration.set(p, Concentration.max(p));
			return Transmutation.activate(level, at, state, drawing, p, CircleSize.NORMAL);
		});
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(20);
		if (result != Transmutation.Result.DONE) {
			System.out.println(String.format(Locale.ROOT, "[tools] %s : %s", page, result));
		}
		return result == Transmutation.Result.DONE;
	}

	/** Combien d'objets de ce type traînent autour du cercle. */
	private static int count(TestSingleplayerContext sp, Item item) {
		return sp.getServer().computeOnServer(server -> server.overworld()
				.getEntitiesOfClass(ItemEntity.class, new AABB(CIRCLE).inflate(4), e -> e.getItem().is(item))
				.stream().mapToInt(e -> e.getItem().getCount()).sum());
	}

	/** Un terrain d'herbe neuf, sans objet qui traîne. */
	private static void clear(ClientGameTestContext context, TestSingleplayerContext sp) {
		sp.getServer().runCommand("fill -8 -60 -4 8 -50 14 air");
		sp.getServer().runCommand("fill -8 -63 -4 8 -62 14 dirt");
		sp.getServer().runCommand("fill -8 -61 -4 8 -61 14 grass_block");
		sp.getServer().runCommand("kill @e[type=item]");
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(5);
	}

	/** Pose des objets au centre du cercle, immobiles et hors de portée du joueur. */
	private static void drop(TestSingleplayerContext sp, ItemStack stack) {
		sp.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			ItemEntity entity = new ItemEntity(level, CIRCLE.getX() + 0.5, CIRCLE.getY() + 0.1, CIRCLE.getZ() + 0.5, stack);
			entity.setDeltaMovement(0, 0, 0);
			entity.setPickUpDelay(32767);
			level.addFreshEntity(entity);
		});
	}
}
