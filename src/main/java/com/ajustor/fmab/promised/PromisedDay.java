package com.ajustor.fmab.promised;

import com.ajustor.fmab.entity.ImmortalSoldierEntity;
import com.ajustor.fmab.platform.event.ServerEvents;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.stone.Eclipse;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.Collection;

/**
 * Le Jour promis, quand l'éclipse vient et que le cercle national tient : le ciel rougit (voir le
 * client), des soldats immortels sortent de terre autour des joueurs, et l'alchimie faiblit partout,
 * sauf à l'abri des points de sang déjà scellés.
 */
public final class PromisedDay {
	/** L'alchimie, hors d'un abri, ne donne que la moitié de sa force. */
	public static final double WEAKENED = 0.5;
	/** Un point de sang scellé abrite l'alchimie dans ce rayon. */
	public static final double SHELTER = 32;
	/** Un soldat de plus toutes les dix secondes, tant qu'il y en a moins que ça autour d'un joueur. */
	static final int SOLDIERS_NEAR = 3;
	private static final int PERIOD = 200;

	private PromisedDay() {
	}

	/** Le Jour promis a-t-il lieu en ce moment dans ce monde ? */
	public static boolean active(ServerLevel level) {
		if (!Eclipse.now(level)) {
			return false;
		}
		NationalCircle circle = NationalCircle.get(level.getServer());
		return !circle.broken() && !circle.fatherFallen();
	}

	/** Ce point est-il à l'abri d'un point de sang scellé ? */
	public static boolean sheltered(BlockPos pos, Collection<BlockPos> sealed) {
		for (BlockPos crest : sealed) {
			double dx = crest.getX() - pos.getX(), dz = crest.getZ() - pos.getZ();
			if (dx * dx + dz * dz <= SHELTER * SHELTER) {
				return true;
			}
		}
		return false;
	}

	/** La force de l'alchimie ici et maintenant : diminuée pendant le Jour promis, hors des abris. */
	public static double alchemy(ServerLevel level, BlockPos pos) {
		if (!active(level) || sheltered(pos, NationalCircle.get(level.getServer()).sealed())) {
			return 1;
		}
		return WEAKENED;
	}

	public static void register() {
		ServerEvents.END_TICK.register(server -> {
			if (server.getTickCount() % PERIOD != 0) {
				return;
			}
			ServerLevel overworld = server.overworld();
			if (!active(overworld)) {
				return;
			}
			for (ServerPlayer p : overworld.players()) {
				if (!p.isSpectator() && !p.isCreative()) {
					raise(overworld, p);
				}
			}
		});
	}

	/** Un soldat immortel sort de terre non loin du joueur, s'il n'y en a pas déjà assez. */
	private static void raise(ServerLevel level, ServerPlayer player) {
		int near = level.getEntitiesOfClass(ImmortalSoldierEntity.class, new AABB(player.blockPosition()).inflate(32))
				.size();
		if (near >= SOLDIERS_NEAR) {
			return;
		}
		double angle = player.getRandom().nextDouble() * Math.PI * 2;
		double distance = 10 + player.getRandom().nextDouble() * 8;
		int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
		int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
		BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
		ImmortalSoldierEntity soldier = FmabEntities.IMMORTAL_SOLDIER.create(level, EntitySpawnReason.EVENT);
		if (soldier == null) {
			return;
		}
		soldier.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getRandom().nextFloat() * 360, 0);
		soldier.setTarget(player);
		level.addFreshEntity(soldier);
	}
}
