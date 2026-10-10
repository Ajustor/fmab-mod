package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Tattoos;
import com.ajustor.fmab.data.Transient;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.tattoo.TattooSlot;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cercles portés : brodés sur les vêtements ou tatoués hors des paumes. Un cercle porté qui
 * fonctionne pour son porteur lui donne en permanence le passif de chacun de ses effets.
 *
 * <ul>
 *   <li>{@code fmab:reinforce} : +2 d'armure ;</li>
 *   <li>{@code fmab:fire_ward} : résistance au feu ;</li>
 *   <li>{@code fmab:swiftness} : +10 % de vitesse ;</li>
 *   <li>{@code fmab:mending} : le corps se répare lentement ;</li>
 *   <li>{@code fmab:thorns} : qui frappe le porteur est blessé en retour.</li>
 * </ul>
 */
public final class Passives {
	private static final int PERIOD = 40;
	private static final Identifier ARMOR = Fmab.id("passive_reinforce");
	private static final Identifier SPEED = Fmab.id("passive_swiftness");
	private static final double ARMOR_PER_CIRCLE = 2;
	private static final double SPEED_PER_CIRCLE = 0.1;
	private static final float MENDING_PER_PERIOD = 0.5f;
	private static final float THORNS_DAMAGE = 1.5f;
	private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
			EquipmentSlot.FEET};
	private static final int CACHE_SIZE = 256;

	/** Passifs actifs par joueur, recalculés toutes les deux secondes. */
	private static final Map<UUID, Map<String, Integer>> ACTIVE = Transient.perPlayer(new HashMap<>());
	/** Analyses des cercles portés : on ne relit pas un tracé inchangé toutes les deux secondes. */
	private static final Map<CacheKey, Analysis> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<CacheKey, Analysis> eldest) {
			return size() > CACHE_SIZE;
		}
	};

	/** Les règles pour lesquelles {@link #CACHE} vaut : un autre monde, d'autres data packs, on oublie tout. */
	private static AlchemyRules cachedFor;

	/** La concentration n'entre pas dans la clé : elle bouge chaque seconde, l'analyse non. */
	private record CacheKey(Drawing drawing, AlchemistData alchemist) {
	}

	static {
		Transient.perServer(CACHE.keySet());
	}

	private Passives() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % PERIOD != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				apply(player, count(player));
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (entity instanceof ServerPlayer player && source.getEntity() instanceof LivingEntity attacker
					&& attacker != player) {
				int thorns = ACTIVE.getOrDefault(player.getUUID(), Map.of()).getOrDefault("fmab:thorns", 0);
				if (thorns > 0) {
					attacker.hurtServer(player.level(), player.level().damageSources().thorns(player),
							THORNS_DAMAGE * thorns);
				}
			}
		});
	}

	/** Les cercles que porte le joueur, et combien de fois chaque passif y figure. */
	private static Map<String, Integer> count(ServerPlayer player) {
		List<Drawing> worn = new ArrayList<>();
		for (EquipmentSlot slot : WORN) {
			Drawing d = player.getItemBySlot(slot).get(FmabComponents.EMBROIDERY);
			if (d != null) {
				worn.add(d);
			}
		}
		Tattoos tattoos = player.getAttachedOrCreate(FmabAttachments.TATTOOS);
		for (TattooSlot slot : TattooSlot.values()) {
			if (!slot.active()) {
				tattoos.get(slot).ifPresent(worn::add);
			}
		}
		Map<String, Integer> out = new HashMap<>();
		if (worn.isEmpty()) {
			return out;
		}
		AlchemistData me = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST).withConcentration(0);
		AlchemyRules rules = AlchemyRules.of(player.level().registryAccess());
		if (rules != cachedFor) {
			CACHE.clear();
			cachedFor = rules;
		}
		for (Drawing d : worn) {
			Analysis a = CACHE.computeIfAbsent(new CacheKey(d, me), k -> rules.analyze(k.drawing(), k.alchemist()));
			if (a.outcome() != Analysis.Outcome.WORKS) {
				continue;
			}
			for (Analysis.StageEffect e : a.effects()) {
				e.combination().passive().ifPresent(p -> out.merge(p, 1, Integer::sum));
			}
		}
		return out;
	}

	private static void apply(ServerPlayer player, Map<String, Integer> passives) {
		ACTIVE.put(player.getUUID(), passives);
		modifier(player, Attributes.ARMOR, ARMOR, ARMOR_PER_CIRCLE * passives.getOrDefault("fmab:reinforce", 0),
				AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.MOVEMENT_SPEED, SPEED, SPEED_PER_CIRCLE * passives.getOrDefault("fmab:swiftness", 0),
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		if (passives.containsKey("fmab:fire_ward")) {
			player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, PERIOD + 20, 0, true, false, true));
		}
		int mending = passives.getOrDefault("fmab:mending", 0);
		if (mending > 0 && player.getHealth() < player.getMaxHealth()) {
			player.heal(MENDING_PER_PERIOD * mending);
		}
	}

	private static void modifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		if (amount == 0) {
			instance.removeModifier(id);
		} else {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
		}
	}
}
