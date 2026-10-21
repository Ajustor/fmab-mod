package com.ajustor.fmab.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * La carte d'un cartographe vers un lieu du mod ({@code fmab:site_map}) : comme
 * {@code minecraft:exploration_map}, mais un lieu unique s'y trouve par sa place, connue d'avance,
 * sans fouiller le monde chunk par chunk (voir {@link Maps#find}). Sans lieu, la carte reste vierge.
 */
public final class SiteMapFunction extends LootItemConditionalFunction {
	public static final MapCodec<SiteMapFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i).and(i.group(
			TagKey.codec(Registries.STRUCTURE).fieldOf("destination").forGetter(f -> f.destination),
			MapDecorationType.CODEC.fieldOf("decoration").forGetter(f -> f.decoration),
			Codec.BYTE.optionalFieldOf("zoom", (byte) 2).forGetter(f -> f.zoom)
	)).apply(i, SiteMapFunction::new));

	private final TagKey<Structure> destination;
	private final Holder<MapDecorationType> decoration;
	private final byte zoom;

	//? if >=26.3 {
	/*private SiteMapFunction(java.util.Optional<Holder<LootItemCondition>> predicates, TagKey<Structure> destination,
	*///?} else {
	private SiteMapFunction(List<LootItemCondition> predicates, TagKey<Structure> destination,
	//?}
			Holder<MapDecorationType> decoration, byte zoom) {
		super(predicates);
		this.destination = destination;
		this.decoration = decoration;
		this.zoom = zoom;
	}

	@Override
	public MapCodec<SiteMapFunction> codec() {
		return MAP_CODEC;
	}

	@Override
	public Set<ContextKey<?>> getReferencedContextParams() {
		return Set.of(LootContextParams.ORIGIN);
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		//? if >=26.3 {
		/*Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
		*///?} else {
		Vec3 origin = context.getOptionalParameter(LootContextParams.ORIGIN);
		//?}
		if (!stack.is(Items.MAP) || origin == null) {
			return stack;
		}
		ItemStack map = Maps.toStructure(context.getLevel(), BlockPos.containing(origin), destination, decoration, zoom);
		return map.isEmpty() ? stack : map;
	}
}
