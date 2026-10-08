package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.exchange.Family;
import com.ajustor.fmab.registry.FmabTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Les effets que les combinaisons peuvent déclencher, par identifiant ({@code fmab:wall}...).
 *
 * <p>Chacun suit les trois étapes : la compréhension a été vérifiée avant (glyphes appris), la
 * décomposition prend la matière à l'étage précédent, dans le monde ou dans les objets posés, la
 * recomposition la reforme. La matière déplacée garde sa nature : un mur de terre est fait de la
 * terre creusée.
 */
public final class Effects {
	public enum Result {
		DONE,
		/** Pas assez de matière de l'élément visé. */
		NO_MATERIAL,
		/** Rien à transformer à portée. */
		NO_TARGET
	}

	@FunctionalInterface
	public interface Effect {
		Result apply(EffectContext ctx);
	}

	private static final Map<String, Effect> EFFECTS = new HashMap<>();
	private static final int WALL_HEIGHT = 3;
	private static final int SPIKE_HEIGHT = 3;
	private static final float SPIKE_DAMAGE = 6;
	/** Masse d'une lame de fer : deux lingots. */
	private static final int BLADE_MASS = 18;
	/** Un lingot (9) répare un quart de la durabilité, comme à l'enclume. */
	private static final int INGOT_MASS = 9;
	private static final float FLAME_DAMAGE = 4;
	private static final float BURST_DAMAGE = 7;
	/** Un charbon cuit huit objets, comme au fourneau. */
	private static final int ITEMS_PER_FUEL = 8;

	static {
		register("fmab:wall", Effects::wall);
		register("fmab:spike", Effects::spike);
		register("fmab:blade", Effects::blade);
		register("fmab:ice_platform", Effects::icePlatform);
		register("fmab:decompose", Effects::decompose);
		register("fmab:repair", Effects::repair);
		register("fmab:flame_jet", ctx -> flames(ctx, 0, FLAME_DAMAGE, 1));
		register("fmab:flame_burst", ctx -> flames(ctx, 1, BURST_DAMAGE, 2));
		register("fmab:gust", Effects::gust);
		register("fmab:smelt", Effects::smelt);
	}

	private Effects() {
	}

	/** Point d'extension : un autre mod peut brancher ses effets sur la table des combinaisons. */
	public static void register(String id, Effect effect) {
		EFFECTS.put(id, effect);
	}

	public static Optional<Effect> get(String id) {
		return Optional.ofNullable(EFFECTS.get(id));
	}

	/** Terre + Fixer : un mur se lève devant le cercle, fait de la terre creusée juste devant lui. */
	private static Result wall(EffectContext ctx) {
		ServerLevel level = ctx.level();
		Direction d = ctx.direction();
		Direction along = d.getClockWise();
		TagKey<Block> earth = FmabTags.elementBlocks("earth");
		BlockPos center = ctx.origin().relative(d, 2);
		int moved = 0;
		for (int i = -ctx.range(); i <= ctx.range(); i++) {
			BlockPos column = surface(level, center.relative(along, i));
			if (column == null) {
				continue;
			}
			List<BlockPos> quarry = new ArrayList<>();
			for (int k = 1; k <= WALL_HEIGHT; k++) {
				quarry.add(column.relative(d).below(k));
			}
			moved += raise(ctx, quarry, column, earth, WALL_HEIGHT, 0);
		}
		return moved > 0 ? Result.DONE : Result.NO_MATERIAL;
	}

	/**
	 * Terre + Projeter : une pique jaillit à portée, dans la direction du cercle. La pierre vient
	 * de l'étage précédent s'il en a décomposé, sinon du sol tout autour de sa base.
	 */
	private static Result spike(EffectContext ctx) {
		ServerLevel level = ctx.level();
		BlockPos column = surface(level, ctx.origin().relative(ctx.direction(), ctx.range()));
		if (column == null) {
			return Result.NO_TARGET;
		}
		List<BlockPos> quarry = new ArrayList<>();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			quarry.add(column.relative(side).below());
			quarry.add(column.relative(side).relative(side.getClockWise()).below());
		}
		int raised = raise(ctx, quarry, column, FmabTags.elementBlocks("earth"), SPIKE_HEIGHT, SPIKE_DAMAGE);
		return raised > 0 ? Result.DONE : Result.NO_MATERIAL;
	}

	/** Fer + Projeter : une lame de fer, forgée avec le fer posé sur le cercle ou porté. */
	private static Result blade(EffectContext ctx) {
		MaterialPool pool = MaterialPool.collect(ctx, "iron", Family.METAL);
		if (!pool.consume(BLADE_MASS)) {
			return Result.NO_MATERIAL;
		}
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		if (ctx.stage().infused("fire")) {
			sword.enchant(ctx.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
					.getOrThrow(Enchantments.FIRE_ASPECT), 1);
		}
		drop(ctx, sword);
		return Result.DONE;
	}

	/** Eau + Fixer : l'eau autour du cercle se fige en glace. */
	private static Result icePlatform(EffectContext ctx) {
		ServerLevel level = ctx.level();
		BlockPos origin = ctx.origin();
		int r = ctx.range() + 1;
		int frozen = 0;
		for (BlockPos p : BlockPos.betweenClosed(origin.offset(-r, -2, -r), origin.offset(r, 0, r))) {
			double dx = p.getX() - origin.getX(), dz = p.getZ() - origin.getZ();
			if (dx * dx + dz * dz > r * r + 0.5) {
				continue;
			}
			BlockState state = level.getBlockState(p);
			if (state.is(Blocks.WATER) && state.getFluidState().isSourceOfType(Fluids.WATER)
					&& !level.getBlockState(p.above()).getFluidState().is(Fluids.WATER)) {
				level.setBlockAndUpdate(p, Blocks.ICE.defaultBlockState());
				frozen++;
			}
		}
		return frozen > 0 ? Result.DONE : Result.NO_TARGET;
	}

	/**
	 * Terre + Décomposer : la terre autour du support se brise en ressources. Elles passent à
	 * l'étage suivant, ou retombent sur le cercle.
	 */
	private static Result decompose(EffectContext ctx) {
		ServerLevel level = ctx.level();
		TagKey<Block> earth = FmabTags.elementBlocks("earth");
		BlockPos support = ctx.support();
		BlockPos center = ctx.origin().below();
		int r = ctx.range();
		int broken = 0;
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, 0, r))) {
			if (p.equals(support)) {
				continue;
			}
			BlockState state = level.getBlockState(p);
			if (state.is(earth) && !state.hasBlockEntity() && state.getDestroySpeed(level, p) >= 0) {
				BlockPos at = p.immutable();
				ctx.flow().addAll(Block.getDrops(state, level, at, null));
				level.destroyBlock(at, false, ctx.caster());
				broken++;
			}
		}
		return broken > 0 ? Result.DONE : Result.NO_TARGET;
	}

	/** Fer + Réparer : les objets en fer posés sur le cercle sont réparés avec du fer. */
	private static Result repair(EffectContext ctx) {
		ServerLevel level = ctx.level();
		ItemStack ingot = new ItemStack(Items.IRON_INGOT);
		List<ItemStack> damaged = new ArrayList<>();
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, ctx.onCircle())) {
			ItemStack stack = entity.getItem();
			if (stack.isDamaged() && stack.isValidRepairItem(ingot)) {
				damaged.add(stack);
			}
		}
		if (damaged.isEmpty()) {
			return Result.NO_TARGET;
		}
		MaterialPool pool = MaterialPool.collect(ctx, "iron", Family.METAL);
		boolean repaired = false;
		for (ItemStack stack : damaged) {
			int max = stack.getMaxDamage();
			// Masse nécessaire pour tout réparer, arrondie au-dessus : un quart de durabilité par lingot.
			int needed = (int) Math.ceil(stack.getDamageValue() * 4.0 * INGOT_MASS / max);
			int used = Math.min(needed, pool.available());
			if (used <= 0 || !pool.consume(used)) {
				continue;
			}
			int restored = (int) Math.floor(used * max / (4.0 * INGOT_MASS));
			stack.setDamageValue(Math.max(0, stack.getDamageValue() - restored));
			repaired = true;
		}
		return repaired ? Result.DONE : Result.NO_MATERIAL;
	}

	/**
	 * Feu + Projeter (jet) et Feu + Air + Projeter (combustion amplifiée) : une traînée de flammes
	 * qui brûle ce qu'elle touche. Le feu ne naît pas de rien : il consomme du combustible.
	 *
	 * @param halfWidth 0 pour un jet d'un bloc de large, 1 pour un cône de trois
	 */
	private static Result flames(EffectContext ctx, int halfWidth, float damage, int fuel) {
		MaterialPool pool = MaterialPool.collect(ctx, "fire", null);
		if (!pool.consume(fuel)) {
			return Result.NO_MATERIAL;
		}
		ServerLevel level = ctx.level();
		Direction d = ctx.direction();
		Direction side = d.getClockWise();
		for (int i = 1; i <= ctx.range(); i++) {
			for (int w = -halfWidth; w <= halfWidth; w++) {
				BlockPos p = surface(level, ctx.origin().relative(d, i).relative(side, w));
				if (p == null) {
					continue;
				}
				level.sendParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
						6, 0.3, 0.3, 0.3, 0.02);
				if (level.getBlockState(p).isAir() && i > 1) {
					level.setBlockAndUpdate(p, BaseFireBlock.getState(level, p));
				}
				for (Entity e : level.getEntities((Entity) null, new AABB(p).expandTowards(0, 1, 0), Entity::isAlive)) {
					if (e != ctx.caster() && e instanceof LivingEntity living) {
						living.hurtServer(level, level.damageSources().inFire(), damage);
						living.igniteForSeconds(4);
						ctx.afflict(living);
					}
				}
			}
		}
		return Result.DONE;
	}

	/** Air + Projeter : une rafale qui repousse tout dans la direction du cercle et éteint les feux. */
	private static Result gust(EffectContext ctx) {
		ServerLevel level = ctx.level();
		Direction d = ctx.direction();
		BlockPos start = ctx.origin();
		BlockPos end = start.relative(d, ctx.range());
		AABB area = new AABB(Vec3.atCenterOf(start), Vec3.atCenterOf(end)).inflate(1.5, 1.5, 1.5);
		int pushed = 0;
		for (Entity e : level.getEntities((Entity) null, area, Entity::isAlive)) {
			if (e == ctx.caster()) {
				continue;
			}
			e.push(d.getStepX() * 1.6, 0.4, d.getStepZ() * 1.6);
			e.hurtMarked = true;
			if (e instanceof LivingEntity living) {
				ctx.afflict(living);
			}
			pushed++;
		}
		for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
				BlockPos.containing(area.maxX, area.maxY, area.maxZ))) {
			if (level.getBlockState(p).is(Blocks.FIRE)) {
				level.removeBlock(p, false);
			}
		}
		level.sendParticles(ParticleTypes.CLOUD, start.getX() + 0.5 + d.getStepX(), start.getY() + 0.8,
				start.getZ() + 0.5 + d.getStepZ(), 20, d.getStepX() * 0.5, 0.2, d.getStepZ() * 0.5, 0.3);
		return pushed > 0 ? Result.DONE : Result.NO_TARGET;
	}

	/** Feu + Décomposer : les objets posés sur le cercle fondent comme au fourneau. */
	private static Result smelt(EffectContext ctx) {
		ServerLevel level = ctx.level();
		List<ItemStack> sources = new ArrayList<>(ctx.flow());
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, ctx.onCircle())) {
			sources.add(entity.getItem());
		}
		int smeltable = 0;
		for (ItemStack stack : sources) {
			if (cooked(level, stack).isPresent()) {
				smeltable += stack.getCount();
			}
		}
		if (smeltable == 0) {
			return Result.NO_TARGET;
		}
		MaterialPool fuel = MaterialPool.collect(ctx, "fire", null);
		int batches = Math.min(fuel.available(), (smeltable + ITEMS_PER_FUEL - 1) / ITEMS_PER_FUEL);
		if (batches == 0) {
			return Result.NO_MATERIAL;
		}
		fuel.consume(batches);
		int budget = batches * ITEMS_PER_FUEL;
		List<ItemStack> results = new ArrayList<>();
		for (ItemStack stack : sources) {
			Optional<ItemStack> result = cooked(level, stack);
			if (result.isEmpty() || budget == 0) {
				continue;
			}
			int count = Math.min(budget, stack.getCount());
			budget -= count;
			stack.shrink(count);
			results.add(result.get().copyWithCount(result.get().getCount() * count));
		}
		ctx.flow().removeIf(ItemStack::isEmpty);
		ctx.flow().addAll(results);
		return Result.DONE;
	}

	private static Optional<ItemStack> cooked(ServerLevel level, ItemStack stack) {
		if (stack.isEmpty()) {
			return Optional.empty();
		}
		SingleRecipeInput input = new SingleRecipeInput(stack);
		return level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level)
				.map(holder -> holder.value().assemble(input))
				.filter(result -> !result.isEmpty());
	}

	/**
	 * Empile jusqu'à {@code maxHeight} blocs de l'élément à partir de {@code column}. Ils viennent
	 * d'abord de la matière de l'étage précédent, puis des positions de {@code quarry}. Les
	 * créatures prises dans la colonne sont soulevées et, si {@code damage} est positif, blessées.
	 *
	 * @return nombre de blocs déplacés
	 */
	private static int raise(EffectContext ctx, List<BlockPos> quarry, BlockPos column, TagKey<Block> element,
			int maxHeight, float damage) {
		ServerLevel level = ctx.level();
		int height = 0;
		Iterator<BlockPos> world = quarry.iterator();
		while (height < maxHeight) {
			BlockPos to = column.above(height);
			if (!level.getBlockState(to).canBeReplaced()) {
				break;
			}
			BlockState state = fromFlow(ctx, element);
			if (state == null) {
				BlockPos from = null;
				while (world.hasNext() && from == null) {
					BlockPos candidate = world.next();
					BlockState s = level.getBlockState(candidate);
					if (s.is(element) && !s.hasBlockEntity()) {
						from = candidate;
						state = s;
					}
				}
				if (from == null) {
					break;
				}
				level.setBlockAndUpdate(from, Blocks.AIR.defaultBlockState());
			}
			level.setBlockAndUpdate(to, state);
			height++;
		}
		if (height > 0) {
			AABB space = new AABB(column).expandTowards(0, height - 1, 0);
			for (Entity e : level.getEntities((Entity) null, space, Entity::isAlive)) {
				e.setPos(e.getX(), column.getY() + height, e.getZ());
				if (damage > 0 && e instanceof LivingEntity living) {
					living.hurtServer(level, level.damageSources().magic(), damage);
					living.setDeltaMovement(living.getDeltaMovement().add(0, 0.8, 0));
					living.hurtMarked = true;
					ctx.afflict(living);
				}
			}
		}
		return height;
	}

	/** Un bloc de l'élément pris dans la matière qui circule entre étages, ou null. */
	private static BlockState fromFlow(EffectContext ctx, TagKey<Block> element) {
		for (ItemStack stack : ctx.flow()) {
			if (!stack.isEmpty() && stack.getItem() instanceof BlockItem item) {
				BlockState state = item.getBlock().defaultBlockState();
				if (state.is(element)) {
					stack.shrink(1);
					return state;
				}
			}
		}
		return null;
	}

	/**
	 * Première position libre posée sur du solide, en cherchant deux blocs au-dessus et
	 * au-dessous : les effets suivent le relief.
	 */
	private static BlockPos surface(ServerLevel level, BlockPos around) {
		for (int dy = 2; dy >= -2; dy--) {
			BlockPos p = around.above(dy);
			BlockPos below = p.below();
			if (level.getBlockState(p).canBeReplaced()
					&& level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
				return p;
			}
		}
		return null;
	}

	static void drop(EffectContext ctx, ItemStack stack) {
		Vec3 c = Vec3.atCenterOf(ctx.circle());
		ItemEntity entity = new ItemEntity(ctx.level(), c.x, c.y, c.z, stack);
		entity.setDeltaMovement(0, 0.2, 0);
		ctx.level().addFreshEntity(entity);
	}
}
