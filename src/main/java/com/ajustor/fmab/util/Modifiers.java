package com.ajustor.fmab.util;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Pose, change ou retire un modificateur d'attribut, et ne touche à rien quand il est déjà en place.
 * Minecraft marque l'attribut modifié (recalcul, et paquet aux clients pour la vitesse ou la santé)
 * à chaque pose, même d'un modificateur identique : les règles qui le réappliquent à chaque tick
 * passent donc par ici.
 */
public final class Modifiers {
	private Modifiers() {
	}

	/** Le modificateur {@code id} vaut {@code amount} (0 : il disparaît). */
	public static void set(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance != null) {
			set(instance, id, amount, operation);
		}
	}

	public static void set(AttributeInstance instance, Identifier id, double amount, AttributeModifier.Operation operation) {
		AttributeModifier current = instance.getModifier(id);
		if (amount == 0) {
			if (current != null) {
				instance.removeModifier(id);
			}
			return;
		}
		if (current != null && current.amount() == amount && current.operation() == operation) {
			return;
		}
		instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
	}
}
