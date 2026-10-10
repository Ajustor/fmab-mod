package com.ajustor.fmab.item;

import com.ajustor.fmab.data.Transient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * L'épée de Xing, celle de Ling Yao : légère et rapide. Les coups qui s'enchaînent sans pause
 * frappent de plus en plus fort. Accroupi, l'épée en main, on perçoit le qi des êtres vivants
 * alentour, à travers les murs.
 */
public class XingSwordItem extends Item {
	/** Délai maximal entre deux coups d'un même enchaînement, en ticks. */
	private static final int CHAIN = 30;
	private static final int MAX_COMBO = 4;
	private static final double QI_RANGE = 20;

	private record Combo(long last, int count) {
	}

	/** Par attaquant, joueur ou monstre : les combos éteints s'effacent quand la table grossit. */
	private static final Map<UUID, Combo> COMBOS = Transient.perServerMap(new HashMap<>());
	private static final int PRUNE_ABOVE = 256;

	public XingSwordItem(Properties properties) {
		super(properties);
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.postHurtEnemy(stack, target, attacker);
		if (!(attacker.level() instanceof ServerLevel level)) {
			return;
		}
		long now = level.getGameTime();
		Combo combo = COMBOS.get(attacker.getUUID());
		int count = combo != null && now - combo.last() <= CHAIN ? Math.min(MAX_COMBO, combo.count() + 1) : 0;
		if (COMBOS.size() > PRUNE_ABOVE) {
			COMBOS.values().removeIf(c -> now - c.last() > CHAIN);
		}
		COMBOS.put(attacker.getUUID(), new Combo(now, count));
		if (count > 0 && target.isAlive()) {
			target.invulnerableTime = 0;
			target.hurtServer(level, level.damageSources().mobAttack(attacker), count);
			if (attacker instanceof Player p) {
				p.sendOverlayMessage(Component.translatable("item.fmab.xing_sword.combo", count + 1));
			}
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		if (slot != EquipmentSlot.MAINHAND || !(owner instanceof Player p) || !p.isShiftKeyDown()
				|| level.getGameTime() % 20 != 0) {
			return;
		}
		// La perception du qi : chaque être vivant proche luit à travers les murs.
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(QI_RANGE),
				e -> e != p && e.isAlive())) {
			e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.xing_sword.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
