package com.ajustor.fmab.util;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Un modificateur réappliqué tel quel ne marque plus l'attribut comme modifié. */
class ModifiersTest {
	private static final Identifier ID = Identifier.fromNamespaceAndPath("fmab", "test");
	private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;

	private int dirty;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private AttributeInstance instance() {
		return new AttributeInstance(Holder.direct(new RangedAttribute("test", 1, 0, 100)), i -> dirty++);
	}

	@Test
	void reapplyingTheSameModifierDoesNothing() {
		AttributeInstance instance = instance();
		Modifiers.set(instance, ID, 2, ADD);
		int afterFirst = dirty;
		for (int tick = 0; tick < 20; tick++) {
			Modifiers.set(instance, ID, 2, ADD);
		}
		assertEquals(afterFirst, dirty);
		assertEquals(3, instance.getValue());
	}

	@Test
	void aNewAmountReplacesTheModifier() {
		AttributeInstance instance = instance();
		Modifiers.set(instance, ID, 2, ADD);
		Modifiers.set(instance, ID, 5, ADD);
		assertEquals(6, instance.getValue());
	}

	@Test
	void zeroRemovesTheModifierOnce() {
		AttributeInstance instance = instance();
		Modifiers.set(instance, ID, 2, ADD);
		Modifiers.set(instance, ID, 0, ADD);
		int afterRemoval = dirty;
		Modifiers.set(instance, ID, 0, ADD);
		assertNull(instance.getModifier(ID));
		assertEquals(afterRemoval, dirty);
		assertEquals(1, instance.getValue());
	}
}
