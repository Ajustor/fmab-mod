package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.entity.AmestrianSoldierEntity;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.stone.Karma;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * La transmutation de l'or (glyphe Or + Recomposer) : le fer posé dans le cercle devient de l'or,
 * pièce pour pièce, même famille, même masse. C'est permis par l'échange équivalent, mais interdit
 * par la loi d'État : le karma chute, l'armée vous recherche, et les soldats proches accourent.
 */
public final class GoldTransmutation {
	/** Ce qui se change en or, et en quoi. */
	private static final Map<Item, Item> GOLD = Map.of(
			Items.IRON_INGOT, Items.GOLD_INGOT,
			Items.IRON_NUGGET, Items.GOLD_NUGGET,
			Items.IRON_BLOCK, Items.GOLD_BLOCK,
			Items.RAW_IRON, Items.RAW_GOLD);
	private static final int KARMA = -10;
	/** La traque dure dix minutes. */
	private static final int WANTED = 20 * 60 * 10;
	private static final double ALERT = 32;

	private GoldTransmutation() {
	}

	public static void register() {
		Effects.register("fmab:gold_transmutation", GoldTransmutation::apply);
	}

	private static Effects.Result apply(EffectContext ctx) {
		ServerLevel level = ctx.level();
		List<ItemStack> sources = new ArrayList<>(ctx.flow());
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, ctx.onCircle())) {
			sources.add(entity.getItem());
		}
		List<ItemStack> gold = new ArrayList<>();
		for (ItemStack stack : sources) {
			Item into = GOLD.get(stack.getItem());
			if (into != null && !stack.isEmpty()) {
				gold.add(new ItemStack(into, stack.getCount()));
				stack.setCount(0);
			}
		}
		if (gold.isEmpty()) {
			return Effects.Result.NO_TARGET;
		}
		ctx.flow().removeIf(ItemStack::isEmpty);
		ctx.flow().addAll(gold);
		BlockPos c = ctx.circle();
		level.sendParticles(ParticleTypes.WAX_ON, c.getX() + 0.5, c.getY() + 0.3, c.getZ() + 0.5, 30, 0.6, 0.2, 0.6, 0.1);
		ServerPlayer caster = ctx.caster();
		if (caster != null && !caster.isCreative()) {
			punish(level, caster);
		}
		return Effects.Result.DONE;
	}

	/** La loi d'État ne pardonne pas : karma, avis de recherche, et la garde qui accourt. */
	private static void punish(ServerLevel level, ServerPlayer caster) {
		Karma.add(caster, KARMA);
		caster.setAttached(FmabAttachments.WANTED, level.getGameTime() + WANTED);
		caster.sendSystemMessage(Component.translatable("transmutation.fmab.gold_forbidden").withStyle(ChatFormatting.RED));
		for (AmestrianSoldierEntity soldier : level.getEntitiesOfClass(AmestrianSoldierEntity.class,
				caster.getBoundingBox().inflate(ALERT))) {
			soldier.setTarget(caster);
		}
	}
}
