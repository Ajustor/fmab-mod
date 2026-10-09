package com.ajustor.fmab.stone;

import com.ajustor.fmab.entity.HomunculusEntity;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.Effects;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Le prix d'une Pierre philosophale : des vies. Le cercle de transmutation humaine, avec du sang
 * cristallisé posé dessus, consume tout ce qui vit dans son aire et condense leurs âmes en une
 * Pierre. Un villageois vaut douze âmes, une bête trois. Le karma s'effondre.
 */
public final class Sacrifice {
	private static final int VILLAGER_SOULS = 12;
	private static final int BEAST_SOULS = 3;
	/** En dessous, il n'y a pas de quoi condenser une Pierre : les âmes se dispersent. */
	private static final int MIN_SOULS = 10;
	private static final int MAX_SOULS = 100;
	private static final DustParticleOptions RED = new DustParticleOptions(0xD01020, 1.2f);

	private Sacrifice() {
	}

	public static Effects.Result perform(ServerLevel level, ServerPlayer caster, BlockPos circle, AABB area,
			List<ItemEntity> items) {
		List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive()
				&& !(e instanceof Player) && !(e instanceof HomunculusEntity));
		int villagers = 0;
		int beasts = 0;
		for (LivingEntity victim : victims) {
			if (victim instanceof AbstractVillager) {
				villagers++;
			} else {
				beasts++;
			}
		}
		int souls = Math.min(MAX_SOULS, villagers * VILLAGER_SOULS + beasts * BEAST_SOULS);
		if (souls < MIN_SOULS) {
			caster.sendOverlayMessage(Component.translatable("stone.fmab.sacrifice_too_few", MIN_SOULS));
			return Effects.Result.NO_TARGET;
		}
		for (ItemEntity e : items) {
			if (e.getItem().is(FmabItems.CRYSTALLIZED_BLOOD)) {
				e.getItem().shrink(1);
				if (e.getItem().isEmpty()) {
					e.discard();
				}
				break;
			}
		}
		for (LivingEntity victim : victims) {
			level.sendParticles(RED, victim.getX(), victim.getY(0.5), victim.getZ(), 30, 0.3, 0.6, 0.3, 0);
			victim.kill(level);
		}
		ItemStack stone = new ItemStack(FmabItems.PHILOSOPHER_STONE);
		stone.setDamageValue(stone.getMaxDamage() - souls);
		Vec3 at = Vec3.atCenterOf(circle);
		level.addFreshEntity(new ItemEntity(level, at.x, at.y, at.z, stone));
		TransmutationLightning.discharge(level, circle, 3, 1.2);
		level.sendParticles(RED, at.x, at.y + 0.5, at.z, 200, 2, 0.5, 2, 0);
		level.playSound(null, circle, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.8f, 1.6f);
		Karma.add(caster, -(villagers * 10 + beasts * 2));
		caster.sendSystemMessage(Component.translatable("stone.fmab.sacrifice_done", souls));
		return Effects.Result.DONE;
	}
}
