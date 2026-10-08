package com.ajustor.fmab.data;

import com.ajustor.fmab.gate.BodyPart;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * Les prothèses fixées sur les ports nerveux du joueur, une par membre perdu (bras et jambes,
 * gauche et droite).
 */
public record Automail(Map<BodyPart, ItemStack> limbs) {
	public static final Automail NONE = new Automail(Map.of());

	private static final Codec<BodyPart> PART = Codec.STRING.xmap(BodyPart::fromSerializedName,
			BodyPart::serializedName);

	public static final Codec<Automail> CODEC = Codec.unboundedMap(PART, ItemStack.CODEC)
			.xmap(Automail::new, Automail::limbs);

	public static final StreamCodec<RegistryFriendlyByteBuf, Automail> STREAM_CODEC =
			ByteBufCodecs.fromCodecWithRegistries(CODEC);

	public Automail {
		Map<BodyPart, ItemStack> copy = new EnumMap<>(BodyPart.class);
		limbs.forEach((part, stack) -> {
			if (!stack.isEmpty()) {
				copy.put(part, stack.copy());
			}
		});
		limbs = Map.copyOf(copy);
	}

	public ItemStack get(BodyPart part) {
		return limbs.getOrDefault(part, ItemStack.EMPTY);
	}

	public Automail with(BodyPart part, ItemStack stack) {
		Map<BodyPart, ItemStack> more = new EnumMap<>(BodyPart.class);
		more.putAll(limbs);
		if (stack.isEmpty()) {
			more.remove(part);
		} else {
			more.put(part, stack);
		}
		return new Automail(more);
	}
}
