package com.ajustor.fmab.fabric;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.world.FlatDimensions;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGeneratorReturnType;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiLevelLoadEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import com.seibel.distanthorizons.api.objects.data.IDhApiFullDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Distant Horizons (facultatif) ne génère rien dans les dimensions du mod : un plateau d'une couche
 * se décrit tel quel, colonne par colonne. Sans cela, il y faisait tourner la génération du jeu en
 * tâche de fond, pour un sol qu'on connaît d'avance, et une sauvegarde du monde a pu l'attendre
 * sans fin.
 */
final class DistantHorizonsCompat {
	private DistantHorizonsCompat() {
	}

	static void install() {
		DhApi.events.bind(DhApiLevelLoadEvent.class, new DhApiLevelLoadEvent() {
			@Override
			public void onLevelLoad(DhApiEventParam<EventParam> event) {
				IDhApiLevelWrapper level = event.value.levelWrapper;
				FlatDimensions.floor(level.getDimensionName()).ifPresent(floor -> register(level, floor));
			}
		});
	}

	private static void register(IDhApiLevelWrapper level, String floor) {
		try {
			IDhApiBiomeWrapper biome = DhApi.Delayed.wrapperFactory.getBiomeWrapper(level.getDimensionName(), level);
			IDhApiBlockStateWrapper ground = DhApi.Delayed.wrapperFactory.getDefaultBlockStateWrapper(floor, level);
			IDhApiBlockStateWrapper air = DhApi.Delayed.wrapperFactory.getAirBlockStateWrapper();
			DhApiResult<Void> result = DhApi.worldGenOverrides.registerWorldGeneratorOverride(level,
					new FlatFloor(level.getMinHeight(), level.getMaxHeight(), ground, air, biome));
			if (!result.success) {
				Fmab.LOGGER.warn("Distant Horizons garde son générateur pour {} : {}", level.getDimensionName(),
						result.message);
			}
		} catch (Exception e) {
			Fmab.LOGGER.warn("Distant Horizons garde son générateur pour {}", level.getDimensionName(), e);
		}
	}

	/** Un sol d'un bloc au fond du monde, de l'air au-dessus, en pleine lumière du ciel. */
	private record FlatFloor(int minY, int maxY, IDhApiBlockStateWrapper ground, IDhApiBlockStateWrapper air,
			IDhApiBiomeWrapper biome) implements IDhApiWorldGenerator {
		private static final int SKY = 15;

		@Override
		public EDhApiWorldGeneratorReturnType getReturnType() {
			return EDhApiWorldGeneratorReturnType.API_DATA_SOURCES;
		}

		@Override
		public boolean runApiValidation() {
			return false;
		}

		@Override
		public CompletableFuture<Void> generateLod(int chunkPosMinX, int chunkPosMinZ, int lodPosX, int lodPosZ,
				byte detailLevel, IDhApiFullDataSource source, EDhApiDistantGeneratorMode mode,
				ExecutorService pool, Consumer<IDhApiFullDataSource> done) {
			return CompletableFuture.runAsync(() -> {
				List<DhApiTerrainDataPoint> column = new ArrayList<>(2);
				int width = source.getWidthInDataColumns();
				for (int x = 0; x < width; x++) {
					for (int z = 0; z < width; z++) {
						column.clear();
						column.add(DhApiTerrainDataPoint.create(detailLevel, 0, 0, minY, minY + 1, ground, biome));
						column.add(DhApiTerrainDataPoint.create(detailLevel, 0, SKY, minY + 1, maxY, air, biome));
						source.setApiDataPointColumn(x, z, EDhApiWorldGenerationStep.LIGHT, column);
					}
				}
				done.accept(source);
			}, pool);
		}

		@Override
		public void preGeneratorTaskStart() {
		}

		@Override
		public void close() {
		}
	}
}
