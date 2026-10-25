package com.ajustor.fmab.stone;

import com.ajustor.fmab.network.CinematicPayload;
import com.ajustor.fmab.platform.event.ServerEvents;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Le rite de la Pierre, comme à Xerxès : le sacrifice ne foudroie pas d'un coup, il se joue en cinq
 * secondes. La lumière rouge court sur le cercle, du bord vers le centre, et fige tout ce qui vit ; puis
 * les vies tombent, des plus lointaines aux plus proches, et leurs âmes filent vers le centre en
 * traînées de lumière ; elles s'y condensent en une colonne rouge, puis en Pierres, ou dans le corps de
 * celui qui se tient au centre. Ceux qui sont pris dans le cercle le voient à l'écran
 * ({@link CinematicPayload#STONE_RITE}, et {@link CinematicPayload#STONE_VESSEL} pour celui qui devient la Pierre).
 */
public final class StoneRite {
	/** Le cercle s'éveille : la lumière court du bord au centre. */
	public static final int AWAKENING = 30;
	/** Les vies tombent jusque-là. */
	public static final int HARVEST_END = 80;
	/** Les âmes se condensent : la colonne de lumière, puis la Pierre. */
	public static final int LENGTH = 100;
	/** Ceux qui voient la cinématique : dans le cercle, ou à quelques pas de son bord. */
	private static final double WITNESS = 16;
	private static final int HEARTBEAT = 15;
	private static final int PILLAR = 24;
	private static final DustParticleOptions RED = new DustParticleOptions(0xD01020, 1.2f);
	private static final DustParticleOptions GLOW = new DustParticleOptions(0xFF3040, 2.0f);

	private static final class Rite {
		final ServerLevel level;
		final UUID caster;
		final BlockPos circle;
		final Vec3 center;
		final double radius;
		/** Des plus lointaines aux plus proches : l'ordre dans lequel elles tombent. */
		final List<LivingEntity> victims;
		final int souls;
		/** Celui qui devient la Pierre, ou null : les âmes se condensent en Pierres. */
		final UUID vessel;
		int age;

		Rite(ServerLevel level, UUID caster, BlockPos circle, double radius, List<LivingEntity> victims, int souls,
				UUID vessel) {
			this.level = level;
			this.caster = caster;
			this.circle = circle;
			this.center = Vec3.atBottomCenterOf(circle).add(0, 0.1, 0);
			this.radius = radius;
			this.victims = victims;
			this.souls = souls;
			this.vessel = vessel;
		}
	}

	private static final List<Rite> ACTIVE = new ArrayList<>();

	private StoneRite() {
	}

	/** Le tick où tombe la victime de ce rang (0 : la plus lointaine) : de la fin de l'éveil à la fin de la moisson. */
	public static int deathTick(int rank, int count) {
		if (count <= 1) {
			return AWAKENING;
		}
		return AWAKENING + rank * (HARVEST_END - AWAKENING - 1) / (count - 1);
	}

	/** Jusqu'où la lumière rouge a gagné, depuis le bord : le rayon de son front à ce tick. */
	public static double front(int age, double radius) {
		return radius * (1 - Mth.clamp((double) age / AWAKENING, 0, 1));
	}

	public static void register() {
		// Un monde fermé (en solo) ne garde pas ses rites pour le suivant.
		ServerEvents.STOPPED.register(server -> ACTIVE.clear());
		ServerEvents.END_TICK.register(server -> {
			for (Iterator<Rite> it = ACTIVE.iterator(); it.hasNext(); ) {
				Rite rite = it.next();
				if (tick(rite)) {
					it.remove();
				}
			}
		});
	}

	/**
	 * Le cercle s'éveille : les victimes sont figées, ceux qui sont pris dedans voient la cinématique, et le
	 * rite suit son cours ({@link #LENGTH} ticks).
	 *
	 * @param vessel celui qui deviendra la Pierre, ou null pour condenser les âmes en Pierres
	 */
	public static void start(ServerLevel level, ServerPlayer caster, BlockPos circle, double radius,
			List<LivingEntity> victims, int souls, ServerPlayer vessel) {
		Vec3 center = Vec3.atCenterOf(circle);
		List<LivingEntity> order = new ArrayList<>(victims);
		order.sort(Comparator.comparingDouble((LivingEntity e) -> e.distanceToSqr(center)).reversed());
		for (LivingEntity victim : order) {
			victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, LENGTH, 9, false, false));
			if (victim instanceof Mob mob) {
				mob.getNavigation().stop();
			}
		}
		ACTIVE.add(new Rite(level, caster.getUUID(), circle.immutable(), radius, order, souls,
				vessel == null ? null : vessel.getUUID()));
		AABB seen = new AABB(circle).inflate(radius + WITNESS);
		for (ServerPlayer p : level.getPlayers(p -> seen.contains(p.position()))) {
			CinematicPayload.play(p, p == vessel ? CinematicPayload.STONE_VESSEL : CinematicPayload.STONE_RITE, LENGTH);
		}
		level.playSound(null, circle, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, loud(radius), 0.5f);
	}

	/** Un tick du rite ; vrai quand il est fini. */
	private static boolean tick(Rite rite) {
		int age = ++rite.age;
		ServerLevel level = rite.level;
		RandomSource random = level.getRandom();
		if (age < HARVEST_END && age % HEARTBEAT == 1) {
			level.playSound(null, rite.circle, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, loud(rite.radius), 0.6f);
		}
		double lit = front(age, rite.radius);
		if (age <= AWAKENING && age % 2 == 0) {
			ring(level, rite.center, lit);
		}
		// Le cercle crépite d'éclairs rouges, là où la lumière a déjà passé.
		if (age < HARVEST_END) {
			for (int i = 0, n = 1 + (int) (rite.radius / 8); i < n; i++) {
				double r = lit + random.nextDouble() * (rite.radius - lit);
				double angle = random.nextDouble() * Math.PI * 2;
				TransmutationLightning.arc(level, rite.center.add(Math.cos(angle) * r, 0, Math.sin(angle) * r), true);
			}
		}
		// Les vies tombent, et leurs âmes filent vers le centre.
		int count = rite.victims.size();
		for (int i = 0; i < count; i++) {
			LivingEntity victim = rite.victims.get(i);
			if (!victim.isAlive() || victim.isRemoved()) {
				continue;
			}
			if (deathTick(i, count) == age) {
				reap(level, victim, rite.center);
			} else if (age % 4 == 0 && front(age, rite.radius) <= Math.sqrt(victim.distanceToSqr(rite.center))) {
				// Pris par la lumière, il ne bouge plus : une lueur rouge monte de lui.
				level.sendParticles(RED, true, false, victim.getX(), victim.getY(0.3), victim.getZ(), 4, 0.25, 0.4, 0.25, 0);
			}
		}
		if (age >= AWAKENING && age < HARVEST_END) {
			vortex(level, rite.center, age);
		}
		if (age == HARVEST_END) {
			level.playSound(null, rite.circle, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, loud(rite.radius), 0.5f);
		}
		if (age >= HARVEST_END) {
			for (double y = 0; y < PILLAR; y += 0.5) {
				level.sendParticles(GLOW, true, false, rite.center.x, rite.center.y + y, rite.center.z, 1, 0.15, 0.1, 0.15, 0);
			}
		}
		if (age < LENGTH) {
			return false;
		}
		finish(rite);
		return true;
	}

	/** Le front de la lumière : un anneau rouge sur le sol, qui se resserre vers le centre. */
	private static void ring(ServerLevel level, Vec3 center, double r) {
		int n = Mth.clamp((int) (r * Math.PI * 2 * 1.2), 12, 400);
		for (int i = 0; i < n; i++) {
			double angle = Math.PI * 2 * i / n;
			level.sendParticles(GLOW, true, false, center.x + Math.cos(angle) * r, center.y + 0.1,
					center.z + Math.sin(angle) * r, 1, 0, 0.05, 0, 0);
		}
	}

	/** Les âmes qui affluent tourbillonnent au-dessus du centre, de plus en plus serrées. */
	private static void vortex(ServerLevel level, Vec3 center, int age) {
		float p = (float) (age - AWAKENING) / (HARVEST_END - AWAKENING);
		double r = Mth.lerp(p, 3.0, 0.6);
		for (int arm = 0; arm < 3; arm++) {
			double angle = age * 0.35 + arm * Math.PI * 2 / 3;
			level.sendParticles(arm == 0 ? ParticleTypes.SOUL : RED, true, false, center.x + Math.cos(angle) * r,
					center.y + 1 + p * 1.5, center.z + Math.sin(angle) * r, 1, 0, 0, 0, 0);
		}
	}

	/** Une vie tombe : son âme lui est arrachée et file vers le centre en arc de lumière. */
	private static void reap(ServerLevel level, LivingEntity victim, Vec3 center) {
		Vec3 from = victim.position().add(0, victim.getBbHeight() * 0.7, 0);
		Vec3 to = center.add(0, 1.5, 0);
		double dist = from.distanceTo(to);
		int steps = Mth.clamp((int) (dist / 0.6), 4, 120);
		for (int i = 0; i <= steps; i++) {
			double t = (double) i / steps;
			Vec3 p = from.lerp(to, t).add(0, Math.sin(t * Math.PI) * Math.min(6, dist * 0.25), 0);
			level.sendParticles(i % 3 == 0 ? ParticleTypes.SOUL : RED, true, false, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
		}
		level.sendParticles(RED, true, false, victim.getX(), victim.getY(0.5), victim.getZ(), 30, 0.3, 0.6, 0.3, 0);
		if (victim instanceof AbstractVillager) {
			level.playSound(null, victim.blockPosition(), SoundEvents.VILLAGER_DEATH, SoundSource.NEUTRAL, 1, 0.6f);
		} else {
			level.playSound(null, victim.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.NEUTRAL, 1.5f, 0.8f);
		}
		victim.kill(level);
	}

	/** Les âmes sont condensées : dans celui qui se tient au centre, sinon en Pierres. */
	private static void finish(Rite rite) {
		ServerLevel level = rite.level;
		ServerPlayer vessel = rite.vessel == null ? null : level.getServer().getPlayerList().getPlayer(rite.vessel);
		ServerPlayer caster = level.getServer().getPlayerList().getPlayer(rite.caster);
		if (vessel != null && vessel.isAlive() && vessel.level() == level) {
			LivingStone.become(vessel, rite.souls, rite.circle);
		} else {
			// Celui qui devait les recevoir n'est plus là : les âmes se condensent en Pierres.
			Vec3 at = Vec3.atCenterOf(rite.circle);
			for (int filled : Sacrifice.stones(rite.souls)) {
				ItemStack stone = new ItemStack(FmabItems.PHILOSOPHER_STONE);
				stone.setDamageValue(stone.getMaxDamage() - filled);
				level.addFreshEntity(new ItemEntity(level, at.x, at.y, at.z, stone));
			}
			if (caster != null) {
				caster.sendSystemMessage(Component.translatable("stone.fmab.sacrifice_done", rite.souls));
			}
		}
		TransmutationLightning.discharge(level, rite.circle, 3, 1.2, true);
		level.sendParticles(RED, true, false, rite.center.x, rite.center.y + 0.5, rite.center.z, 200, 2, 0.5, 2, 0);
		level.playSound(null, rite.circle, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.8f, 1.6f);
	}

	/** Un son qui porte jusqu'au bord du cercle (un volume de 1 porte à seize blocs). */
	private static float loud(double radius) {
		return (float) Math.max(1, radius / 16 + 1);
	}
}
