package com.ajustor.fmab.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** Les deux emplacements de gants du joueur. Une case vide contient {@link ItemStack#EMPTY}. */
public record Gloves(ItemStack left, ItemStack right) {
	public static final Gloves NONE = new Gloves(ItemStack.EMPTY, ItemStack.EMPTY);

	public static final Codec<Gloves> CODEC = RecordCodecBuilder.create(i -> i.group(
			ItemStack.OPTIONAL_CODEC.optionalFieldOf("left", ItemStack.EMPTY).forGetter(Gloves::left),
			ItemStack.OPTIONAL_CODEC.optionalFieldOf("right", ItemStack.EMPTY).forGetter(Gloves::right)
	).apply(i, Gloves::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, Gloves> STREAM_CODEC = StreamCodec.composite(
			ItemStack.OPTIONAL_STREAM_CODEC, Gloves::left,
			ItemStack.OPTIONAL_STREAM_CODEC, Gloves::right,
			Gloves::new);

	public Gloves {
		left = left.copy();
		right = right.copy();
	}

	public ItemStack get(boolean leftHand) {
		return leftHand ? left : right;
	}

	public Gloves with(boolean leftHand, ItemStack stack) {
		return leftHand ? new Gloves(stack, right) : new Gloves(left, stack);
	}
}
