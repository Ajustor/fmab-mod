package com.ajustor.fmab.stone;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.platform.event.ServerEvents;
import com.ajustor.fmab.util.Modifiers;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La proie d'une Pierre vivante est paralysée tant qu'on la tient : elle ne marche plus, ne saute
 * plus, ne frappe plus, et une créature cesse de penser (ni cible, ni chemin, ni attaque). Les
 * attributs bloqués lui sont rendus dès qu'on la lâche.
 */
public final class Paralysis {
	private static final Identifier LOCK = Fmab.id("paralysis");
	private static final List<Holder<Attribute>> LOCKED = List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH);

	/** Un être tenu, jusqu'à quelle heure de jeu. */
	private record Hold(LivingEntity entity, long until) {
	}

	private static final Map<UUID, Hold> HELD = new HashMap<>();

	private Paralysis() {
	}

	/** La prise tient-elle encore à cette heure ? */
	public static boolean holds(long until, long now) {
		return now < until;
	}

	/** L'être est paralysé {@code ticks} ticks de plus (la prise se renouvelle à chaque pulsation). */
	public static void hold(LivingEntity entity, int ticks) {
		HELD.put(entity.getUUID(), new Hold(entity, entity.level().getGameTime() + ticks));
		for (Holder<Attribute> attribute : LOCKED) {
			Modifiers.set(entity, attribute, LOCK, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}
		entity.setDeltaMovement(new Vec3(0, Math.min(0, entity.getDeltaMovement().y), 0));
		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
		}
	}

	/** L'être est-il paralysé ? Côté serveur seulement. */
	public static boolean held(LivingEntity entity) {
		if (entity.level().isClientSide()) {
			return false;
		}
		Hold hold = HELD.get(entity.getUUID());
		return hold != null && holds(hold.until(), entity.level().getGameTime());
	}

	public static void register() {
		ServerEvents.STOPPED.register(server -> HELD.clear());
		ServerEvents.END_TICK.register(server -> {
			for (Iterator<Hold> it = HELD.values().iterator(); it.hasNext(); ) {
				Hold hold = it.next();
				LivingEntity entity = hold.entity();
				if (entity.isRemoved() || !holds(hold.until(), entity.level().getGameTime())) {
					release(entity);
					it.remove();
				}
			}
		});
	}

	private static void release(LivingEntity entity) {
		for (Holder<Attribute> attribute : LOCKED) {
			Modifiers.set(entity, attribute, LOCK, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}
	}
}
