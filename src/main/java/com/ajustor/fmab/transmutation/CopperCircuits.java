package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.platform.event.ServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Le cuivre conduit : l'alchimie s'en sert pour toucher aux circuits de redstone. Cuivre + Fixer
 * alimente (un bloc de redstone se forme devant le cercle, le temps de {@link #CHARGE} ticks), Cuivre
 * + Projeter coupe (les fils, répéteurs, comparateurs et torches alentour se défont).
 */
public final class CopperCircuits {
	/** Dix secondes de courant. */
	static final int CHARGE = 20 * 10;
	private static final DustParticleOptions SPARK = new DustParticleOptions(0xE07A3A, 1.0f);
	/** Les blocs de redstone posés par l'alchimie, et quand ils se défont. */
	private static final List<Charge> CHARGES = new ArrayList<>();

	private record Charge(ResourceKey<Level> dimension, BlockPos pos, long until) {
	}

	private CopperCircuits() {
	}

	public static void register() {
		Effects.register("fmab:redstone_power", CopperCircuits::power);
		Effects.register("fmab:redstone_cut", CopperCircuits::cut);
		ServerEvents.STOPPED.register(server -> CHARGES.clear());
		ServerEvents.END_TICK.register(server -> CHARGES.removeIf(c -> {
			ServerLevel level = server.getLevel(c.dimension());
			if (level == null) {
				return true;
			}
			if (level.getGameTime() < c.until()) {
				return false;
			}
			if (level.getBlockState(c.pos()).is(Blocks.REDSTONE_BLOCK)) {
				level.setBlockAndUpdate(c.pos(), Blocks.AIR.defaultBlockState());
			}
			return true;
		}));
	}

	/** Cuivre + Fixer : un bloc de redstone devant le cercle, le temps d'une charge. */
	private static Effects.Result power(EffectContext ctx) {
		ServerLevel level = ctx.level();
		Direction d = ctx.horizontalDirection();
		BlockPos at = ctx.origin().relative(d, ctx.range());
		if (!level.getBlockState(at).canBeReplaced()) {
			return Effects.Result.NO_TARGET;
		}
		level.setBlockAndUpdate(at, Blocks.REDSTONE_BLOCK.defaultBlockState());
		CHARGES.add(new Charge(level.dimension(), at.immutable(), level.getGameTime() + CHARGE));
		level.sendParticles(SPARK, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0);
		return Effects.Result.DONE;
	}

	/** Cuivre + Projeter : les circuits alentour se défont, et rendent leurs pièces. */
	private static Effects.Result cut(EffectContext ctx) {
		ServerLevel level = ctx.level();
		int r = ctx.range() + 2;
		int cut = 0;
		for (BlockPos p : BlockPos.betweenClosed(ctx.origin().offset(-r, -1, -r), ctx.origin().offset(r, 2, r))) {
			if (conducts(level.getBlockState(p))) {
				level.destroyBlock(p, true, ctx.caster());
				level.sendParticles(SPARK, p.getX() + 0.5, p.getY() + 0.3, p.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0);
				cut++;
			}
		}
		return cut > 0 ? Effects.Result.DONE : Effects.Result.NO_TARGET;
	}

	/** Ce que la coupure défait : ce qui porte le signal. */
	static boolean conducts(BlockState state) {
		return state.is(Blocks.REDSTONE_WIRE) || state.is(Blocks.REPEATER) || state.is(Blocks.COMPARATOR)
				|| state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH);
	}
}
