package com.ajustor.fmab.item;

import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.gate.BodyPart;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Bras ou jambe d'automail. Il se pose sur un membre perdu (chez Winry ou à l'établi d'automail),
 * s'use à l'usage et se répare chez Winry.
 */
public class AutomailItem extends Item {
	/** Les modèles d'automail : le fer standard, l'acier léger de Rush Valley, le modèle d'hiver de Briggs. */
	public enum Model {
		IRON,
		/** Plus léger : la jambe court plus vite, le bras frappe plus vite ; il s'use plus vite. */
		RUSH_VALLEY,
		/** Isolé contre le froid : on ne gèle pas ; plus robuste. */
		BRIGGS;

		public String serializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private final Model model;
	private final boolean arm;

	public AutomailItem(Model model, boolean arm, Properties properties) {
		super(properties);
		this.model = model;
		this.arm = arm;
	}

	public Model model() {
		return model;
	}

	public boolean arm() {
		return arm;
	}

	/** Ce prothèse convient-il à ce membre ? */
	public boolean fits(BodyPart part) {
		return arm ? part.arm() : part.leg();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.automail.model." + model.serializedName())
				.withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.fmab.automail.fit").withStyle(ChatFormatting.DARK_GRAY));
		if (Automails.bladed(stack)) {
			tooltip.accept(Component.translatable("item.fmab.automail.bladed").withStyle(ChatFormatting.AQUA));
		}
		if (Automails.broken(stack)) {
			tooltip.accept(Component.translatable("item.fmab.automail.broken").withStyle(ChatFormatting.RED));
		}
	}
}
