package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.entity.ChimeraBeastEntity;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.stone.Karma;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;

import java.util.List;

/**
 * La fabrication d'une chimère (glyphe Humain + Décomposer, école de la Vie) : deux bêtes posées dans
 * le cercle sont défaites et recomposées en une seule créature, qui obéit à son créateur. Le travail
 * de Shou Tucker ; un tabou : le karma chute lourdement.
 */
public final class ChimeraTransmutation {
	private static final int KARMA = -15;
	private static final DustParticleOptions FLESH = new DustParticleOptions(0x9A3040, 1.2f);

	private ChimeraTransmutation() {
	}

	public static void register() {
		Effects.register("fmab:chimera", ChimeraTransmutation::apply);
	}

	private static Effects.Result apply(EffectContext ctx) {
		ServerLevel level = ctx.level();
		List<Animal> beasts = level.getEntitiesOfClass(Animal.class, ctx.onCircle().inflate(1, 1, 1),
				// Pas les compagnons des autres : ni bête apprivoisée (chevaux compris), nommée, en laisse ou montée.
				a -> a.isAlive() && !a.isBaby() && !(a instanceof OwnableEntity o && o.getOwnerReference() != null)
						&& !a.hasCustomName() && !a.isLeashed() && !a.isVehicle());
		if (beasts.size() < 2) {
			return Effects.Result.NO_TARGET;
		}
		ChimeraBeastEntity chimera = FmabEntities.CHIMERA_BEAST.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (chimera == null) {
			return Effects.Result.NO_TARGET;
		}
		BlockPos c = ctx.circle();
		for (Animal beast : beasts.subList(0, 2)) {
			level.sendParticles(FLESH, beast.getX(), beast.getY(0.5), beast.getZ(), 40, 0.4, 0.4, 0.4, 0);
			beast.discard();
		}
		chimera.snapTo(c.getX() + 0.5, c.getY() + 0.1, c.getZ() + 0.5, ctx.caster().getYRot(), 0);
		chimera.bindTo(ctx.caster());
		// Une greffe maîtrisée tient mieux : la chimère est plus robuste.
		double graft = ctx.knowledge() == null ? 0 : ctx.knowledge().perk("life", "chimera_vigor");
		if (graft > 0) {
			chimera.getAttribute(Attributes.MAX_HEALTH).setBaseValue(chimera.getMaxHealth() * (1 + graft * 0.5));
			chimera.setHealth(chimera.getMaxHealth());
		}
		level.addFreshEntity(chimera);
		level.sendParticles(FLESH, c.getX() + 0.5, c.getY() + 0.8, c.getZ() + 0.5, 80, 0.8, 0.6, 0.8, 0);
		level.playSound(null, c, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.2f, 0.6f);
		ServerPlayer caster = ctx.caster();
		if (!caster.isCreative()) {
			Karma.add(caster, KARMA);
		}
		caster.sendSystemMessage(Component.translatable("transmutation.fmab.chimera_made").withStyle(ChatFormatting.DARK_RED));
		return Effects.Result.DONE;
	}
}
