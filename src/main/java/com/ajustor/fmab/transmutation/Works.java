package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.registry.FmabTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * L'alchimie comme outil : abattre les arbres, récolter et faire pousser les champs, creuser un
 * tunnel, jeter un pont, aplanir un terrain. Les formes viennent de {@link WorkShapes} ; ici, on les
 * applique au monde.
 *
 * <p>L'échange équivalent tient toujours : ce qu'on abat, récolte, creuse ou rabote passe dans la
 * matière du cercle (l'étage suivant, ou le sol du cercle) ; ce qu'on bâtit est fait de la matière
 * posée sur le cercle, de l'étage précédent ou du sac de l'alchimiste ; ce qu'on fait pousser coûte
 * de la poudre d'os.
 */
public final class Works {
	/** Les minerais, pris avec la pierre qui les enferme quand on creuse. */
	private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));
	/** Ce qui fait pousser : la poudre d'os, et ce qu'un autre mod range avec elle. */
	public static final TagKey<Item> FERTILIZER = TagKey.create(Registries.ITEM, Fmab.id("fertilizer"));
	/** Un tunnel s'enfonce de tant de blocs par bloc de portée. */
	private static final int TUNNEL_PER_RANGE = 8;
	/** Un aplanissement rabote jusqu'à tant de blocs au-dessus du sol, et comble jusqu'à tant au-dessous. */
	private static final int LEVEL_ABOVE = 6;
	private static final int LEVEL_BELOW = 3;
	/** Les champs se récoltent un peu au-dessus et au-dessous du cercle : la canne à sucre, les terrasses. */
	private static final int FIELD_BELOW = 2;
	private static final int FIELD_ABOVE = 3;

	private Works() {
	}

	/**
	 * Bois + Décomposer : les objets de bois posés sur le cercle se défont, comme avant, et chaque
	 * arbre dont une bûche est à portée tombe tout entier. Les feuilles restent, et se faneront en
	 * laissant leurs pousses ; une poutre ou une maison en rondins, sans feuilles poussées d'elles-mêmes,
	 * ne bouge pas.
	 */
	static Effects.Result fell(EffectContext ctx) {
		ServerLevel level = ctx.level();
		int dismantled = Recomposition.dismantle(ctx);
		BlockPos center = ctx.origin();
		int r = ctx.range();
		Set<BlockPos> felled = new HashSet<>();
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, r, r))) {
			if (felled.contains(p)) {
				continue;
			}
			Set<BlockPos> tree = WorkShapes.tree(p.immutable(), q -> level.getBlockState(q).is(BlockTags.LOGS),
					q -> natural(level.getBlockState(q)));
			for (BlockPos log : tree) {
				dig(ctx, log);
			}
			felled.addAll(tree);
		}
		return felled.size() + dismantled > 0 ? Effects.Result.DONE : Effects.Result.NO_TARGET;
	}

	private static boolean natural(BlockState state) {
		return state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT)
				&& !state.getValue(LeavesBlock.PERSISTENT);
	}

	/**
	 * Plante + Décomposer : les objets végétaux posés sur le cercle se défont, comme avant, et tout ce
	 * qui est mûr à portée se récolte. Le blé, les carottes, les pommes de terre, les betteraves, les
	 * verrues et le cacao sont replantés d'une de leurs graines ; la canne à sucre, le cactus et le bambou
	 * sont coupés au-dessus de leur pied ; les melons et les citrouilles cueillis sur leur tige.
	 */
	static Effects.Result harvest(EffectContext ctx) {
		ServerLevel level = ctx.level();
		int dismantled = Recomposition.dismantle(ctx);
		BlockPos center = ctx.origin();
		int r = ctx.range();
		int picked = 0;
		List<BlockPos> field = new ArrayList<>();
		// Du haut vers le bas : on coupe une canne à sucre par le haut avant de regarder son pied.
		for (int dy = FIELD_ABOVE; dy >= -FIELD_BELOW; dy--) {
			for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, dy, -r), center.offset(r, dy, r))) {
				field.add(p.immutable());
			}
		}
		for (BlockPos p : field) {
			BlockState state = level.getBlockState(p);
			Ripeness ripe = ripeness(state, level.getBlockState(p.below()));
			if (ripe == Ripeness.GREEN) {
				continue;
			}
			List<ItemStack> drops = Block.getDrops(state, level, p, null);
			BlockState after = Blocks.AIR.defaultBlockState();
			if (ripe == Ripeness.REPLANT) {
				Item seed = state.getBlock().asItem();
				if (spend(drops, seed)) {
					after = replanted(state);
				}
			} else if (ripe == Ripeness.PICK) {
				after = state.setValue(SweetBerryBushBlock.AGE, 1);
			}
			ctx.flow().addAll(drops);
			level.setBlockAndUpdate(p, after);
			picked++;
		}
		return picked + dismantled > 0 ? Effects.Result.DONE : Effects.Result.NO_TARGET;
	}

	/** Ce qu'une récolte fait de ce bloc. */
	enum Ripeness {
		/** Pas mûr, ou rien à récolter. */
		GREEN,
		/** On récolte, et on replante d'une graine. */
		REPLANT,
		/** On cueille les fruits, la plante reste. */
		PICK,
		/** On coupe : il repoussera de son pied, ou de sa tige. */
		CUT
	}

	/** @param below ce qui est dessous : une canne à sucre ne se coupe pas à son pied */
	static Ripeness ripeness(BlockState state, BlockState below) {
		Block block = state.getBlock();
		if (block instanceof CropBlock crop) {
			return crop.isMaxAge(state) ? Ripeness.REPLANT : Ripeness.GREEN;
		}
		if (block instanceof NetherWartBlock) {
			return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE ? Ripeness.REPLANT : Ripeness.GREEN;
		}
		if (block instanceof CocoaBlock) {
			return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE ? Ripeness.REPLANT : Ripeness.GREEN;
		}
		if (block instanceof SweetBerryBushBlock) {
			return state.getValue(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE ? Ripeness.PICK : Ripeness.GREEN;
		}
		if (block instanceof SugarCaneBlock || block instanceof CactusBlock || block instanceof BambooStalkBlock) {
			return below.is(block) ? Ripeness.CUT : Ripeness.GREEN;
		}
		return state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN) ? Ripeness.CUT : Ripeness.GREEN;
	}

	/** La même plante, toute jeune, tournée du même côté (le cacao sur son tronc). */
	static BlockState replanted(BlockState ripe) {
		if (ripe.getBlock() instanceof CropBlock crop) {
			return crop.getStateForAge(0);
		}
		if (ripe.getBlock() instanceof NetherWartBlock) {
			return ripe.setValue(NetherWartBlock.AGE, 0);
		}
		return ripe.setValue(CocoaBlock.AGE, 0);
	}

	/** Retire une graine de la récolte, pour la replanter. */
	private static boolean spend(List<ItemStack> drops, Item seed) {
		for (ItemStack stack : drops) {
			if (stack.is(seed) && !stack.isEmpty()) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	/**
	 * Plante + Projeter : les cultures, les pousses d'arbre, le cacao et les baies à portée poussent
	 * d'un coup, comme sous la poudre d'os ; chacune en coûte une, prise sur le cercle, dans la matière
	 * de l'étage précédent ou dans le sac de l'alchimiste.
	 */
	static Effects.Result grow(EffectContext ctx) {
		ServerLevel level = ctx.level();
		BlockPos center = ctx.origin();
		int r = ctx.range();
		int grown = 0;
		boolean lacking = false;
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -FIELD_BELOW, -r), center.offset(r, FIELD_ABOVE, r))) {
			BlockState state = level.getBlockState(p);
			if (!growable(state) || (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state))) {
				continue;
			}
			if (!Effects.takeOne(ctx, FERTILIZER)) {
				lacking = true;
				break;
			}
			BlockPos at = p.immutable();
			if (!BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, at)) {
				// Elle ne pouvait plus pousser : la poudre retourne à la matière du cercle.
				ctx.flow().add(new ItemStack(Items.BONE_MEAL));
				continue;
			}
			level.levelEvent(1505, at, 15);
			grown++;
		}
		if (grown > 0) {
			return Effects.Result.DONE;
		}
		return lacking ? Effects.Result.NO_MATERIAL : Effects.Result.NO_TARGET;
	}

	/** Ce qu'on cultive : pas l'herbe ni la mousse, qui couvriraient tout de fleurs. */
	static boolean growable(BlockState state) {
		return state.is(BlockTags.CROPS) || state.getBlock() instanceof SaplingBlock || state.getBlock() instanceof CocoaBlock
				|| state.getBlock() instanceof SweetBerryBushBlock;
	}

	/**
	 * Terre + Décomposer, sur un mur ou avec une flèche de direction : un tunnel de trois sur trois
	 * s'enfonce droit devant, dans la pierre, la terre et les minerais qu'elle enferme. Sur un mur, il
	 * traverse la paroi qui porte le cercle ; au sol, il part au ras du sol dans le sens de la flèche ;
	 * au plafond, il monte. Les blocs taillés passent dans la matière du cercle.
	 */
	static Effects.Result tunnel(EffectContext ctx) {
		ServerLevel level = ctx.level();
		CircleFrame frame = ctx.frame();
		List<BlockPos> bore;
		int length = TUNNEL_PER_RANGE * ctx.range();
		if (frame.onFloor()) {
			Direction d = ctx.horizontalDirection();
			bore = WorkShapes.tunnel(ctx.circle().relative(d), d, length, 0);
		} else {
			bore = WorkShapes.tunnel(ctx.support(), frame.normal().getOpposite(), length, -1);
		}
		TagKey<Block> earth = FmabTags.elementBlocks("earth");
		int dug = 0;
		for (BlockPos p : bore) {
			BlockState state = level.getBlockState(p);
			if ((state.is(earth) || state.is(ORES)) && !state.hasBlockEntity() && state.getDestroySpeed(level, p) >= 0) {
				dig(ctx, p);
				dug++;
			}
		}
		return dug > 0 ? Effects.Result.DONE : Effects.Result.NO_TARGET;
	}

	/**
	 * Terre + Air + Fixer : un pont de trois de large file devant le cercle, de niveau avec le sol qui le
	 * porte, au-dessus du vide, de l'eau ou de la lave. Il est fait de la pierre et de la terre posées
	 * sur le cercle, de l'étage précédent ou du sac de l'alchimiste ; il s'arrête quand elles manquent.
	 */
	static Effects.Result bridge(EffectContext ctx) {
		ServerLevel level = ctx.level();
		TagKey<Block> earth = FmabTags.elementBlocks("earth");
		int placed = 0;
		boolean lacking = false;
		for (BlockPos p : WorkShapes.bridge(ctx.circle().below(), ctx.horizontalDirection(), ctx.range())) {
			if (!level.getBlockState(p).canBeReplaced()) {
				continue;
			}
			BlockState block = takeBlock(ctx, earth);
			if (block == null) {
				lacking = true;
				break;
			}
			level.setBlockAndUpdate(p, block);
			placed++;
		}
		if (placed > 0) {
			return Effects.Result.DONE;
		}
		return lacking ? Effects.Result.NO_MATERIAL : Effects.Result.NO_TARGET;
	}

	/**
	 * Terre + Air + Décomposer : le terrain autour du cercle s'aplanit au niveau du sol qui le porte.
	 * Les bosses de pierre et de terre sont rabotées, et leur matière comble les creux ; ce qui reste
	 * passe dans la matière du cercle, ce qui manque se prend sur le cercle ou dans le sac.
	 */
	static Effects.Result level(EffectContext ctx) {
		ServerLevel level = ctx.level();
		TagKey<Block> earth = FmabTags.elementBlocks("earth");
		WorkShapes.Leveling plan = WorkShapes.leveling(ctx.circle(), ctx.support().getY(), ctx.range(), LEVEL_ABOVE,
				LEVEL_BELOW);
		int changed = 0;
		for (BlockPos p : plan.cut()) {
			BlockState state = level.getBlockState(p);
			if (!p.equals(ctx.circle()) && state.is(earth) && !state.hasBlockEntity()
					&& state.getDestroySpeed(level, p) >= 0) {
				dig(ctx, p);
				changed++;
			}
		}
		for (BlockPos p : plan.fill()) {
			BlockState state = level.getBlockState(p);
			if (!state.canBeReplaced()) {
				continue;
			}
			BlockState block = takeBlock(ctx, earth);
			if (block == null) {
				break;
			}
			level.setBlockAndUpdate(p, block);
			changed++;
		}
		return changed > 0 ? Effects.Result.DONE : Effects.Result.NO_TARGET;
	}

	/** Casse ce bloc ; ce qu'il donne passe dans la matière du cercle. */
	private static void dig(EffectContext ctx, BlockPos p) {
		ServerLevel level = ctx.level();
		ctx.flow().addAll(Block.getDrops(level.getBlockState(p), level, p, level.getBlockEntity(p)));
		level.destroyBlock(p, false, ctx.caster());
	}

	/**
	 * Un bloc de l'élément à poser : dans la matière qui circule entre étages (ce qu'on vient de raboter
	 * compris), sur le cercle, puis dans le sac de l'alchimiste. Un alchimiste en créatif n'en manque
	 * jamais.
	 */
	private static BlockState takeBlock(EffectContext ctx, TagKey<Block> element) {
		for (ItemStack stack : ctx.flow()) {
			BlockState state = blockOf(stack, element);
			if (state != null) {
				stack.shrink(1);
				return state;
			}
		}
		for (ItemEntity entity : ctx.level().getEntitiesOfClass(ItemEntity.class, ctx.onCircle())) {
			BlockState state = blockOf(entity.getItem(), element);
			if (state != null) {
				entity.getItem().shrink(1);
				return state;
			}
		}
		ServerPlayer caster = ctx.caster();
		if (caster == null) {
			return null;
		}
		if (caster.isCreative()) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		for (ItemStack stack : caster.getInventory()) {
			BlockState state = blockOf(stack, element);
			if (state != null) {
				stack.shrink(1);
				return state;
			}
		}
		return null;
	}

	private static BlockState blockOf(ItemStack stack, TagKey<Block> element) {
		if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) {
			return null;
		}
		BlockState state = item.getBlock().defaultBlockState();
		return state.is(element) ? state : null;
	}
}
