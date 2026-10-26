package com.ajustor.fmab.stone;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.TransmutationPose;
import com.ajustor.fmab.entity.EnvyEntity;
import com.ajustor.fmab.entity.HomunculusEntity;
import com.ajustor.fmab.entity.LustEntity;
import com.ajustor.fmab.entity.SoulEyeEntity;
import com.ajustor.fmab.entity.TruthEntity;
import com.ajustor.fmab.gate.GateOfTruth;
import com.ajustor.fmab.item.PhilosopherStoneItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import com.ajustor.fmab.util.PlayerMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.ajustor.fmab.platform.event.InteractionEvents;
import com.ajustor.fmab.platform.event.PlayerEvents;
import com.ajustor.fmab.platform.event.ServerEvents;
import com.ajustor.fmab.compat.EntityCompat;

/**
 * La Pierre philosophale vivante, comme Van Hohenheim : l'alchimiste a fondu dans son corps les âmes
 * de deux Pierres pleines, pendant une éclipse, sur un cercle de transmutation humaine. Il ne paie
 * plus de péage à la Porte, ses transmutations ne lui coûtent rien, et il ressuscite sur place à
 * chaque mort ; ses âmes le nourrissent, il n'a plus besoin de manger, et ses plaies se referment
 * d'elles-mêmes : en théorie, il est immortel. Mais chaque grande transmutation et chaque mort
 * consument des âmes ; à zéro, il meurt pour de bon et redevient un homme. Et les homonculus le traquent.
 *
 * <p>Ses âmes n'ont pas de limite : main nue, il absorbe celles des vivants en gardant la main sur
 * eux, de plus en plus vite. Une autre Pierre vivante (ou un homonculus) perd ses âmes une à une ;
 * tout autre être se vide, et sa mort lui rapporte ses âmes.
 */
public final class LivingStone {
	/** Ce que vaut la mort d'un être absorbé : un humain, une bête. */
	private static final int HUMAN_SOULS = 12;
	private static final int BEAST_SOULS = 3;
	/** Le jeu répète le geste toutes les quatre ticks tant qu'on maintient le clic. */
	private static final int ABSORB_GAP = 8;
	/** Une absorption soutenue s'accélère jusqu'à ce rythme, en âmes par pulsation. */
	private static final int MAX_RATE = 12;

	/** Un absorbeur : sa victime et depuis combien de pulsations il la tient. */
	private record Grip(UUID target, long last, int streak) {
	}

	private static final Map<UUID, Grip> GRIPS = PlayerMaps.track(new HashMap<>());
	/** Une mort coûte cher ; une grande transmutation (deux étages ou plus), une âme. */
	private static final int DEATH = 20;
	private static final DustParticleOptions RED = new DustParticleOptions(0xD01020, 1.2f);
	/** Toutes les dix minutes, une chance sur trois qu'un homonculus vienne le chercher. */
	private static final int HUNT_PERIOD = 20 * 60 * 10;
	/** Une barre de faim pleine. */
	private static final int FULL = 20;
	/** Ce que ses âmes lui rendent de vie chaque seconde : un cœur. */
	private static final float REGENERATION = 2;

	private LivingStone() {
	}

	public static int souls(ServerPlayer player) {
		Integer souls = FmabAttachments.LIVING_STONE.get(player);
		return souls == null ? 0 : souls;
	}

	/** Le joueur est-il lui-même une Pierre philosophale, comme Hohenheim ? */
	public static boolean is(ServerPlayer player) {
		return souls(player) > 0;
	}

	/**
	 * A-t-il des âmes à dépenser ? La dernière est la sienne : réduite à elle, la Pierre vivante
	 * n'amplifie plus rien, et il paie de sa concentration comme tout le monde.
	 */
	public static boolean fuels(ServerPlayer player) {
		return souls(player) > 1;
	}

	/**
	 * Le rituel : sur un cercle de transmutation humaine, pendant l'éclipse, un Initié de la Porte qui
	 * tient une Pierre pleine dans chaque main devient une Pierre vivante.
	 *
	 * @return vrai si le rituel a eu lieu (la transmutation humaine n'a pas lieu)
	 */
	public static boolean tryRitual(ServerPlayer player, BlockPos circle) {
		ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
		ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
		if (!main.is(FmabItems.PHILOSOPHER_STONE) || !off.is(FmabItems.PHILOSOPHER_STONE) || souls(player) > 0) {
			return false;
		}
		if (!full(main) || !full(off)) {
			// Deux Pierres en main sur ce cercle : c'est un rituel, raté, pas une transmutation humaine.
			player.sendSystemMessage(Component.translatable("stone.fmab.ritual_needs_full",
					PhilosopherStoneItem.souls(main), PhilosopherStoneItem.souls(off), main.getMaxDamage()));
			return true;
		}
		if (!Eclipse.now(player.level())) {
			player.sendSystemMessage(Component.translatable("stone.fmab.ritual_needs_eclipse"));
			return true;
		}
		if (!FmabAttachments.ALCHEMIST.getOrCreate(player).rank()
				.atLeast(Rank.GATE)) {
			player.sendSystemMessage(Component.translatable("stone.fmab.ritual_needs_gate"));
			return true;
		}
		// Les âmes des deux Pierres, sans limite : elles ne feront que croître.
		int souls = PhilosopherStoneItem.souls(main) + PhilosopherStoneItem.souls(off);
		if (!player.isCreative()) {
			main.shrink(1);
			off.shrink(1);
		}
		become(player, souls, circle);
		return true;
	}

	/**
	 * Les âmes se fondent dans le corps du joueur : il devient une Pierre vivante, ou grossit celle qu'il
	 * est déjà. Ses membres pris par la Porte lui reviennent ; son karma ne bouge pas. Une Pierre
	 * vivante transmute sans cercle, mains jointes, même sans avoir vu sa Porte.
	 */
	public static void become(ServerPlayer player, int souls, BlockPos circle) {
		boolean already = souls(player) > 0;
		FmabAttachments.LIVING_STONE.set(player, souls(player) + souls);
		GateState gate = FmabAttachments.GATE.getOrCreate(player);
		FmabAttachments.GATE.set(player, gate.restore(gate.lost()));
		GateOfTruth.returnParts(player);
		player.setHealth(player.getMaxHealth());
		ServerLevel level = player.level();
		level.sendParticles(RED, player.getX(), player.getY(0.5), player.getZ(), 300, 1.5, 1.5, 1.5, 0);
		TransmutationLightning.discharge(level, circle, 3, 1.5, true);
		level.playSound(null, circle, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5f, 0.6f);
		player.sendSystemMessage(Component.translatable(already ? "stone.fmab.living_stone_grew" : "stone.fmab.became_living_stone",
				souls(player)));
	}

	private static boolean full(ItemStack stack) {
		return stack.is(FmabItems.PHILOSOPHER_STONE) && PhilosopherStoneItem.souls(stack) >= stack.getMaxDamage();
	}

	/** La faim d'une Pierre vivante : ses âmes la nourrissent, elle reste rassasiée. Les autres gardent la leur. */
	public static int fed(int souls, int food) {
		return souls > 0 ? FULL : food;
	}

	/** La vie que ses âmes rendent chaque seconde à une Pierre vivante, tant qu'il lui en reste ; rien aux autres. */
	public static float regeneration(int souls) {
		return souls > 0 ? REGENERATION : 0;
	}

	/** Chaque usage de la Pierre vivante consume des âmes (jamais la dernière, la sienne). */
	public static void spend(ServerPlayer player, int souls) {
		int left = souls(player);
		if (left <= 0 || player.isCreative()) {
			return;
		}
		FmabAttachments.LIVING_STONE.set(player, Math.max(1, left - souls));
	}

	public static void register() {
		InteractionEvents.USE_ENTITY.register((player, level, hand, entity, hit) -> {
			if (!(player instanceof ServerPlayer absorber) || hand != InteractionHand.MAIN_HAND
					|| !player.getMainHandItem().isEmpty() || souls(absorber) <= 0 || !player.isShiftKeyDown()
					|| !(entity instanceof LivingEntity target) || entity instanceof TruthEntity) {
				return InteractionResult.PASS;
			}
			absorb(absorber, target);
			return InteractionResult.SUCCESS;
		});
		// Il ressuscite sur place tant qu'il lui reste des âmes.
		PlayerEvents.ALLOW_DEATH.register((player, source, amount) -> {
			int left = souls(player);
			if (left <= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && player.getY() < -128) {
				return true;
			}
			int after = left - DEATH;
			if (after <= 0) {
				FmabAttachments.LIVING_STONE.remove(player);
				player.sendSystemMessage(Component.translatable("stone.fmab.living_stone_spent"));
				return true;
			}
			FmabAttachments.LIVING_STONE.set(player, after);
			player.setHealth(player.getMaxHealth());
			player.level().sendParticles(RED, player.getX(), player.getY(0.5), player.getZ(), 80, 0.5, 1, 0.5, 0);
			player.level().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS,
					1, 0.8f);
			player.sendOverlayMessage(Component.translatable("stone.fmab.resurrected", after));
			return false;
		});
		ServerEvents.END_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				player.getFoodData().setFoodLevel(fed(souls(player), player.getFoodData().getFoodLevel()));
				float heal = regeneration(souls(player));
				if (heal > 0 && player.isAlive() && player.getHealth() < player.getMaxHealth()) {
					player.heal(heal);
				}
			}
		});
		ServerEvents.END_TICK.register(server -> {
			if (server.getTickCount() % HUNT_PERIOD != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.level().getRandom().nextInt(3) == 0) {
					visit(player);
				}
			}
		});
	}

	/**
	 * Une pulsation d'absorption. Plus on tient la même victime, plus ça va vite.
	 */
	private static void absorb(ServerPlayer absorber, LivingEntity target) {
		ServerLevel level = absorber.level();
		long now = level.getGameTime();
		Grip grip = GRIPS.get(absorber.getUUID());
		int streak = grip != null && grip.target().equals(target.getUUID()) && now - grip.last() <= ABSORB_GAP
				? grip.streak() + 1 : 0;
		GRIPS.put(absorber.getUUID(), new Grip(target.getUUID(), now, streak));
		int rate = Math.min(MAX_RATE, 1 + streak / 3);
		EntityCompat.swing(absorber, InteractionHand.MAIN_HAND, true);
		TransmutationPose.strike(absorber, TransmutationPose.Kind.REACH, ABSORB_GAP + 2);
		draw(level, target, absorber);
		// Un œil s'ouvre dans le sol sous la proie, tant qu'on la tient.
		SoulEyeEntity.under(level, target, ABSORB_GAP + 2);

		int gained;
		if (target instanceof ServerPlayer victim && souls(victim) > 0) {
			// Une autre Pierre vivante : ses âmes passent une à une, de plus en plus vite.
			int left = souls(victim);
			gained = Math.min(rate, left);
			if (left - gained <= 0) {
				FmabAttachments.LIVING_STONE.remove(victim);
				victim.sendSystemMessage(Component.translatable("stone.fmab.drained_dry"));
				victim.kill(level);
			} else {
				FmabAttachments.LIVING_STONE.set(victim, left - gained);
				victim.sendOverlayMessage(Component.translatable("stone.fmab.being_drained", left - gained));
			}
		} else if (target instanceof HomunculusEntity homunculus && homunculus.souls() > 0) {
			// La Pierre d'un homonculus se vide dans la sienne.
			gained = homunculus.takeSouls(rate);
		} else {
			// Tout autre être se vide de sa vie ; à sa mort, ses âmes passent dans la Pierre.
			target.hurtServer(level, level.damageSources().magic(), 2 * rate);
			gained = target.isAlive() ? 0
					: target instanceof Player || target instanceof AbstractVillager ? HUMAN_SOULS : BEAST_SOULS;
		}
		if (gained > 0) {
			FmabAttachments.LIVING_STONE.set(absorber, souls(absorber) + gained);
			absorber.sendOverlayMessage(Component.translatable("stone.fmab.absorbed", gained, souls(absorber)));
			if (target instanceof Player || target instanceof AbstractVillager) {
				Karma.add(absorber, -gained);
			}
		}
	}

	/** Un filet rouge des âmes, de la victime à l'absorbeur. */
	private static void draw(ServerLevel level, LivingEntity from, ServerPlayer to) {
		var a = from.position().add(0, from.getBbHeight() * 0.6, 0);
		var b = to.position().add(0, to.getBbHeight() * 0.6, 0);
		for (int i = 0; i <= 12; i++) {
			var p = a.lerp(b, i / 12.0);
			level.sendParticles(RED, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
		}
	}

	/**
	 * Les homonculus viennent chercher une Pierre vivante (et recruter les alchimistes sans
	 * scrupules) : Lust, ou Envy démasqué, non loin du joueur.
	 */
	/**
	 * La visite des homonculus ({@link Recruitment#visit}) : rien pour une recrue, l'offre de Père
	 * portée par Lust pour une âme noire qui n'a pas répondu, sinon un traqueur hostile.
	 */
	public static void visit(ServerPlayer player) {
		Recruitment.Visit visit = Recruitment.visit(Karma.of(player), souls(player),
				FmabAttachments.RECRUITMENT.get(player));
		ServerLevel level = player.level();
		if (visit == Recruitment.Visit.NONE
				|| !level.getEntitiesOfClass(HomunculusEntity.class, player.getBoundingBox().inflate(64)).isEmpty()) {
			return;
		}
		boolean offer = visit == Recruitment.Visit.OFFER;
		double angle = level.getRandom().nextDouble() * Math.PI * 2;
		int reach = offer ? 12 : 28;
		int x = player.getBlockX() + (int) (Math.cos(angle) * reach);
		int z = player.getBlockZ() + (int) (Math.sin(angle) * reach);
		BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
		HomunculusEntity hunter = offer || level.getRandom().nextBoolean()
				? FmabEntities.LUST.create(level, EntitySpawnReason.EVENT)
				: FmabEntities.ENVY.create(level, EntitySpawnReason.EVENT);
		if (hunter == null) {
			return;
		}
		hunter.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		if (hunter instanceof EnvyEntity envy) {
			envy.disguiseAs(player);
		}
		hunter.markWanderer();
		level.addFreshEntity(hunter);
		if (offer && hunter instanceof LustEntity lust) {
			lust.bringOffer(player);
			return;
		}
		hunter.setTarget(player);
		player.sendSystemMessage(Component.translatable(hunter instanceof LustEntity
				? "stone.fmab.hunted_lust" : "stone.fmab.hunted_envy"));
	}
}
