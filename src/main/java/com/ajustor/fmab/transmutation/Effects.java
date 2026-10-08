package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.exchange.Family;
import com.ajustor.fmab.registry.FmabTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Les effets que les combinaisons peuvent déclencher, par identifiant ({@code fmab:wall}...).
 *
 * <p>Chacun suit les trois étapes : la compréhension a été vérifiée avant (glyphes appris), la
 * décomposition prend la matière dans le monde ou dans les objets posés, la recomposition la
 * reforme. La matière déplacée garde sa nature : un mur de terre est fait de la terre creusée.
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

	static {
		register("fmab:wall", Effects::wall);
		register("fmab:spike", Effects::spike);
		register("fmab:blade", Effects::blade);
		register("fmab:ice_platform", Effects::icePlatform);
		register("fmab:decompose", Effects::decompose);
		register("fmab:repair", Effects::repair);
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
		BlockPos center = ctx.circle().relative(d, 2);
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
			moved += raise(level, quarry, column, earth, WALL_HEIGHT, 0);
		}
		return moved > 0 ? Result.DONE : Result.NO_MATERIAL;
	}

	/**
	 * Terre + Projeter : une pique jaillit à portée, dans la direction du cercle. La pierre vient
	 * du sol tout autour de sa base, qui reste en place.
	 */
	private static Result spike(EffectContext ctx) {
		ServerLevel level = ctx.level();
		BlockPos column = surface(level, ctx.circle().relative(ctx.direction(), ctx.range()));
		if (column == null) {
			return Result.NO_TARGET;
		}
		List<BlockPos> quarry = new ArrayList<>();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			quarry.add(column.relative(side).below());
			quarry.add(column.relative(side).relative(side.getClockWise()).below());
		}
		int raised = raise(level, quarry, column, FmabTags.elementBlocks("earth"), SPIKE_HEIGHT, SPIKE_DAMAGE);
		return raised > 0 ? Result.DONE : Result.NO_MATERIAL;
	}

	/** Fer + Projeter : une lame de fer, forgée avec le fer posé sur le cercle ou porté. */
	private static Result blade(EffectContext ctx) {
		MaterialPool pool = MaterialPool.collect(ctx.level(), ctx.onCircle(), ctx.caster(), "iron", Family.METAL);
		if (!pool.consume(BLADE_MASS)) {
			return Result.NO_MATERIAL;
		}
		drop(ctx, new ItemStack(Items.IRON_SWORD));
		return Result.DONE;
	}

	/** Eau + Fixer : l'eau autour du cercle se fige en glace. */
	private static Result icePlatform(EffectContext ctx) {
		ServerLevel level = ctx.level();
		int r = ctx.range() + 1;
		int frozen = 0;
		for (BlockPos p : BlockPos.betweenClosed(ctx.circle().offset(-r, -2, -r), ctx.circle().offset(r, 0, r))) {
			double dx = p.getX() - ctx.circle().getX(), dz = p.getZ() - ctx.circle().getZ();
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

	/** Terre + Décomposer : la terre autour du support se brise en ressources. */
	private static Result decompose(EffectContext ctx) {
		ServerLevel level = ctx.level();
		TagKey<Block> earth = FmabTags.elementBlocks("earth");
		BlockPos support = ctx.support();
		int r = ctx.range();
		int broken = 0;
		for (BlockPos p : BlockPos.betweenClosed(support.offset(-r, -r, -r), support.offset(r, 0, r))) {
			if (p.equals(support)) {
				continue;
			}
			BlockState state = level.getBlockState(p);
			if (state.is(earth) && !state.hasBlockEntity() && state.getDestroySpeed(level, p) >= 0) {
				level.destroyBlock(p.immutable(), true, ctx.caster());
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
		MaterialPool pool = MaterialPool.collect(level, ctx.onCircle(), ctx.caster(), "iron", Family.METAL);
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
	 * Déplace jusqu'à {@code maxHeight} blocs de l'élément depuis les positions de
	 * {@code quarry} pour les empiler à partir de {@code column}. Les créatures prises dans la
	 * colonne sont soulevées et, si {@code damage} est positif, blessées.
	 *
	 * @return nombre de blocs déplacés
	 */
	private static int raise(ServerLevel level, List<BlockPos> quarry, BlockPos column, TagKey<Block> element,
			int maxHeight, float damage) {
		int height = 0;
		for (BlockPos from : quarry) {
			if (height >= maxHeight) {
				break;
			}
			BlockState state = level.getBlockState(from);
			if (!state.is(element) || state.hasBlockEntity()) {
				continue;
			}
			BlockPos to = column.above(height);
			if (!level.getBlockState(to).canBeReplaced()) {
				break;
			}
			level.setBlockAndUpdate(from, Blocks.AIR.defaultBlockState());
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
				}
			}
		}
		return height;
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

	private static void drop(EffectContext ctx, ItemStack stack) {
		Vec3 c = Vec3.atCenterOf(ctx.circle());
		ItemEntity entity = new ItemEntity(ctx.level(), c.x, c.y, c.z, stack);
		entity.setDeltaMovement(0, 0.2, 0);
		ctx.level().addFreshEntity(entity);
	}
}
