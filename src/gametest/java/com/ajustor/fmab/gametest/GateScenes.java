package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.ForbiddenCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.gate.GateOfTruth;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Transmutation;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * Une visite de la Porte, du début à la fin, sans Pierre : les bras jaillissent du cercle et
 * l'aspirent, la Vérité l'accueille, la Porte s'ouvre et ses bras viennent le chercher, le savoir
 * déferle (et l'alchimiste comprend alors tous les glyphes), la Vérité prend son péage, le réveil.
 * Chaque moment est capturé.
 */
public class GateScenes implements FabricClientGameTest {
	private static final BlockPos CIRCLE = new BlockPos(0, -60, 3);
	private static final Map<Item, Integer> BODY = Map.of(Items.WATER_BUCKET, 1, Items.COAL, 8, Items.BONE_MEAL, 4,
			Items.GLOWSTONE_DUST, 2, Items.GUNPOWDER, 2, Items.IRON_NUGGET, 1, Items.QUARTZ, 1);
	/** Les instants de la visite (en ticks depuis l'arrivée) qu'on capture, et leur nom. */
	private static final Object[][] MOMENTS = {
			{20, "01_arrival"}, {120, "02_presentation"}, {194, "03_gate_opens"}, {207, "04_arms_reach"},
			{226, "05_arms_hold"}, {262, "06_knowledge_eye"}, {277, "06b_knowledge_dive"}, {290, "07_knowledge_torrent"},
			{310, "08_knowledge_flood"}, {336, "09_toll"}, {358, "09b_gate_closed"}};

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("gate")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("tp @p 0.5 -60 -0.5 0 35");
			sp.getConnection().waitForChunksRender();
			// Le cercle doit être compris pour réagir : on connaît tout, le temps de l'activer.
			server(sp, (level, p) -> {
				AlchemistData data = p.getAttachedOrCreate(FmabAttachments.ALCHEMIST).withRank(Rank.GATE);
				for (Glyph g : AlchemyRules.of(level.registryAccess()).glyphs()) {
					data = data.learn(g.id());
				}
				p.setAttached(FmabAttachments.ALCHEMIST, data.withConcentration(data.maxConcentration()));
				p.getInventory().clearContent();
				return null;
			});
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});

			// 1. Le cercle s'active : les bras jaillissent du sol et l'aspirent.
			body(context, sp);
			cast(context, sp);
			for (int i = 0; i < 100 && !server(sp, (level, p) ->
					p.getAttachedOrCreate(FmabAttachments.GATE).visit().isPresent()); i++) {
				context.waitTick();
			}
			check(server(sp, (level, p) -> p.getAttachedOrCreate(FmabAttachments.GATE).visit().isPresent()),
					"le cercle devrait happer l'alchimiste");
			// Pendant l'aspiration, il oublie tout : c'est la Porte qui doit tout lui apprendre.
			server(sp, (level, p) -> {
				p.setAttached(FmabAttachments.ALCHEMIST, p.getAttachedOrCreate(FmabAttachments.ALCHEMIST).forgetAll());
				return null;
			});
			context.waitTicks(15);
			context.takeScreenshot("gate_00a_pull_hands");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(10);
			context.takeScreenshot("gate_00b_pull_hands_behind");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(18);
			context.takeScreenshot("gate_00c_pull_dark");

			// 2. La visite, instant par instant.
			context.waitFor(mc -> mc.player != null && mc.player.level().dimension() == GateOfTruth.WHITE_SPACE,
					20 * 10);
			for (Object[] moment : MOMENTS) {
				int at = (Integer) moment[0];
				while (visitTicks(sp) < at) {
					context.waitTick();
				}
				String name = (String) moment[1];
				context.takeScreenshot("gate_" + name);
				if (name.equals("05_arms_hold")) {
					// De dos, pour voir les bras le tenir.
					context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
					context.waitTicks(2);
					context.takeScreenshot("gate_05b_arms_hold_behind");
					context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
				}
				if (name.equals("09b_gate_closed")) {
					// De haut et de côté : la Porte refermée, et plus rien derrière elle.
					context.runOnClient(mc -> {
						mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
						mc.player.setXRot(35);
						mc.player.setYRot(mc.player.getYRot() + 60);
					});
					context.waitTicks(2);
					context.takeScreenshot("gate_09c_gate_closed_side");
					context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
				}
				if (name.equals("08_knowledge_flood")) {
					int total = server(sp, (level, p) -> AlchemyRules.of(level.registryAccess()).glyphs().size());
					int known = server(sp, (level, p) -> p.getAttachedOrCreate(FmabAttachments.ALCHEMIST).known().size());
					check(known == total, "le savoir de la Porte devrait apprendre tous les glyphes : " + known + "/" + total);
				}
			}

			// 3. Le réveil sur le cercle.
			context.waitFor(mc -> mc.player != null && mc.player.level().dimension() != GateOfTruth.WHITE_SPACE,
					20 * 30);
			context.waitTicks(20);
			check(server(sp, (level, p) -> p.getAttachedOrCreate(FmabAttachments.GATE).visit().isEmpty()),
					"la visite devrait être terminée");
			context.takeScreenshot("gate_10_back_home");
		}
	}

	/** Où en est la visite : -1 tant qu'il n'est pas devant sa Porte. */
	private static int visitTicks(TestSingleplayerContext sp) {
		return server(sp, (level, p) -> p.getAttachedOrCreate(FmabAttachments.GATE).visit()
				.map(GateState.Visit::ticks).orElse(Integer.MAX_VALUE));
	}

	@FunctionalInterface
	private interface OnServer<T> {
		T run(ServerLevel level, ServerPlayer player);
	}

	private static <T> T server(TestSingleplayerContext sp, OnServer<T> action) {
		return sp.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			return action.run(player.level(), player);
		});
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	/** Les ingrédients d'un corps, posés sur le cercle. */
	private static void body(ClientGameTestContext context, TestSingleplayerContext sp) {
		server(sp, (level, p) -> {
			BODY.forEach((item, count) -> {
				ItemEntity e = new ItemEntity(level, CIRCLE.getX() + 0.5, CIRCLE.getY() + 0.1, CIRCLE.getZ() + 0.5,
						new ItemStack(item, count));
				e.setDeltaMovement(0, 0, 0);
				e.setNeverPickUp();
				level.addFreshEntity(e);
			});
			return null;
		});
		context.waitTicks(5);
	}

	/** Le cercle de la transmutation humaine, tracé et activé paume posée. */
	private static void cast(ClientGameTestContext context, TestSingleplayerContext sp) {
		server(sp, (level, p) -> {
			BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
					.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
			level.setBlockAndUpdate(CIRCLE, state);
			if (level.getBlockEntity(CIRCLE) instanceof TransmutationCircleBlockEntity be) {
				be.setDrawing(ForbiddenCircles.HUMAN_TRANSMUTATION);
			}
			return Transmutation.activate(level, CIRCLE, state, ForbiddenCircles.HUMAN_TRANSMUTATION, p,
					CircleSize.NORMAL);
		});
		sp.getConnection().waitForClientboundPackets();
	}
}
