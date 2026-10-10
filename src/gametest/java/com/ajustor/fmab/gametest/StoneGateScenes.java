package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.ForbiddenCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.client.screen.BargainScreen;
import com.ajustor.fmab.client.screen.StoneChoiceScreen;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.GateOfTruth;
import com.ajustor.fmab.item.PhilosopherStoneItem;
import com.ajustor.fmab.network.BargainAnswerPayload;
import com.ajustor.fmab.network.StoneChosenPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Transmutation;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Map;

/**
 * La transmutation humaine, Pierre philosophale en main : l'alchimiste choisit. D'abord créer un
 * être (une âme de la Pierre, et le corps vit), puis ouvrir la Porte et racheter à la Vérité ce
 * qu'elle a pris. Chaque étape est vérifiée et capturée.
 */
public class StoneGateScenes implements FabricClientGameTest {
	private static final BlockPos CIRCLE = new BlockPos(0, -60, 3);
	private static final Map<Item, Integer> BODY = Map.of(Items.WATER_BUCKET, 1, Items.COAL, 8, Items.BONE_MEAL, 4,
			Items.GLOWSTONE_DUST, 2, Items.GUNPOWDER, 2, Items.IRON_NUGGET, 1, Items.QUARTZ, 1);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("stonegate")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("tp @p 0.5 -60 -0.5 0 35");
			sp.getConnection().waitForChunksRender();
			server(context, sp, (level, p) -> {
				AlchemistData data = p.getAttachedOrCreate(FmabAttachments.ALCHEMIST).withRank(Rank.GATE);
				for (Glyph g : AlchemyRules.of(level.registryAccess()).glyphs()) {
					data = data.learn(g.id());
				}
				p.setAttached(FmabAttachments.ALCHEMIST, data);
				p.getInventory().clearContent();
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FmabItems.PHILOSOPHER_STONE));
				return null;
			});
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, false);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});

			// 1. Pierre en main, le cercle s'éveille : il faut choisir.
			body(context, sp);
			cast(context, sp);
			context.waitForScreen(StoneChoiceScreen.class);
			context.takeScreenshot("stonegate_00_choice");

			// 2. Créer un être : une âme de la Pierre, et le corps vit.
			int before = souls(context, sp);
			context.runOnClient(mc -> ClientPlayNetworking.send(new StoneChosenPayload("being")));
			context.setScreen(() -> null);
			sp.getConnection().waitForServerboundPackets();
			context.waitTicks(20);
			check(server(context, sp, (level, p) -> level.getEntitiesOfClass(Villager.class, around(),
					v -> v.hasCustomName()).size()) == 1, "un être devrait être né du cercle");
			check(souls(context, sp) == before - 1, "la Pierre devrait avoir donné une âme");
			check(server(context, sp, (level, p) -> level.getEntitiesOfClass(ItemEntity.class, around()).stream()
					.noneMatch(e -> e.getItem().is(Items.COAL))), "les ingrédients devraient être consommés");
			check(server(context, sp, (level, p) -> p.getAttachedOrCreate(FmabAttachments.GATE).visit().isEmpty()),
					"la Porte ne devrait pas s'ouvrir");
			context.takeScreenshot("stonegate_01_being_created");
			server(context, sp, (level, p) -> {
				level.getEntitiesOfClass(Villager.class, around()).forEach(v -> v.discard());
				return null;
			});

			// 3. Ouvrir la Porte : la Vérité prend son péage, puis propose le marché.
			body(context, sp);
			cast(context, sp);
			context.waitForScreen(StoneChoiceScreen.class);
			context.runOnClient(mc -> ClientPlayNetworking.send(new StoneChosenPayload("gate")));
			context.setScreen(() -> null);
			context.waitFor(mc -> mc.gui.screen() instanceof BargainScreen, 20 * 60);
			context.takeScreenshot("stonegate_02_bargain");
			BodyPart lost = server(context, sp, (level, p) ->
					p.getAttachedOrCreate(FmabAttachments.GATE).lost().iterator().next());
			int stoneBefore = souls(context, sp);
			context.runOnClient(mc -> ClientPlayNetworking.send(new BargainAnswerPayload(lost.serializedName())));
			sp.getConnection().waitForServerboundPackets();
			context.waitTicks(10);
			GateState gate = server(context, sp, (level, p) -> p.getAttachedOrCreate(FmabAttachments.GATE));
			check(!gate.lost().contains(lost), "la partie rachetée devrait être rendue : " + lost);
			check(souls(context, sp) <= stoneBefore - 15, "la Pierre devrait avoir payé");
			context.takeScreenshot("stonegate_03_bought_back");

			// 4. On s'en va : la visite reprend, l'alchimiste se réveille sur son cercle.
			context.setScreen(() -> null);
			context.waitFor(mc -> mc.player != null && mc.player.level().dimension() != GateOfTruth.WHITE_SPACE,
					20 * 30);
			context.waitTicks(20);
			check(server(context, sp, (level, p) -> p.getAttachedOrCreate(FmabAttachments.GATE).visit().isEmpty()),
					"la visite devrait être terminée");
			context.takeScreenshot("stonegate_04_back_home");
		}
	}

	@FunctionalInterface
	private interface OnServer<T> {
		T run(ServerLevel level, ServerPlayer player);
	}

	private static <T> T server(ClientGameTestContext context, TestSingleplayerContext sp, OnServer<T> action) {
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

	private static int souls(ClientGameTestContext context, TestSingleplayerContext sp) {
		return server(context, sp, (level, p) -> {
			int souls = 0;
			for (ItemStack stack : p.getInventory()) {
				if (stack.is(FmabItems.PHILOSOPHER_STONE)) {
					souls += PhilosopherStoneItem.souls(stack);
				}
			}
			return souls;
		});
	}

	/** Les ingrédients d'un corps, posés sur le cercle. */
	private static void body(ClientGameTestContext context, TestSingleplayerContext sp) {
		server(context, sp, (level, p) -> {
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
		server(context, sp, (level, p) -> {
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
		context.waitTicks(5);
	}

	private static AABB around() {
		return new AABB(CIRCLE).inflate(3, 2, 3);
	}
}
