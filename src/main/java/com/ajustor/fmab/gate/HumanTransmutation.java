package com.ajustor.fmab.gate;

import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.stone.Karma;
import com.ajustor.fmab.stone.LivingStone;
import com.ajustor.fmab.stone.PhilosopherStones;
import com.ajustor.fmab.stone.Sacrifice;
import com.ajustor.fmab.transmutation.EffectContext;
import com.ajustor.fmab.transmutation.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * La transmutation humaine : le glyphe Humain et Recomposer, sur les ingrédients d'un corps et le
 * sang de l'alchimiste. Elle échoue toujours. Ce qui naît du cercle n'est pas humain, et
 * l'alchimiste est happé vers sa Porte, où la Vérité prend son péage.
 *
 * <p>Une Pierre philosophale en main change la donne : l'alchimiste choisit d'ouvrir sa Porte (et
 * d'y racheter ce qu'il a perdu avec les âmes de la Pierre, voir {@link StoneBargain}), ou de donner
 * une âme de la Pierre à l'être qui naît du cercle (voir {@link StoneChoice}).
 */
public final class HumanTransmutation {
	/**
	 * Un ingrédient du corps humain, à poser sur le cercle.
	 *
	 * @param nameKey clé de traduction de ce qu'il représente (eau, carbone...)
	 */
	public record Ingredient(String nameKey, Predicate<ItemStack> matches, int count) {
	}

	/** Eau, carbone, chaux, phosphore, salpêtre, fer et silicium : de quoi faire un adulte. */
	public static final List<Ingredient> INGREDIENTS = List.of(
			new Ingredient("gate.fmab.ingredient.water", s -> s.is(Items.WATER_BUCKET), 1),
			new Ingredient("gate.fmab.ingredient.carbon", s -> s.is(ItemTags.COALS), 8),
			new Ingredient("gate.fmab.ingredient.lime", s -> s.is(Items.BONE_MEAL), 4),
			new Ingredient("gate.fmab.ingredient.phosphorus", s -> s.is(Items.GLOWSTONE_DUST), 2),
			new Ingredient("gate.fmab.ingredient.saltpeter", s -> s.is(Items.GUNPOWDER), 2),
			new Ingredient("gate.fmab.ingredient.iron", s -> s.is(Items.IRON_NUGGET), 1),
			new Ingredient("gate.fmab.ingredient.silicon", s -> s.is(Items.QUARTZ), 1));

	/** Le sang de l'alchimiste : la part qu'il met de lui-même dans le cercle. */
	private static final float BLOOD = 4;
	/** Jusqu'où l'on cherche les ingrédients et les humains visés autour du cercle. */
	private static final double REACH = 2.5;
	/** L'âme que la Pierre donne à l'être créé. */
	private static final int BEING_SOUL = 1;
	/** Donner à un corps l'âme d'un autre : un tabou de plus. */
	private static final int BEING_KARMA = -10;
	private static final DustParticleOptions STONE_RED = new DustParticleOptions(0xD01020, 1.2f);

	private HumanTransmutation() {
	}

	public static void register() {
		Effects.register("fmab:human_transmutation", HumanTransmutation::apply);
	}

	private static Effects.Result apply(EffectContext ctx) {
		ServerPlayer caster = ctx.caster();
		ServerLevel level = ctx.level();
		BlockPos circle = ctx.circle();
		if (caster.getAttachedOrCreate(FmabAttachments.GATE).visit().isPresent()) {
			return Effects.Result.NO_TARGET;
		}
		if (LivingStone.tryRitual(caster, circle)) {
			return Effects.Result.DONE;
		}
		AABB area = new AABB(circle).inflate(REACH, 1, REACH);
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive);
		if (items.stream().anyMatch(e -> e.getItem().is(FmabItems.CRYSTALLIZED_BLOOD))) {
			return Sacrifice.perform(level, caster, circle, area, items);
		}
		if (PhilosopherStones.held(caster).isPresent()) {
			// La Pierre en main : l'alchimiste choisit d'ouvrir sa Porte ou de donner une âme à un être.
			StoneChoice.ask(caster, circle);
			return Effects.Result.DONE;
		}
		openGate(caster, level, circle);
		return Effects.Result.DONE;
	}

	/**
	 * La transmutation humaine ordinaire : les ingrédients se consument, l'alchimiste donne son sang,
	 * ce qui naît du cercle n'est pas humain, et la Porte l'appelle.
	 */
	static void openGate(ServerPlayer caster, ServerLevel level, BlockPos circle) {
		GateState gate = caster.getAttachedOrCreate(FmabAttachments.GATE);
		AABB area = new AABB(circle).inflate(REACH, 1, REACH);
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive);
		int sets = sets(items);
		consume(level, items, sets, Vec3.atCenterOf(circle));

		// Les « matériaux humains » : villageois et joueurs debout dans le cercle.
		int humans = level.getEntitiesOfClass(LivingEntity.class, area,
				e -> e != caster && e.isAlive() && (e instanceof AbstractVillager || e instanceof Player)).size();
		boolean severe = sets == 0;
		int ambition = Math.max(0, sets - 1) + 2 * humans + gate.openings();

		bleed(caster, level);
		level.sendParticles(ParticleTypes.SQUID_INK, circle.getX() + 0.5, circle.getY() + 0.2, circle.getZ() + 0.5,
				80, 1.5, 0.3, 1.5, 0.05);
		level.playSound(null, circle, SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.5f, 0.6f);
		if (!severe) {
			spawnCreature(level, circle);
		}
		caster.sendSystemMessage(Component.translatable(severe ? "gate.fmab.pulled_severe" : "gate.fmab.pulled"));
		// Des bras noirs jaillissent du cercle et agrippent l'alchimiste : la Porte l'appelle.
		GateOfTruth.pullFromCircle(level, circle, caster);
		caster.setAttached(FmabAttachments.GATE, gate.withVisit(new GateState.Visit(
				level.dimension().identifier().toString(), circle, ambition, severe, -GateOfTruth.PULL)));
	}

	/**
	 * Avec la Pierre, l'être qui naît du cercle reçoit une âme : un corps complet d'ingrédients et une
	 * âme de la Pierre, et il vit, sous la forme que l'alchimiste a choisie. Une bête s'attache à
	 * lui. La Porte ne s'ouvre pas ; mais cette âme était celle de quelqu'un.
	 */
	static void createBeing(ServerPlayer caster, ServerLevel level, BlockPos circle, BeingKind kind) {
		AABB area = new AABB(circle).inflate(REACH, 1, REACH);
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive);
		if (sets(items) == 0) {
			caster.sendSystemMessage(Component.translatable("gate.fmab.being.no_body"));
			return;
		}
		Optional<ItemStack> stone = PhilosopherStones.held(caster);
		if (stone.isEmpty()) {
			caster.sendSystemMessage(Component.translatable("gate.fmab.being.no_stone"));
			return;
		}
		Mob being = kind.type().create(level, EntitySpawnReason.MOB_SUMMONED);
		if (being == null) {
			return;
		}
		consume(level, items, 1, Vec3.atCenterOf(circle));
		PhilosopherStones.drain(caster, stone.get(), BEING_SOUL);
		bleed(caster, level);
		being.setPos(Vec3.atBottomCenterOf(circle));
		being.setCustomName(Component.translatable("entity.fmab.transmuted_being"));
		being.setPersistenceRequired();
		if (being instanceof TamableAnimal pet) {
			pet.tame(caster);
		} else if (being instanceof AbstractHorse horse) {
			horse.tameWithName(caster);
		}
		level.addFreshEntity(being);
		level.sendParticles(STONE_RED, circle.getX() + 0.5, circle.getY() + 0.5, circle.getZ() + 0.5,
				150, 1.2, 0.8, 1.2, 0);
		level.playSound(null, circle, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 1.2f, 0.8f);
		Karma.add(caster, BEING_KARMA);
		caster.sendSystemMessage(Component.translatable("gate.fmab.being.created"));
	}

	/** Le sang qu'on donne : jamais jusqu'à la mort, la Porte veut son dû vivant. */
	private static void bleed(ServerPlayer caster, ServerLevel level) {
		caster.hurtServer(level, level.damageSources().magic(), Math.min(BLOOD, caster.getHealth() - 1));
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, caster.getX(), caster.getY(1), caster.getZ(), 8,
				0.3, 0.3, 0.3, 0.1);
	}

	/** Combien de corps complets les ingrédients posés permettent. */
	public static int sets(List<ItemEntity> items) {
		int sets = Integer.MAX_VALUE;
		for (Ingredient ingredient : INGREDIENTS) {
			int count = 0;
			for (ItemEntity e : items) {
				if (ingredient.matches().test(e.getItem())) {
					count += e.getItem().getCount();
				}
			}
			sets = Math.min(sets, count / ingredient.count());
		}
		return sets;
	}

	/**
	 * Les ingrédients sont consommés : pour chaque corps tenté, ou tous si le compte n'y est pas (la
	 * matière se perd dans l'échec). Les seaux d'eau rendent leur seau.
	 */
	private static void consume(ServerLevel level, List<ItemEntity> items, int sets, Vec3 at) {
		for (Ingredient ingredient : INGREDIENTS) {
			int left = sets == 0 ? Integer.MAX_VALUE : sets * ingredient.count();
			for (ItemEntity e : items) {
				ItemStack stack = e.getItem();
				if (left <= 0 || !ingredient.matches().test(stack)) {
					continue;
				}
				int taken = Math.min(left, stack.getCount());
				left -= taken;
				if (stack.is(Items.WATER_BUCKET)) {
					level.addFreshEntity(new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.BUCKET, taken)));
				}
				stack.shrink(taken);
				if (stack.isEmpty()) {
					e.discard();
				} else {
					e.setItem(stack);
				}
			}
		}
	}

	/** Ce qui naît du cercle n'a rien d'humain. */
	private static void spawnCreature(ServerLevel level, BlockPos circle) {
		Husk creature = EntityTypes.HUSK.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (creature == null) {
			return;
		}
		creature.setPos(Vec3.atBottomCenterOf(circle));
		creature.setCustomName(Component.translatable("entity.fmab.failed_transmutation"));
		creature.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 60 * 10, 1));
		creature.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 60, 0));
		level.addFreshEntity(creature);
	}
}
