package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.ForbiddenCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.gate.GateOfTruth;
import com.ajustor.fmab.homunculus.Belly;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.Transmutation;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Avalé par Gluttony, on en sort par la Porte, comme Edward : un cercle de transmutation humaine tracé
 * dans le Ventre emporte l'alchimiste dans l'Espace blanc, et la Vérité le rend là où Gluttony l'avait
 * avalé (et non dans le Ventre, ni au point d'apparition du monde). Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=belly}.
 */
public class BellyScenes implements FabricClientGameTest {
	private static final BlockPos SWALLOWED_AT = new BlockPos(24, -60, 24);
	private static final Map<Item, Integer> BODY = Map.of(Items.WATER_BUCKET, 1, Items.COAL, 8, Items.BONE_MEAL, 4,
			Items.GLOWSTONE_DUST, 2, Items.GUNPOWDER, 2, Items.IRON_NUGGET, 1, Items.QUARTZ, 1);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("belly")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getConnection().waitForChunksRender();
			// Un Initié qui comprend tous les glyphes, que les restes du Ventre ne peuvent pas tuer.
			onServer(sp, p -> {
				AlchemistData data = FmabAttachments.ALCHEMIST.getOrCreate(p).withRank(Rank.GATE);
				for (Glyph g : AlchemyRules.of(p.level().registryAccess()).glyphs()) {
					data = data.learn(g.id());
				}
				FmabAttachments.ALCHEMIST.set(p, data);
				p.getInventory().clearContent();
				p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 600, 4, false, false));
				FmabAttachments.SWALLOWED.set(p, new Belly.Swallowed(Level.OVERWORLD.identifier().toString(), SWALLOWED_AT, -1, List.of()));
				return null;
			});
			context.waitFor(mc -> mc.player != null && mc.player.level().dimension() == Belly.BELLY, 20 * 10);
			context.waitTicks(20);
			context.takeScreenshot("belly_00_swallowed");

			BlockPos circle = onServer(sp, p -> {
				ServerLevel belly = p.level();
				BlockPos at = p.blockPosition().relative(p.getDirection(), 2);
				BODY.forEach((item, count) -> {
					ItemEntity e = new ItemEntity(belly, at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, new ItemStack(item, count));
					e.setDeltaMovement(0, 0, 0);
					e.setNeverPickUp();
					belly.addFreshEntity(e);
				});
				return at;
			});
			context.waitTicks(5);
			Transmutation.Result result = onServer(sp, p -> {
				ServerLevel belly = p.level();
				BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
						.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
				belly.setBlockAndUpdate(circle, state);
				if (belly.getBlockEntity(circle) instanceof TransmutationCircleBlockEntity be) {
					be.setDrawing(ForbiddenCircles.HUMAN_TRANSMUTATION);
				}
				Concentration.set(p, Concentration.max(p));
				return Transmutation.activate(belly, circle, state, ForbiddenCircles.HUMAN_TRANSMUTATION, p, CircleSize.NORMAL);
			});
			check(result == Transmutation.Result.DONE, "le cercle humain devrait partir dans le Ventre : " + result);
			check(onServer(sp, p -> !Belly.swallowed(p)), "le Ventre devrait lâcher l'alchimiste");

			context.waitFor(mc -> mc.player != null && mc.player.level().dimension() == GateOfTruth.WHITE_SPACE, 20 * 15);
			context.takeScreenshot("belly_01_gate");
			context.waitFor(mc -> mc.player != null && mc.player.level().dimension() != GateOfTruth.WHITE_SPACE, 20 * 60);
			context.waitTicks(40);
			String where = onServer(sp, p -> p.level().dimension().identifier() + " " + p.blockPosition().toShortString());
			check(onServer(sp, p -> p.level().dimension() == Level.OVERWORLD
					&& p.blockPosition().distManhattan(SWALLOWED_AT) <= 4), "la Vérité devrait le rendre là où Gluttony l'avait avalé : " + where);
			check(onServer(sp, p -> FmabAttachments.GATE.getOrCreate(p).visit().isEmpty()), "la visite devrait être finie");
			context.takeScreenshot("belly_02_home");
		}
	}

	private static <T> T onServer(TestSingleplayerContext sp, Function<ServerPlayer, T> action) {
		return sp.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
	}
}
