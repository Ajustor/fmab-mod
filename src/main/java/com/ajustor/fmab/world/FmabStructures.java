package com.ajustor.fmab.world;

import com.ajustor.fmab.Fmab;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Types de structure et de pièce du mod. Leur placement (biomes, fréquence) est en JSON :
 * {@code data/fmab/worldgen/structure} et {@code structure_set}.
 */
public final class FmabStructures {
	public static final StructurePieceType PROCEDURAL_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
			Fmab.id("procedural"), (StructurePieceType.ContextlessType) ProceduralPiece::new);

	public static final StructureType<CentralStructure> CENTRAL = type("central", CentralStructure.CODEC);
	public static final StructureType<ResemboolStructure> RESEMBOOL = type("resembool", ResemboolStructure.CODEC);
	public static final StructureType<RushValleyStructure> RUSH_VALLEY = type("rush_valley", RushValleyStructure.CODEC);

	private FmabStructures() {
	}

	private static <S extends Structure> StructureType<S> type(String name, MapCodec<S> codec) {
		return Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Fmab.id(name), () -> codec);
	}

	public static void register() {
	}
}
