package com.ajustor.fmab.stone;

import com.ajustor.fmab.alchemy.ForbiddenCircles;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.network.FmabNetwork;
import com.ajustor.fmab.network.StoneRadiusPayload;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.EffectContext;
import com.ajustor.fmab.transmutation.Effects;
import com.ajustor.fmab.util.PlayerMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Le cercle de la Pierre philosophale ({@link ForbiddenCircles#PHILOSOPHER_STONE}). Avec du sang
 * cristallisé posé dessus, ou quelqu'un debout en son centre pour recevoir les âmes, il s'éveille et
 * demande à l'alchimiste jusqu'où il doit porter : quelques blocs, ou toute une ferme. Le sacrifice a
 * lieu à la réponse (voir {@link Sacrifice}).
 */
public final class StoneCircle {
	/** Le plus grand rayon qu'on puisse demander : une ferme entière. */
	public static final int MAX_RADIUS = 64;
	/** Le cercle n'attend pas plus d'une minute. */
	private static final int PATIENCE = 20 * 60;
	/** Il faut rester près de son cercle pour répondre. */
	private static final double REACH = 8;

	/** @param inscribed le cercle est tracé : il doit être encore là au moment de la réponse */
	private record Pending(ResourceKey<Level> dimension, BlockPos circle, long since, boolean inscribed) {
	}

	private static final Map<UUID, Pending> PENDING = PlayerMaps.track(new HashMap<>());

	private StoneCircle() {
	}

	public static void register() {
		Effects.register(ForbiddenCircles.PHILOSOPHER_STONE_EFFECT, StoneCircle::apply);
	}

	/** Le rayon qu'on obtient vraiment de celui qu'on demande : d'un bloc à {@link #MAX_RADIUS}. */
	public static int radius(int asked) {
		return Math.clamp(asked, 1, MAX_RADIUS);
	}

	private static Effects.Result apply(EffectContext ctx) {
		ServerPlayer caster = ctx.caster();
		if (caster == null) {
			return Effects.Result.NO_TARGET;
		}
		// Le sang cristallisé, ou le sang de celui qui se tient au centre et recevra les âmes.
		boolean blood = !ctx.level().getEntitiesOfClass(ItemEntity.class, ctx.onCircle(),
				e -> e.getItem().is(FmabItems.CRYSTALLIZED_BLOOD)).isEmpty();
		if (!blood && Sacrifice.vessel(ctx.level(), ctx.circle()).isEmpty()) {
			caster.sendOverlayMessage(Component.translatable("stone.fmab.circle_needs_blood"));
			return Effects.Result.NO_MATERIAL;
		}
		ServerLevel level = ctx.level();
		PENDING.put(caster.getUUID(), new Pending(level.dimension(), ctx.circle().immutable(), level.getGameTime(),
				level.getBlockEntity(ctx.circle()) instanceof TransmutationCircleBlockEntity));
		FmabNetwork.send(caster, new StoneRadiusPayload(radius(ctx.range()), MAX_RADIUS));
		return Effects.Result.DONE;
	}

	/**
	 * Le rayon choisi : le sacrifice a lieu. Zéro (l'écran fermé), ou une réponse trop tardive, trop
	 * loin du cercle, laisse le cercle s'éteindre.
	 */
	public static Effects.Result answer(ServerPlayer player, int asked) {
		Pending pending = PENDING.remove(player.getUUID());
		ServerLevel level = player.level();
		if (pending == null || !player.isAlive() || level.dimension() != pending.dimension()
				|| pending.inscribed() && !(level.getBlockEntity(pending.circle()) instanceof TransmutationCircleBlockEntity)
				|| level.getGameTime() - pending.since() > PATIENCE
				|| player.position().distanceTo(Vec3.atCenterOf(pending.circle())) > REACH) {
			if (asked > 0) {
				player.sendOverlayMessage(Component.translatable("gate.fmab.choice.too_late"));
			}
			return Effects.Result.NO_TARGET;
		}
		if (asked <= 0) {
			player.sendOverlayMessage(Component.translatable("stone.fmab.circle_declined"));
			return Effects.Result.NO_TARGET;
		}
		int r = radius(asked);
		AABB area = new AABB(pending.circle()).inflate(r, r, r);
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pending.circle()).inflate(1, 0.5, 1),
				ItemEntity::isAlive);
		if (items.stream().noneMatch(e -> e.getItem().is(FmabItems.CRYSTALLIZED_BLOOD))
				&& Sacrifice.vessel(level, pending.circle()).isEmpty()) {
			player.sendOverlayMessage(Component.translatable("stone.fmab.circle_needs_blood"));
			return Effects.Result.NO_MATERIAL;
		}
		return Sacrifice.perform(level, player, pending.circle(), area, items);
	}
}
