package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.progress.Milestones;
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
	/** Ce que vaut chaque forme du fer, en pépites. */
	private static final Map<Item, Integer> NUGGETS = Map.of(
			Items.IRON_NUGGET, 1,
			Items.IRON_INGOT, 9,
			Items.RAW_IRON, 9,
			Items.IRON_BLOCK, 81);
	/** Une transmutation ne change pas plus d'un bloc de fer en or : le reste demeure du fer. */
	static final int MAX_NUGGETS = 81;
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
		int budget = MAX_NUGGETS;
		for (ItemStack stack : sources) {
			Item into = GOLD.get(stack.getItem());
			if (into == null || stack.isEmpty()) {
				continue;
			}
			int unit = NUGGETS.get(stack.getItem());
			int taken = convertible(stack.getCount(), unit, budget);
			if (taken > 0) {
				gold.add(new ItemStack(into, taken));
				stack.shrink(taken);
				budget -= taken * unit;
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
		if (caster != null) {
			Milestones.reach(caster, "gold");
		}
		if (caster != null && !caster.isCreative()) {
			punish(level, caster, karma(MAX_NUGGETS - budget));
		}
		return Effects.Result.DONE;
	}

	/** Combien d'objets de ce tas (chacun valant {@code unit} pépites) tiennent dans ce qui reste. */
	static int convertible(int count, int unit, int budget) {
		return Math.max(0, Math.min(count, budget / unit));
	}

	/** Le karma perdu : dix, plus un par lingot d'or fait. */
	static int karma(int nuggets) {
		return KARMA - nuggets / 9;
	}

	/** La loi d'État ne pardonne pas : karma, avis de recherche, et la garde qui accourt. */
	private static void punish(ServerLevel level, ServerPlayer caster, int karma) {
		Karma.add(caster, karma);
		FmabAttachments.WANTED.set(caster, level.getGameTime() + WANTED);
		caster.sendSystemMessage(Component.translatable("transmutation.fmab.gold_forbidden").withStyle(ChatFormatting.RED));
		for (AmestrianSoldierEntity soldier : level.getEntitiesOfClass(AmestrianSoldierEntity.class,
				caster.getBoundingBox().inflate(ALERT))) {
			soldier.setTarget(caster);
		}
	}
}
