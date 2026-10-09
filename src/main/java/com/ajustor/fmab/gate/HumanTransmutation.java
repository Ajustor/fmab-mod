package com.ajustor.fmab.gate;

import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.stone.LivingStone;
import com.ajustor.fmab.stone.Sacrifice;
import com.ajustor.fmab.transmutation.EffectContext;
import com.ajustor.fmab.transmutation.Effects;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/**
 * La transmutation humaine : le glyphe Humain et Recomposer, sur les ingrédients d'un corps et le
 * sang de l'alchimiste. Elle échoue toujours. Ce qui naît du cercle n'est pas humain, et
 * l'alchimiste est happé vers sa Porte, où la Vérité prend son péage.
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

	private HumanTransmutation() {
	}

	public static void register() {
		Effects.register("fmab:human_transmutation", HumanTransmutation::apply);
	}

	private static Effects.Result apply(EffectContext ctx) {
		ServerPlayer caster = ctx.caster();
		ServerLevel level = ctx.level();
		BlockPos circle = ctx.circle();
		GateState gate = caster.getAttachedOrCreate(FmabAttachments.GATE);
		if (gate.visit().isPresent()) {
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
		int sets = sets(items);
		consume(level, items, sets, Vec3.atCenterOf(circle));

		// Les « matériaux humains » : villageois et joueurs debout dans le cercle.
		int humans = level.getEntitiesOfClass(LivingEntity.class, area,
				e -> e != caster && e.isAlive() && (e instanceof AbstractVillager || e instanceof Player)).size();
		boolean severe = sets == 0;
		int ambition = Math.max(0, sets - 1) + 2 * humans + gate.openings();

		caster.hurtServer(level, level.damageSources().magic(), BLOOD);
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, caster.getX(), caster.getY(1), caster.getZ(), 8,
				0.3, 0.3, 0.3, 0.1);
		level.sendParticles(ParticleTypes.SQUID_INK, circle.getX() + 0.5, circle.getY() + 0.2, circle.getZ() + 0.5,
				80, 1.5, 0.3, 1.5, 0.05);
		level.playSound(null, circle, SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.5f, 0.6f);
		if (!severe) {
			spawnCreature(level, circle);
		}
		caster.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
		caster.sendSystemMessage(Component.translatable(severe ? "gate.fmab.pulled_severe" : "gate.fmab.pulled"));
		caster.setAttached(FmabAttachments.GATE, gate.withVisit(new GateState.Visit(
				level.dimension().identifier().toString(), circle, ambition, severe, -1)));
		return Effects.Result.DONE;
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
