package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.entity.ChimeraBeastEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;

/**
 * Ceux que l'alchimie d'un joueur épargne : lui-même, ses coéquipiers, ses bêtes apprivoisées, ses
 * chimères, et les villageois, qui n'ont rien demandé.
 */
public final class Allies {
	private Allies() {
	}

	public static boolean spares(ServerPlayer caster, Entity target) {
		if (target == caster || caster.isAlliedTo(target) || target instanceof AbstractVillager) {
			return true;
		}
		if (target instanceof OwnableEntity pet && pet.getOwner() == caster) {
			return true;
		}
		return target instanceof ChimeraBeastEntity chimera
				&& chimera.master().map(caster.getUUID()::equals).orElse(false);
	}
}
