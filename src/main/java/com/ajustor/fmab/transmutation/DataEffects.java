package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.compat.EntityCompat;
import com.ajustor.fmab.registry.FmabTags;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Les effets décrits en données, dans le {@code spec} d'une combinaison : ce que fait un cercle sans
 * qu'il faille mettre le mod à jour. Un effet est une étape, ou une suite d'étapes ({@code "steps"}) :
 *
 * <pre>{@code
 * "spec": { "target": "blocks", "zone": "sphere", "filter": "#minecraft:ice", "operation": "break" }
 * "spec": { "steps": [ { "zone": "wall", "operation": "place" },
 *                      { "target": "entities", "filter": "hostile", "operation": "push", "strength": 1.2 } ] }
 * }</pre>
 *
 * <ul>
 * <li>{@code target} : {@code blocks} (par défaut), {@code entities}, {@code items}, ou {@code function}.</li>
 * <li>{@code zone} (blocs) : {@code sphere}, {@code cube}, {@code below} (le sol sous le cercle),
 * {@code wall} (un mur devant le cercle), {@code column} (une colonne à portée, dans la direction),
 * {@code area} (la ligne, le cône ou le disque que dessinent les glyphes de forme). La taille suit la
 * portée du cercle ; une zone d'entités ou d'objets est la sphère de la portée.</li>
 * <li>{@code filter} : {@code element} (la matière de l'élément du cercle, par défaut), un tag
 * {@code #ns:tag} ou un identifiant ; pour les créatures, aussi {@code living}, {@code hostile},
 * {@code animal}.</li>
 * <li>{@code operation} : pour les blocs, {@code break}, {@code place} (option {@code material},
 * {@code damage}), {@code freeze}, {@code drain} ; pour les créatures, {@code damage}
 * ({@code amount}), {@code effect} ({@code effect}, {@code duration} en secondes, {@code amplifier}),
 * {@code push} ({@code strength}), {@code ignite} ({@code seconds}), {@code heal} ({@code amount}),
 * {@code grow_up} ; pour les objets, {@code dismantle}, {@code collect}.</li>
 * <li>{@code target: effect} : un effet codé du mod ({@code effect}), pour le compléter d'autres étapes.</li>
 * <li>{@code only_aimed: true} : l'étape n'a lieu que si son glyphe de cible est tracé (Créature pour
 * {@code entities}).</li>
 * <li>{@code target: function} : lance la fonction de datapack {@code function} au cercle, avec en
 * paramètres de macro {@code range}, {@code dx}, {@code dz} (la direction) et {@code element}.</li>
 * </ul>
 *
 * <p>L'échange équivalent tient toujours : ce qu'on brise passe dans la matière du cercle, ce qu'on
 * pose se prend sur le cercle, dans l'étage précédent ou dans le sac. Une donnée inconnue ne fait rien
 * (le cercle répond qu'il n'y a rien à portée) plutôt que de planter le serveur.
 */
public final class DataEffects {
	private static final int WALL_HEIGHT = 3;

	private DataEffects() {
	}

	public static Effects.Result apply(EffectContext ctx, JsonObject spec) {
		if (spec.has("steps")) {
			Effects.Result result = Effects.Result.NO_TARGET;
			boolean done = false;
			for (JsonElement step : spec.getAsJsonArray("steps")) {
				Effects.Result r = step(ctx, step.getAsJsonObject());
				done |= r == Effects.Result.DONE;
				if (r == Effects.Result.NO_MATERIAL) {
					result = r;
				}
			}
			return done ? Effects.Result.DONE : result;
		}
		return step(ctx, spec);
	}

	private static Effects.Result step(EffectContext ctx, JsonObject s) {
		try {
			// Une étape « only_aimed » n'a lieu que si son glyphe de cible est tracé : le glyphe Créature
			// ajoute ainsi à un cercle ce qu'il fait aux créatures, sans rien changer à ce qu'il faisait.
			boolean onlyAimed = s.has("only_aimed") && s.get("only_aimed").getAsBoolean();
			return switch (text(s, "target", "blocks")) {
				case "entities" -> !onlyAimed || ctx.stage().aimsEntities() ? entities(ctx, s) : Effects.Result.NO_TARGET;
				case "effect" -> Effects.get(text(s, "effect", "")).map(e -> e.apply(ctx)).orElse(Effects.Result.NO_TARGET);
				case "items" -> ctx.stage().aimsItems() ? items(ctx, s) : Effects.Result.NO_TARGET;
				case "function" -> function(ctx, s);
				default -> ctx.stage().aimsBlocks() ? blocks(ctx, s) : Effects.Result.NO_TARGET;
			};
		} catch (RuntimeException e) {
			com.ajustor.fmab.Fmab.LOGGER.warn("Effet en données illisible ({}) : {}", ctx.stage().combination().id(), e.getMessage());
			return Effects.Result.NO_TARGET;
		}
	}

	// --- blocs ---------------------------------------------------------------------------------------

	private static Effects.Result blocks(EffectContext ctx, JsonObject s) {
		ServerLevel level = ctx.level();
		List<BlockPos> zone = zone(ctx, text(s, "zone", "sphere"));
		String operation = text(s, "operation", "break");
		Predicate<BlockState> filter = blockFilter(ctx, text(s, "filter", "element"));
		int changed = 0;
		switch (operation) {
			case "break" -> {
				for (BlockPos p : zone) {
					BlockState state = level.getBlockState(p);
					if (!state.isAir() && filter.test(state) && !state.hasBlockEntity() && state.getDestroySpeed(level, p) >= 0) {
						ctx.flow().addAll(Block.getDrops(state, level, p, null));
						level.destroyBlock(p, false, ctx.caster());
						changed++;
					}
				}
			}
			case "place" -> {
				String material = text(s, "material", "element");
				float damage = s.has("damage") ? ctx.damage(s.get("damage").getAsFloat()) : 0;
				for (BlockPos p : zone) {
					if (!level.getBlockState(p).canBeReplaced()) {
						continue;
					}
					BlockState block = takeBlock(ctx, material);
					if (block == null) {
						return changed > 0 ? Effects.Result.DONE : Effects.Result.NO_MATERIAL;
					}
					level.setBlockAndUpdate(p, block);
					changed++;
					if (damage > 0) {
						strike(ctx, p, damage);
					}
				}
			}
			case "freeze" -> {
				for (BlockPos p : zone) {
					FluidState fluid = level.getFluidState(p);
					if (fluid.isSource() && fluid.is(Fluids.WATER) && level.getBlockState(p).is(Blocks.WATER)) {
						level.setBlockAndUpdate(p, Blocks.ICE.defaultBlockState());
						changed++;
					} else if (fluid.isSource() && fluid.is(Fluids.LAVA)) {
						level.setBlockAndUpdate(p, Blocks.OBSIDIAN.defaultBlockState());
						changed++;
					}
				}
			}
			case "drain" -> {
				for (BlockPos p : zone) {
					BlockState state = level.getBlockState(p);
					if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
						level.setBlockAndUpdate(p, state.setValue(BlockStateProperties.WATERLOGGED, false));
						changed++;
					} else if (!level.getFluidState(p).isEmpty() && state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
						level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
						changed++;
					}
				}
			}
			default -> {
				return Effects.Result.NO_TARGET;
			}
		}
		return changed > 0 ? Effects.Result.DONE : Effects.Result.NO_TARGET;
	}

	/** Les cases d'une zone, dans l'ordre où agir (du bas vers le haut pour ce qu'on empile). */
	static List<BlockPos> zone(EffectContext ctx, String zone) {
		BlockPos o = ctx.origin();
		int r = ctx.range();
		List<BlockPos> out = new ArrayList<>();
		switch (zone) {
			case "cube" -> BlockPos.betweenClosed(o.offset(-r, -r, -r), o.offset(r, r, r)).forEach(p -> out.add(p.immutable()));
			case "below" -> BlockPos.betweenClosed(o.below().offset(-r, -r, -r), o.below().offset(r, 0, r)).forEach(p -> out.add(p.immutable()));
			case "area" -> out.addAll(Effects.area(ctx, 0));
			case "wall" -> {
				Direction d = ctx.horizontalDirection();
				Direction along = d.getClockWise();
				int height = ctx.grown(WALL_HEIGHT);
				BlockPos base = o.relative(d, 2);
				for (int h = 0; h < height; h++) {
					for (int i = -r; i <= r; i++) {
						out.add(base.relative(along, i).above(h));
					}
				}
			}
			case "column" -> {
				BlockPos base = ctx.frame().onFloor() ? o.relative(ctx.direction(), r) : o;
				Direction up = ctx.frame().onFloor() ? Direction.UP : ctx.frame().normal();
				for (int h = 0; h < ctx.grown(WALL_HEIGHT); h++) {
					out.add(base.relative(up, h));
				}
			}
			default -> {
				for (BlockPos p : BlockPos.betweenClosed(o.offset(-r, -r, -r), o.offset(r, r, r))) {
					if (p.distSqr(o) <= (r + 0.5) * (r + 0.5)) {
						out.add(p.immutable());
					}
				}
			}
		}
		// Le support est de la matière comme une autre : s'il cède, le cercle part avec lui.
		out.remove(ctx.circle());
		return out;
	}

	/** Ce qu'un filtre retient des blocs : la matière de l'élément du cercle, un tag ou un bloc. */
	private static Predicate<BlockState> blockFilter(EffectContext ctx, String filter) {
		if (filter.equals("element")) {
			TagKey<Block> blocks = FmabTags.elementBlocks(element(ctx));
			TagKey<Item> items = FmabTags.elementItems(element(ctx));
			return state -> state.is(blocks) || state.getBlock().asItem().getDefaultInstance().is(items);
		}
		if (filter.equals("any")) {
			return state -> true;
		}
		if (filter.startsWith("#")) {
			TagKey<Block> tag = TagKey.create(Registries.BLOCK, Identifier.parse(filter.substring(1)));
			return state -> state.is(tag);
		}
		Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(filter));
		return state -> block.isPresent() && state.is(block.get());
	}

	/**
	 * Un bloc à poser : dans la matière qui circule entre étages, sur le cercle, puis dans le sac de
	 * l'alchimiste. En créatif, le premier bloc de la matière ne manque jamais.
	 */
	private static BlockState takeBlock(EffectContext ctx, String material) {
		Predicate<ItemStack> wanted = material(ctx, material);
		for (ItemStack stack : ctx.flow()) {
			if (placeable(stack, wanted)) {
				return take(stack);
			}
		}
		for (ItemEntity entity : ctx.level().getEntitiesOfClass(ItemEntity.class, ctx.onCircle())) {
			if (placeable(entity.getItem(), wanted)) {
				return take(entity.getItem());
			}
		}
		ServerPlayer caster = ctx.caster();
		if (caster == null) {
			return null;
		}
		if (caster.isCreative()) {
			for (Item item : BuiltInRegistries.ITEM) {
				ItemStack sample = item.getDefaultInstance();
				if (placeable(sample, wanted)) {
					return ((BlockItem) item).getBlock().defaultBlockState();
				}
			}
			return null;
		}
		for (ItemStack stack : caster.getInventory()) {
			if (placeable(stack, wanted)) {
				return take(stack);
			}
		}
		return null;
	}

	private static boolean placeable(ItemStack stack, Predicate<ItemStack> wanted) {
		return !stack.isEmpty() && stack.getItem() instanceof BlockItem item && wanted.test(stack)
				&& item.getBlock().defaultBlockState().isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
	}

	private static BlockState take(ItemStack stack) {
		BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
		stack.shrink(1);
		return state;
	}

	private static Predicate<ItemStack> material(EffectContext ctx, String material) {
		if (material.equals("element")) {
			TagKey<Item> items = FmabTags.elementItems(element(ctx));
			TagKey<Block> blocks = FmabTags.elementBlocks(element(ctx));
			return stack -> stack.is(items) || ((BlockItem) stack.getItem()).getBlock().defaultBlockState().is(blocks);
		}
		if (material.startsWith("#")) {
			TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(material.substring(1)));
			return stack -> stack.is(tag);
		}
		Optional<Item> item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(material));
		return stack -> item.isPresent() && stack.is(item.get());
	}

	/** Les créatures prises dans un bloc qui vient d'être posé sont soulevées, et blessées. */
	private static void strike(EffectContext ctx, BlockPos p, float damage) {
		for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, new AABB(p), Entity::isAlive)) {
			e.setPos(e.getX(), p.getY() + 1, e.getZ());
			if (!ctx.spares(e)) {
				e.hurtServer(ctx.level(), ctx.magic(), damage);
				ctx.afflict(e);
			}
		}
	}

	// --- créatures -----------------------------------------------------------------------------------

	private static Effects.Result entities(EffectContext ctx, JsonObject s) {
		ServerLevel level = ctx.level();
		Vec3 center = Vec3.atCenterOf(ctx.origin());
		double r = ctx.range();
		Predicate<Entity> filter = entityFilter(text(s, "filter", "living"));
		String operation = text(s, "operation", "damage");
		boolean friendly = operation.equals("heal") || operation.equals("grow_up");
		int touched = 0;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(ctx.origin()).inflate(r),
				e -> e.isAlive() && e.distanceToSqr(center) <= r * r && filter.test(e))) {
			// Ce qui blesse épargne l'alchimiste et ses alliés ; ce qui soigne ou fait grandir, non.
			if (!friendly && ctx.spares(e)) {
				continue;
			}
			switch (operation) {
				case "damage" -> e.hurtServer(level, ctx.magic(), ctx.damage(number(s, "amount", 4)));
				case "effect" -> {
					Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(text(s, "effect", "minecraft:slowness")))
							.map(h -> (Holder<MobEffect>) h).orElseThrow(() -> new IllegalArgumentException("effet inconnu"));
					e.addEffect(new MobEffectInstance(effect, ctx.lasting((int) (20 * number(s, "duration", 5))),
							(int) number(s, "amplifier", 0)), ctx.caster());
				}
				case "push" -> {
					Vec3 away = e.position().subtract(center).normalize().scale(number(s, "strength", 1));
					e.setDeltaMovement(e.getDeltaMovement().add(away.x, 0.3, away.z));
					EntityCompat.syncVelocity(e);
				}
				case "ignite" -> e.igniteForSeconds(number(s, "seconds", 4));
				case "heal" -> e.heal(number(s, "amount", 4));
				case "grow_up" -> {
					if (!(e instanceof AgeableMob young) || !young.isBaby()) {
						continue;
					}
					young.setAge(0);
				}
				default -> {
					return Effects.Result.NO_TARGET;
				}
			}
			if (!friendly) {
				ctx.afflict(e);
			}
			touched++;
		}
		return touched > 0 ? Effects.Result.DONE : Effects.Result.NO_TARGET;
	}

	private static Predicate<Entity> entityFilter(String filter) {
		return switch (filter) {
			case "living" -> e -> true;
			case "hostile" -> e -> e instanceof Enemy;
			case "animal" -> e -> e instanceof Animal;
			default -> {
				if (filter.startsWith("#")) {
					TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(filter.substring(1)));
					yield e -> e.getType().builtInRegistryHolder().is(tag);
				}
				Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(filter));
				yield e -> type.isPresent() && e.getType() == type.get();
			}
		};
	}

	// --- objets --------------------------------------------------------------------------------------

	private static Effects.Result items(EffectContext ctx, JsonObject s) {
		return switch (text(s, "operation", "dismantle")) {
			case "dismantle" -> Recomposition.dismantle(ctx) > 0 ? Effects.Result.DONE : Effects.Result.NO_TARGET;
			case "collect" -> {
				// Les objets au sol à portée viennent sur le cercle.
				Vec3 c = Vec3.atCenterOf(ctx.circle());
				int moved = 0;
				for (ItemEntity item : ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(ctx.origin()).inflate(ctx.range()))) {
					if (!item.getBoundingBox().intersects(ctx.onCircle())) {
						item.teleportTo(c.x, c.y, c.z);
						item.setDeltaMovement(Vec3.ZERO);
						moved++;
					}
				}
				yield moved > 0 ? Effects.Result.DONE : Effects.Result.NO_TARGET;
			}
			default -> Effects.Result.NO_TARGET;
		};
	}

	// --- fonctions de datapack -----------------------------------------------------------------------

	/**
	 * Lance une fonction de datapack au cercle, comme le serveur (tous les droits), avec la portée, la
	 * direction et l'élément du cercle en paramètres de macro. Tout ce que les commandes savent faire
	 * devient un effet d'alchimie.
	 */
	private static Effects.Result function(EffectContext ctx, JsonObject s) {
		String function = text(s, "function", "");
		if (!function.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
			return Effects.Result.NO_TARGET;
		}
		ServerLevel level = ctx.level();
		Direction d = ctx.horizontalDirection();
		String args = String.format(Locale.ROOT, "{range:%d,dx:%d,dz:%d,element:\"%s\"}", ctx.range(), d.getStepX(),
				d.getStepZ(), element(ctx));
		var source = level.getServer().createCommandSourceStack()
				.withLevel(level)
				.withPosition(Vec3.atCenterOf(ctx.circle()))
				.withSuppressedOutput();
		if (ctx.caster() != null) {
			source = source.withEntity(ctx.caster());
		}
		level.getServer().getCommands().performPrefixedCommand(source, "function " + function + " " + args);
		return Effects.Result.DONE;
	}

	// --- outils --------------------------------------------------------------------------------------

	private static String element(EffectContext ctx) {
		return ctx.stage().combination().elements().stream().sorted().findFirst().orElse("earth");
	}

	private static String text(JsonObject s, String key, String fallback) {
		return s.has(key) ? s.get(key).getAsString() : fallback;
	}

	private static float number(JsonObject s, String key, float fallback) {
		return s.has(key) ? s.get(key).getAsFloat() : fallback;
	}
}
