package com.ajustor.fmab.stone;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.exchange.Composition;
import com.ajustor.fmab.item.FirearmItem;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.registry.FmabSounds;
import com.ajustor.fmab.transmutation.ItemCompositions;
import com.ajustor.fmab.util.PlayerMaps;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Ce qu'on lance sur une Pierre vivante se défait avant de la toucher : flèches, tridents, boules de
 * feu, potions, balles de fusil. Pas de blessure, pas d'explosion ; la matière du projectile passe
 * dans ses poches, à l'échange équivalent et au prorata (quatre flèches rendent un silex, un bâton
 * et une plume). Un objet sans composition (un trident, une potion, un objet du mod) lui revient tel
 * quel ; une flèche d'Infinité, qui n'a rien coûté, ne rend rien, pas plus qu'un projectile sans
 * objet (crachat de lama, balle de shulker, crâne de Wither). Ses propres projectiles, eux, ne se
 * défont jamais.
 *
 * <p>À chaque tick, on guette ce qui file vers elle ; ce qui la touche avant d'avoir été vu (un tir à
 * bout portant) se défait au moment du choc, que le jeu prend pour une déviation
 * ({@code EntityMixin}).
 */
public final class ProjectileTransmutation {
	/** Le rayon où l'on guette les projectiles. */
	private static final double WATCH = 8;
	/** Combien de ticks à l'avance on prolonge leur trajectoire. */
	private static final double LOOKAHEAD = 4;
	/** Ce qui passe à moins d'un demi-bloc du corps le vise. */
	private static final double MARGIN = 0.5;
	/** Sous cette vitesse (en blocs par tick), un projectile est au repos : planté, ou posé. */
	private static final double AT_REST = 0.01;
	/** Le projectile s'est défait au contact : il ne touche pas, et ne rebondit pas. */
	public static final ProjectileDeflection TRANSMUTED = (projectile, by, random) -> {
	};

	private static final Map<UUID, MassBalance> BALANCES = PlayerMaps.track(new HashMap<>());

	private ProjectileTransmutation() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isAlive() && !player.isSpectator() && LivingStone.is(player)) {
					watch(player);
				}
			}
		});
		// Les balles ne sont pas des projectiles. Ce qui aurait échappé au guet (l'explosion d'une boule
		// de feu sur le mur d'à côté, l'éclaboussure d'une potion) ne la blesse pas davantage, mais ne
		// lui rend rien : sa matière est partie dans l'explosion.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer stone) || source.getEntity() == stone || !LivingStone.is(stone)) {
				return true;
			}
			if (source.is(FirearmItem.BULLET)) {
				bullet(stone, source);
				return false;
			}
			return !(source.getDirectEntity() instanceof Projectile);
		});
	}

	/**
	 * Un projectile touche cette créature ({@link Entity#deflection}). Si c'est une Pierre vivante et
	 * qu'il n'est pas à elle, il se défait sur-le-champ.
	 *
	 * @return vrai s'il s'est défait : le jeu ne doit pas l'appliquer
	 */
	public static boolean intercept(Entity target, Projectile projectile) {
		if (!(target instanceof ServerPlayer stone) || stone.isSpectator() || projectile.isRemoved()
				|| !LivingStone.is(stone) || !foreign(projectile, stone)) {
			return false;
		}
		transmute(stone, projectile);
		return true;
	}

	/** Ce qui file vers la Pierre se défait avant de l'atteindre. */
	private static void watch(ServerPlayer stone) {
		AABB body = stone.getBoundingBox();
		for (Projectile projectile : stone.level().getEntitiesOfClass(Projectile.class, body.inflate(WATCH),
				p -> foreign(p, stone) && onCourse(p.getBoundingBox().getCenter(), p.getDeltaMovement(),
						body.inflate(MARGIN + p.getBbWidth() / 2), LOOKAHEAD) && clear(p, stone))) {
			transmute(stone, projectile);
		}
	}

	/** Rien ne s'interpose : ce qui va se planter dans un mur ne l'atteindra pas. */
	private static boolean clear(Projectile projectile, ServerPlayer stone) {
		Vec3 from = projectile.getBoundingBox().getCenter();
		return stone.level().clip(new ClipContext(from, stone.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, projectile)).getType() == HitResult.Type.MISS;
	}

	/** Un projectile d'un autre, ou de personne. La fusée qui pousse un planeur ne vise personne. */
	private static boolean foreign(Projectile projectile, ServerPlayer stone) {
		Entity owner = projectile.getOwner();
		return owner != stone && !(projectile instanceof FireworkRocketEntity
				&& owner instanceof LivingEntity flyer && flyer.isFallFlying());
	}

	/**
	 * La trajectoire d'un projectile, prolongée en ligne droite sur quelques ticks, traverse-t-elle
	 * cette boîte (le corps, élargi d'une marge) ? Un projectile au repos, ou qui s'éloigne de son
	 * centre, ne vise rien.
	 */
	static boolean onCourse(Vec3 position, Vec3 velocity, AABB target, double ticks) {
		if (velocity.lengthSqr() < AT_REST * AT_REST || velocity.dot(target.getCenter().subtract(position)) <= 0) {
			return false;
		}
		double[] window = {0, ticks};
		return within(window, position.x, velocity.x, target.minX, target.maxX)
				&& within(window, position.y, velocity.y, target.minY, target.maxY)
				&& within(window, position.z, velocity.z, target.minZ, target.maxZ);
	}

	/** Resserre la fenêtre aux instants où, sur cet axe, la trajectoire est entre ces bornes ; vide, elle manque. */
	private static boolean within(double[] window, double from, double speed, double min, double max) {
		if (Math.abs(speed) < 1e-9) {
			return from >= min && from <= max;
		}
		double a = (min - from) / speed;
		double b = (max - from) / speed;
		window[0] = Math.max(window[0], Math.min(a, b));
		window[1] = Math.min(window[1], Math.max(a, b));
		return window[0] <= window[1];
	}

	/** Le projectile disparaît dans un éclair ; sa matière passe à la Pierre. */
	private static void transmute(ServerPlayer stone, Projectile projectile) {
		ItemStack item = itemOf(projectile);
		spark(stone.level(), projectile.getBoundingBox().getCenter());
		projectile.discard();
		if (!item.isEmpty()) {
			refund(stone, item, composition(stone.level(), item));
		}
	}

	/** L'objet dont le projectile est fait : rien pour une flèche d'Infinité, qui n'a rien coûté. */
	private static ItemStack itemOf(Projectile projectile) {
		if (projectile instanceof AbstractArrow arrow) {
			return arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY ? ItemStack.EMPTY
					: arrow.getPickupItemStackOrigin().copy();
		}
		return projectile instanceof ItemSupplier supplier ? supplier.getItem().copy() : ItemStack.EMPTY;
	}

	/**
	 * Une balle de fusil n'est pas un projectile, mais un tir en ligne droite ({@link FirearmItem}) : elle
	 * se défait au contact, et la cartouche brûlée rend sa matière. Un tireur en créatif n'en a brûlé
	 * aucune.
	 */
	private static void bullet(ServerPlayer stone, DamageSource source) {
		spark(stone.level(), stone.getBoundingBox().getCenter());
		if (!(source.getEntity() instanceof Player shooter && shooter.isCreative())) {
			refund(stone, new ItemStack(FmabItems.CARTRIDGE), ItemCompositions.of(stone.level(), FmabItems.CARTRIDGE));
		}
	}

	/** Les objets du mod ont leurs propres façons d'être faits : comme le cercle, on ne les défait pas. */
	private static Optional<Composition> composition(ServerLevel level, ItemStack stack) {
		if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(Fmab.MOD_ID)) {
			return Optional.empty();
		}
		return ItemCompositions.of(level, stack.getItem());
	}

	/**
	 * La matière de ces objets passe à la Pierre : chaque élément s'ajoute à ce qu'elle a déjà reçu, et
	 * ce qui fait des pièces entières lui est rendu. Un objet sans composition lui revient tel quel.
	 */
	private static void refund(ServerPlayer stone, ItemStack stack, Optional<Composition> composition) {
		if (composition.isEmpty()) {
			stone.getInventory().placeItemBackInInventory(stack);
			return;
		}
		ServerLevel level = stone.level();
		MassBalance balance = BALANCES.computeIfAbsent(stone.getUUID(), id -> new MassBalance());
		balance.add(composition.get().masses(), stack.getCount());
		balance.takeWhole().forEach((element, mass) -> {
			int given = 0;
			for (ItemStack unit : ItemCompositions.units(level, element, mass)) {
				given += unit.getCount() * (int) Math.round(ItemCompositions.of(level, unit.getItem())
						.map(Composition::total).orElse(0.0));
				stone.getInventory().placeItemBackInInventory(unit);
			}
			// Ce qui ne fait pas une pièce (une masse de bois, quand la planche en pèse deux) attend la suite.
			balance.add(element, mass - given);
		});
	}

	private static void spark(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 8, 0.15, 0.15, 0.15, 0.05);
		level.playSound(null, at.x, at.y, at.z, FmabSounds.TRANSMUTE, SoundSource.PLAYERS, 0.3f, 1.6f);
	}
}
