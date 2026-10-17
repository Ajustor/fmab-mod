package com.ajustor.fmab.gametest;

import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Le butin : un point de sang posé a son coffre, que sa table de butin remplit à l'ouverture ; et un
 * homonculus abattu par un joueur laisse des éclats de sa Pierre. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=loot}.
 */
public class LootScenes implements FabricClientGameTest {
	private static final int GROUND = -60;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("loot")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getConnection().waitForChunksRender();
			String crest = sp.getServer().computeOnServer(server -> {
				try {
					server.getCommands().getDispatcher().execute("place structure fmab:blood_crest 0 %d 20".formatted(GROUND),
							server.createCommandSourceStack().withSuppressedOutput());
				} catch (CommandSyntaxException e) {
					return "refusé : " + e.getMessage();
				}
				var p = server.getPlayerList().getPlayers().getFirst();
				var level = p.level();
				for (BlockPos pos : BlockPos.betweenClosed(-20, GROUND - 2, 0, 20, GROUND + 6, 40)) {
					if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
						chest.unpackLootTable(p);
						int items = 0;
						for (int i = 0; i < chest.getContainerSize(); i++) {
							items += chest.getItem(i).getCount();
						}
						return "coffre " + items;
					}
				}
				return "pas de coffre";
			});
			check(crest.startsWith("coffre ") && !crest.equals("coffre 0"),
					"le point de sang devrait avoir un coffre garni : " + crest);

			int shards = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				var level = p.level();
				var greed = FmabEntities.GREED.create(level, EntitySpawnReason.COMMAND);
				greed.snapTo(p.getX() + 3, p.getY(), p.getZ(), 0, 0);
				level.addFreshEntity(greed);
				// Un coup qui ne laisse pas la Pierre le reconstituer, porté par le joueur.
				greed.hurtServer(level, new DamageSource(level.damageSources().damageTypes.getOrThrow(DamageTypes.GENERIC_KILL), p),
						Float.MAX_VALUE);
				return level.getEntitiesOfClass(ItemEntity.class, new AABB(greed.blockPosition()).inflate(4),
						e -> e.getItem().is(FmabItems.RED_STONE_SHARD)).stream().mapToInt(e -> e.getItem().getCount()).sum();
			});
			check(shards >= 1, "Greed abattu par un joueur devrait laisser des éclats de sa Pierre : " + shards);
		}
	}
}
