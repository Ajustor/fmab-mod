package com.ajustor.fmab.stone;

import com.ajustor.fmab.entity.ChimeraBeastEntity;
import com.ajustor.fmab.entity.HomunculusEntity;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

/**
 * Le prix d'une Pierre philosophale : des vies. Le cercle de la Pierre philosophale, avec du sang
 * cristallisé posé dessus, consume tout ce qui vit dans son aire (le rayon que l'alchimiste a choisi,
 * voir {@link StoneCircle}) et condense leurs âmes. Un villageois vaut douze âmes, une bête trois.
 *
 * <p>Du sang cristallisé posé sur le cercle : les âmes s'y condensent en Pierres, une par cent âmes,
 * même si quelqu'un se tient au centre. Sans sang, celui qui se tient au centre du cercle devient la
 * Pierre : les âmes se fondent dans son corps, comme en Hohenheim au centre du cercle de Xerxès, et il
 * devient une Pierre philosophale vivante (ou grossit celle qu'il est déjà). Le karma de l'alchimiste
 * s'effondre.
 *
 * <p>Le sacrifice ne foudroie pas d'un coup : il se joue en un rite de quelques secondes, comme à Xerxès
 * (voir {@link StoneRite}).
 */
public final class Sacrifice {
	private static final int VILLAGER_SOULS = 12;
	private static final int BEAST_SOULS = 3;
	/** En dessous, il n'y a pas de quoi condenser une Pierre : les âmes se dispersent. */
	static final int MIN_SOULS = 10;
	/** Ce que tient une Pierre ; au-delà, le cercle en condense plusieurs. */
	static final int MAX_SOULS = 100;
	/** Être au centre du cercle : dans le bloc du cercle, à peu près. */
	private static final double CENTER = 0.8;

	/** Ce qu'un sacrifice consume : tout ce qui vit, sauf les joueurs, les homonculus et ce qui a un maître. */
	static List<LivingEntity> victims(ServerLevel level, AABB area) {
		return level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive()
				&& !(e instanceof Player) && !(e instanceof HomunculusEntity) && !(e instanceof ArmorStand)
				&& !(e instanceof AbstractGolem) && !(e instanceof OwnableEntity o && o.getOwnerReference() != null)
				&& !(e instanceof ChimeraBeastEntity c && c.master().isPresent()));
	}

	/** Celui qui se tient au centre du cercle, s'il y en a un : les âmes iront en lui. */
	public static Optional<ServerPlayer> vessel(ServerLevel level, BlockPos circle) {
		return level.getEntitiesOfClass(ServerPlayer.class, new AABB(circle).inflate(CENTER - 0.5, 1, CENTER - 0.5),
				p -> p.isAlive() && !p.isSpectator()).stream().findFirst();
	}

	/** Les âmes de ces vies : douze par villageois, trois par bête. */
	public static int souls(int villagers, int beasts) {
		return villagers * VILLAGER_SOULS + beasts * BEAST_SOULS;
	}

	/** Les Pierres que donnent tant d'âmes : pleines de cent, la dernière du reste. */
	public static List<Integer> stones(int souls) {
		List<Integer> out = new java.util.ArrayList<>();
		for (int left = souls; left > 0; left -= MAX_SOULS) {
			out.add(Math.min(MAX_SOULS, left));
		}
		return out;
	}

	private Sacrifice() {
	}

	public static Effects.Result perform(ServerLevel level, ServerPlayer caster, BlockPos circle, AABB area,
			List<ItemEntity> items) {
		List<LivingEntity> victims = victims(level, area);
		int villagers = 0;
		int beasts = 0;
		for (LivingEntity victim : victims) {
			if (victim instanceof AbstractVillager) {
				villagers++;
			} else {
				beasts++;
			}
		}
		int souls = souls(villagers, beasts);
		if (souls < MIN_SOULS) {
			caster.sendOverlayMessage(Component.translatable("stone.fmab.sacrifice_too_few", MIN_SOULS));
			return Effects.Result.NO_TARGET;
		}
		// Le sang cristallisé se change en Pierre ; sans lui, c'est celui qui se tient au centre.
		boolean blood = false;
		for (ItemEntity e : items) {
			if (e.getItem().is(FmabItems.CRYSTALLIZED_BLOOD)) {
				e.getItem().shrink(1);
				if (e.getItem().isEmpty()) {
					e.discard();
				}
				blood = true;
				break;
			}
		}
		Optional<ServerPlayer> vessel = blood ? Optional.empty() : vessel(level, circle);
		if (!blood && vessel.isEmpty()) {
			caster.sendOverlayMessage(Component.translatable("stone.fmab.circle_needs_blood"));
			return Effects.Result.NO_MATERIAL;
		}
		// Le cercle est lancé : la faute est commise, même si l'alchimiste s'en va avant la fin du rite.
		Karma.add(caster, -(villagers * 10 + beasts * 2));
		StoneRite.start(level, caster, circle, area.getXsize() / 2 - 0.5, victims, souls, vessel.orElse(null));
		return Effects.Result.DONE;
	}
}
