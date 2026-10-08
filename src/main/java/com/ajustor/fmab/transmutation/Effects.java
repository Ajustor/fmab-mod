package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.exchange.Family;
import com.ajustor.fmab.item.TransmutedWeaponItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.registry.FmabTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

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
	/** Distance à laquelle une pique ou un mur de glace va chercher son eau. */
	private static final int ICE_REACH = 3;
	/** Puissance d'une détonation, avant le savoir (une TNT vaut 4). */
	private static final float BLAST_POWER = 2;
	/** Une lance de pierre demande deux blocs. */
	private static final int LANCE_BLOCKS = 2;
	/** Un charbon cuit huit objets, comme au fourneau. */
	private static final int ITEMS_PER_FUEL = 8;

	static {
		register("fmab:wall", Effects::wall);
		register("fmab:spike", Effects::spike);
		register("fmab:blade", Effects::blade);
		register("fmab:ice_platform", Effects::icePlatform);
		register("fmab:decompose", Effects::decompose);
		register("fmab:repair", Effects::repair);
		register("fmab:flame_jet", ctx -> flames(ctx, 0, ctx.damage(FLAME_DAMAGE), 1));
		register("fmab:flame_burst", ctx -> flames(ctx, 1, ctx.damage(BURST_DAMAGE), 2));
		register("fmab:gust", Effects::gust);
		register("fmab:smelt", Effects::smelt);
		register("fmab:stone_lance", Effects::stoneLance);
		register("fmab:arm_blade", Effects::armBlade);
		register("fmab:ice_spike", Effects::iceSpike);
		register("fmab:ice_wall", Effects::iceWall);
		register("fmab:detonate", Effects::detonate);
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

	/**
	 * Terre + Fixer : un mur se lève devant le cercle, fait de la terre creusée juste devant lui.
	 * Le nœud « Mur épais » lui ajoute des couches.
	 */
	private static Result wall(EffectContext ctx) {
		ServerLevel level = ctx.level();
		Direction d = ctx.horizontalDirection();
		Direction along = d.getClockWise();
		TagKey<Block> earth = FmabTags.elementBlocks("earth");
		int thickness = 1 + (int) ctx.perk("wall_thickness");
		int moved = 0;
		for (int layer = 0; layer < thickness; layer++) {
			BlockPos center = ctx.origin().relative(d, 2 + layer);
			for (int i = -ctx.range(); i <= ctx.range(); i++) {
				BlockPos column = surface(level, center.relative(along, i));
				if (column == null) {
					continue;
				}
				// La terre vient de devant le mur : derrière sa dernière couche.
				List<BlockPos> quarry = new ArrayList<>();
				for (int k = 1; k <= WALL_HEIGHT; k++) {
					quarry.add(column.relative(d, thickness - layer).below(k));
				}
				moved += raise(ctx, quarry, column, Direction.UP, earth, WALL_HEIGHT, 0);
			}
		}
		return moved > 0 ? Result.DONE : Result.NO_MATERIAL;
	}

	/**
	 * Terre + Projeter : une pique jaillit à portée, dans la direction du cercle. La pierre vient
	 * de l'étage précédent s'il en a décomposé, sinon du sol tout autour de sa base. Sur un mur ou
	 * un plafond, la pique sort droit de la surface, faite de la pierre qui entoure le support.
	 */
	private static Result spike(EffectContext ctx) {
		ServerLevel level = ctx.level();
		TagKey<Block> earth = FmabTags.elementBlocks("earth");
		CircleFrame frame = ctx.frame();
		if (!frame.onFloor()) {
			BlockPos support = ctx.support();
			// Les huit blocs qui entourent le support, dans le plan de la surface.
			List<BlockPos> quarry = new ArrayList<>();
			for (int a = -1; a <= 1; a++) {
				for (int b = -1; b <= 1; b++) {
					if (a != 0 || b != 0) {
						quarry.add(support.relative(frame.pageRight(), a).relative(frame.pageUp(), b));
					}
				}
			}
			int raised = raise(ctx, quarry, ctx.circle().relative(frame.normal()), frame.normal(), earth, SPIKE_HEIGHT,
					ctx.damage(SPIKE_DAMAGE));
			return raised > 0 ? Result.DONE : Result.NO_MATERIAL;
		}
		BlockPos column = surface(level, ctx.origin().relative(ctx.direction(), ctx.range()));
		if (column == null) {
			return Result.NO_TARGET;
		}
		List<BlockPos> quarry = new ArrayList<>();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			quarry.add(column.relative(side).below());
			quarry.add(column.relative(side).relative(side.getClockWise()).below());
		}
		int raised = raise(ctx, quarry, column, Direction.UP, earth, SPIKE_HEIGHT, ctx.damage(SPIKE_DAMAGE));
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
		// Une âme scellée dans une armure, debout sur le cercle : on la répare comme Ed répare Al.
		for (ServerPlayer soul : level.getEntitiesOfClass(ServerPlayer.class, ctx.onCircle().expandTowards(0, 1, 0),
				p -> p.getAttachedOrCreate(FmabAttachments.GATE).soulBound())) {
			for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
					EquipmentSlot.FEET}) {
				ItemStack piece = soul.getItemBySlot(slot);
				if (piece.isDamaged() && piece.isValidRepairItem(ingot)) {
					damaged.add(piece);
				}
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
		Direction side = d.getAxis().isHorizontal() ? d.getClockWise() : ctx.frame().pageRight();
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

	/**
	 * Terre + Recomposer : une lance tirée du sol, faite de deux blocs de terre ou de pierre. Elle se
	 * défait au bout d'une minute et rend ces deux blocs.
	 */
	private static Result stoneLance(EffectContext ctx) {
		ServerLevel level = ctx.level();
		TagKey<Block> earth = FmabTags.elementBlocks("earth");
		List<BlockState> taken = new ArrayList<>();
		while (taken.size() < LANCE_BLOCKS) {
			BlockState fromFlow = fromFlow(ctx, earth);
			if (fromFlow == null) {
				break;
			}
			taken.add(fromFlow);
		}
		List<BlockPos> dug = new ArrayList<>();
		BlockPos below = ctx.origin().below();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (taken.size() + dug.size() >= LANCE_BLOCKS) {
				break;
			}
			BlockPos p = below.relative(side);
			BlockState state = level.getBlockState(p);
			if (state.is(earth) && !state.hasBlockEntity()) {
				dug.add(p);
				taken.add(state);
			}
		}
		if (taken.size() < LANCE_BLOCKS) {
			// Rien n'est consommé : ce qui venait de l'étage précédent y retourne.
			for (int i = 0; i < taken.size() - dug.size(); i++) {
				ctx.flow().add(new ItemStack(taken.get(i).getBlock()));
			}
			return Result.NO_MATERIAL;
		}
		dug.forEach(p -> level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState()));
		ItemStack remains = new ItemStack(taken.getFirst().getBlock(), LANCE_BLOCKS);
		give(ctx, TransmutedWeaponItem.create(FmabItems.STONE_LANCE, level, remains));
		return Result.DONE;
	}

	/** Fer + Recomposer : une lame-bras, d'un lingot de fer, qu'elle rend en se défaisant. */
	private static Result armBlade(EffectContext ctx) {
		MaterialPool pool = MaterialPool.collect(ctx, "iron", Family.METAL);
		if (!pool.consume(INGOT_MASS)) {
			return Result.NO_MATERIAL;
		}
		ItemStack blade = TransmutedWeaponItem.create(FmabItems.ARM_BLADE, ctx.level(), new ItemStack(Items.IRON_INGOT));
		if (ctx.stage().infused("fire")) {
			blade.enchant(ctx.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
					.getOrThrow(Enchantments.FIRE_ASPECT), 1);
		}
		give(ctx, blade);
		return Result.DONE;
	}

	/** Une arme transmutée va droit dans la main de l'alchimiste, ou à ses pieds. */
	private static void give(EffectContext ctx, ItemStack stack) {
		if (!ctx.caster().getInventory().add(stack)) {
			drop(ctx, stack);
		}
	}

	/** L'eau se fige : glace tassée, qui ne fond pas. */
	private static BlockState frozen(BlockState state) {
		return Blocks.PACKED_ICE.defaultBlockState();
	}

	/** L'eau, la glace et la neige à portée d'un point, les plus proches d'abord. */
	private static List<BlockPos> waterAround(ServerLevel level, BlockPos center, int r) {
		TagKey<Block> water = FmabTags.elementBlocks("water");
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -2, -r), center.offset(r, 1, r))) {
			BlockState state = level.getBlockState(p);
			if (state.is(water) && (!state.is(Blocks.WATER) || state.getFluidState().isSource())) {
				out.add(p.immutable());
			}
		}
		out.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
		return out;
	}

	/**
	 * Eau + Projeter : une pique de glace jaillit à portée, faite de l'eau, de la glace ou de la
	 * neige qui l'entoure. Ce qu'elle touche est ralenti.
	 */
	private static Result iceSpike(EffectContext ctx) {
		ServerLevel level = ctx.level();
		BlockPos column = surface(level, ctx.origin().relative(ctx.horizontalDirection(), ctx.range()));
		if (column == null) {
			return Result.NO_TARGET;
		}
		int raised = raise(ctx, waterAround(level, column, ICE_REACH), column, Direction.UP,
				FmabTags.elementBlocks("water"), SPIKE_HEIGHT, ctx.damage(SPIKE_DAMAGE), Effects::frozen);
		if (raised > 0) {
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(column).inflate(1.5))) {
				if (e != ctx.caster()) {
					e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
				}
			}
		}
		return raised > 0 ? Result.DONE : Result.NO_MATERIAL;
	}

	/**
	 * Eau + Air + Fixer : un mur de glace se lève devant le cercle, fait de l'eau alentour. L'Air
	 * porte l'humidité jusqu'au mur.
	 */
	private static Result iceWall(EffectContext ctx) {
		ServerLevel level = ctx.level();
		Direction d = ctx.horizontalDirection();
		Direction along = d.getClockWise();
		BlockPos center = ctx.origin().relative(d, 2);
		List<BlockPos> water = waterAround(level, ctx.origin(), ctx.range() * 2 + ICE_REACH);
		int moved = 0;
		for (int i = -ctx.range(); i <= ctx.range(); i++) {
			BlockPos column = surface(level, center.relative(along, i));
			if (column != null) {
				moved += raise(ctx, water, column, Direction.UP, FmabTags.elementBlocks("water"), WALL_HEIGHT, 0,
						Effects::frozen);
			}
		}
		return moved > 0 ? Result.DONE : Result.NO_MATERIAL;
	}

	/**
	 * Feu + Air + Décomposer : la matière visée devient explosif et saute. Le bloc qui explose
	 * disparaît : c'est lui qui a été transmuté. Les dégâts aux blocs suivent la règle mobGriefing.
	 */
	private static Result detonate(EffectContext ctx) {
		ServerLevel level = ctx.level();
		BlockPos target = surface(level, ctx.origin().relative(ctx.horizontalDirection(), ctx.range()));
		if (target == null) {
			return Result.NO_TARGET;
		}
		BlockPos charge = target.below();
		BlockState state = level.getBlockState(charge);
		if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, charge) < 0) {
			return Result.NO_MATERIAL;
		}
		level.removeBlock(charge, false);
		float power = ctx.damage(BLAST_POWER + (float) ctx.perk("blast_power"));
		Vec3 c = Vec3.atCenterOf(charge);
		level.explode(ctx.caster(), c.x, c.y, c.z, power, Level.ExplosionInteraction.MOB);
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
	 * Empile jusqu'à {@code maxHeight} blocs de l'élément à partir de {@code column}, dans la
	 * direction {@code up}. Ils viennent d'abord de la matière de l'étage précédent, puis des
	 * positions de {@code quarry}. Les créatures prises dans la colonne sont repoussées au bout et,
	 * si {@code damage} est positif, blessées.
	 *
	 * @return nombre de blocs déplacés
	 */
	private static int raise(EffectContext ctx, List<BlockPos> quarry, BlockPos column, Direction up,
			TagKey<Block> element, int maxHeight, float damage) {
		return raise(ctx, quarry, column, up, element, maxHeight, damage, UnaryOperator.identity());
	}

	/**
	 * Comme {@link #raise(EffectContext, List, BlockPos, Direction, TagKey, int, float)}, mais la
	 * matière est reformée par {@code recompose} : l'eau puisée autour devient de la glace.
	 */
	private static int raise(EffectContext ctx, List<BlockPos> quarry, BlockPos column, Direction up,
			TagKey<Block> element, int maxHeight, float damage, UnaryOperator<BlockState> recompose) {
		ServerLevel level = ctx.level();
		int height = 0;
		Iterator<BlockPos> world = quarry.iterator();
		while (height < maxHeight) {
			BlockPos to = column.relative(up, height);
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
			level.setBlockAndUpdate(to, recompose.apply(state));
			height++;
		}
		if (height > 0) {
			AABB space = new AABB(column).expandTowards(up.getStepX() * (height - 1), up.getStepY() * (height - 1),
					up.getStepZ() * (height - 1));
			Vec3 tip = Vec3.atBottomCenterOf(column.relative(up, height));
			for (Entity e : level.getEntities((Entity) null, space, Entity::isAlive)) {
				if (up == Direction.UP) {
					e.setPos(e.getX(), tip.y, e.getZ());
				}
				if (damage > 0 && e instanceof LivingEntity living) {
					living.hurtServer(level, level.damageSources().magic(), damage);
					living.setDeltaMovement(living.getDeltaMovement().add(up.getStepX() * 0.8,
							up == Direction.DOWN ? -0.4 : 0.8, up.getStepZ() * 0.8));
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
