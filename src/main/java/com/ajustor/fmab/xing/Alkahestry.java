package com.ajustor.fmab.xing;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.knowledge.Knowledge;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.entity.HomunculusEntity;
import com.ajustor.fmab.entity.KunaiEntity;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabSounds;
import com.ajustor.fmab.transmutation.AlchemyRules;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * L'alkahestry de Xing. Elle puise au flux du dragon, l'énergie qui court dans la terre, et non à
 * l'énergie tectonique d'Amestris : la zone de Père ne l'arrête pas. Elle agit à distance : cinq
 * kunaï plantés autour d'une zone y dessinent un cercle, qui soigne ce qui s'y trouve ou y piège
 * les ennemis, quelques secondes durant. Il faut l'avoir apprise d'une maîtresse de Xing.
 */
public final class Alkahestry {
	/** L'école de savoir que l'alkahestry fait progresser. */
	public static final String SCHOOL = "medicine";
	private static final int MASTERY = 2;
	public static final ResourceKey<DamageType> DAMAGE =
			ResourceKey.create(Registries.DAMAGE_TYPE, Fmab.id("alkahestry"));
	/** Rayon dans lequel on cherche les kunaï d'un même cercle autour du dernier planté. */
	private static final double GATHER = 10;
	private static final int DURATION = 200;
	private static final int PULSE = 20;
	private static final float COST = 3;
	private static final float HEAL = 3;
	private static final float HARM = 4;
	private static final DustParticleOptions JADE = new DustParticleOptions(0x3CCB8C, 1.0f);
	private static final DustParticleOptions VIOLET = new DustParticleOptions(0x8A3CCB, 1.0f);

	/** Un cercle d'alkahestry en action. */
	private record Circle(ServerLevel level, Vec3 center, double radius, boolean trap, UUID caster, long until) {
	}

	private static final List<Circle> CIRCLES = new ArrayList<>();

	private Alkahestry() {
	}

	public static boolean knows(ServerPlayer player) {
		return Boolean.TRUE.equals(player.getAttached(FmabAttachments.ALKAHESTRY));
	}

	/** Un kunaï vient de se planter : referme-t-il un cercle ? */
	public static void landed(KunaiEntity kunai, ServerPlayer thrower) {
		ServerLevel level = (ServerLevel) kunai.level();
		List<KunaiEntity> points = new ArrayList<>(level.getEntitiesOfClass(KunaiEntity.class,
				kunai.getBoundingBox().inflate(GATHER), k -> k == kunai || k.planted() && k.getOwner() == thrower));
		if (!points.contains(kunai)) {
			points.add(kunai);
		}
		if (points.size() < KunaiRing.POINTS) {
			return;
		}
		// Les plus proches du dernier planté, au plus huit.
		points.sort(Comparator.comparingDouble(k -> k.distanceToSqr(kunai)));
		List<KunaiEntity> ring = new ArrayList<>(points.subList(0, Math.min(8, points.size())));
		Vec3 center = centerOf(ring);
		double radius = enclosure(ring, center);
		if (radius < 0) {
			return;
		}
		if (!knows(thrower)) {
			thrower.sendOverlayMessage(Component.translatable("alkahestry.fmab.unknown"));
			return;
		}
		AlchemistData alchemist = thrower.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		// L'école de la Médecine : moins cher, plus large, plus long à mesure qu'on la pratique.
		Knowledge knowledge = AlchemyRules.of(level.registryAccess()).knowledge(alchemist);
		float cost = (float) Math.max(1, COST - knowledge.perk(SCHOOL, "concentration_discount"));
		if (!thrower.isCreative()) {
			if (alchemist.concentration() < cost) {
				thrower.sendOverlayMessage(Component.translatable("transmutation.fmab.tired", (int) cost,
						(int) alchemist.concentration()));
				return;
			}
			alchemist = alchemist.withConcentration(alchemist.concentration() - cost);
		}
		thrower.setAttached(FmabAttachments.ALCHEMIST, alchemist.addMastery(SCHOOL, MASTERY));
		ring.forEach(KunaiEntity::spend);
		boolean trap = kunai.trap();
		double reach = radius + 0.5 + knowledge.perk(SCHOOL, "range_bonus");
		long duration = DURATION + (long) (knowledge.perk(SCHOOL, "duration_bonus") * 100);
		CIRCLES.add(new Circle(level, center, reach, trap, thrower.getUUID(), level.getGameTime() + duration));
		level.playSound(null, kunai.blockPosition(), FmabSounds.ALKAHESTRY, SoundSource.PLAYERS, 1.5f,
				trap ? 0.6f : 1.4f);
		thrower.sendOverlayMessage(Component.translatable(trap ? "alkahestry.fmab.trap" : "alkahestry.fmab.heal"));
		// Les traits du cercle, d'un kunaï à l'autre, et vers le centre.
		ring.sort(Comparator.comparingDouble(k -> angle(k.position(), center)));
		for (int i = 0; i < ring.size(); i++) {
			line(level, ring.get(i).position(), ring.get((i + 1) % ring.size()).position(), trap);
			line(level, ring.get(i).position(), center, trap);
		}
	}

	private static Vec3 centerOf(List<KunaiEntity> points) {
		Vec3 c = Vec3.ZERO;
		for (KunaiEntity k : points) {
			c = c.add(k.position());
		}
		return c.scale(1.0 / points.size());
	}

	/** Le rayon du cercle que referment les kunaï autour du centre, ou −1. */
	private static double enclosure(List<KunaiEntity> points, Vec3 center) {
		List<double[]> xz = new ArrayList<>();
		for (KunaiEntity k : points) {
			xz.add(new double[]{k.getX(), k.getZ()});
		}
		return KunaiRing.radius(xz, center.x, center.z);
	}

	private static double angle(Vec3 p, Vec3 center) {
		return Math.atan2(p.z - center.z, p.x - center.x);
	}

	private static void line(ServerLevel level, Vec3 a, Vec3 b, boolean trap) {
		Vec3 step = b.subtract(a);
		int n = (int) (step.length() * 3);
		for (int i = 0; i <= n; i++) {
			Vec3 p = a.add(step.scale((double) i / Math.max(1, n)));
			level.sendParticles(trap ? VIOLET : JADE, p.x, p.y + 0.1, p.z, 1, 0, 0, 0, 0);
		}
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Iterator<Circle> it = CIRCLES.iterator(); it.hasNext(); ) {
				Circle c = it.next();
				long now = c.level().getGameTime();
				if (now >= c.until() || c.level().getServer() != server) {
					it.remove();
					continue;
				}
				ring(c, now);
				if (now % PULSE == 0) {
					pulse(c);
				}
			}
		});
	}

	/** Le cercle luit au sol tant qu'il agit. */
	private static void ring(Circle c, long now) {
		if (now % 4 != 0) {
			return;
		}
		int n = (int) (c.radius() * 8);
		for (int i = 0; i < n; i++) {
			double a = 2 * Math.PI * i / n + now * 0.01;
			c.level().sendParticles(c.trap() ? VIOLET : JADE, c.center().x + Math.cos(a) * c.radius(),
					c.center().y + 0.1, c.center().z + Math.sin(a) * c.radius(), 1, 0, 0, 0, 0);
		}
	}

	private static void pulse(Circle c) {
		ServerLevel level = c.level();
		AABB box = new AABB(c.center(), c.center()).inflate(c.radius(), 3, c.radius());
		ServerPlayer caster = level.getServer().getPlayerList().getPlayer(c.caster());
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e.isAlive() && Math.hypot(e.getX() - c.center().x, e.getZ() - c.center().z) <= c.radius())) {
			boolean hostile = e instanceof Enemy;
			if (c.trap()) {
				if (!hostile) {
					continue;
				}
				e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, PULSE + 10, 3));
				DamageSource source = caster == null ? level.damageSources().magic()
						: new DamageSource(level.damageSources().damageTypes.getOrThrow(DAMAGE), caster);
				e.hurtServer(level, source, e instanceof HomunculusEntity ? HARM * 1.5f : HARM);
				level.sendParticles(VIOLET, e.getX(), e.getY(0.5), e.getZ(), 8, 0.3, 0.5, 0.3, 0);
			} else if (!hostile) {
				e.heal(HEAL);
				e.removeEffect(MobEffects.POISON);
				e.removeEffect(MobEffects.WITHER);
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY(0.8), e.getZ(), 4, 0.3, 0.4, 0.3, 0);
			}
		}
	}
}
