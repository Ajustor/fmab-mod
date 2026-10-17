package com.ajustor.fmab.gate;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.Shell;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.util.PlayerMaps;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.ajustor.fmab.platform.event.InteractionEvents;
import com.ajustor.fmab.platform.event.LivingEvents;
import com.ajustor.fmab.platform.event.ServerEvents;
import com.ajustor.fmab.platform.event.PlayerEvents;

/**
 * L'armure d'une âme comme coquille (voir {@link Shell}). L'âme se fige avec sa touche : elle n'est
 * plus qu'une armure, qu'un autre joueur revêt d'un clic main nue. Tant qu'elle reste figée, c'est lui
 * qui la fait marcher, et il en sort en s'accroupissant. Dès qu'elle bouge, elle reprend son corps :
 * celui qui est dedans est emporté, sans rien pouvoir faire, jusqu'à ce qu'elle le relâche.
 *
 * <p>Les deux joueurs occupent la même place : celui qui ne mène pas est figé (vitesse et saut
 * annulés) et ramené chaque tick sur l'autre. Celui qui est dedans ne se voit plus, ne se vise plus,
 * n'agit plus, et les coups qui le visent frappent l'armure.
 */
public final class Shells {
	/** Le verrou de celui qui ne mène pas : plus de vitesse ni de saut à lui. */
	private static final Identifier LOCK = Fmab.id("shell_lock");
	private static final List<Holder<Attribute>> LOCKED = List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH);
	/** Les âmes figées ou habitées, et qui est dedans : de quoi les retrouver sans parcourir le monde. */
	private static final Map<UUID, Pair> SHELLS = PlayerMaps.track(new HashMap<>());

	private static final class Pair {
		final ServerPlayer soul;
		@Nullable ServerPlayer occupant;

		Pair(ServerPlayer soul) {
			this.soul = soul;
		}
	}

	/** Le délai entre deux appuis sur la touche de la coquille : une seconde. */
	public static final int KEY_DELAY = 20;
	public static final com.ajustor.fmab.util.Throttle KEY = new com.ajustor.fmab.util.Throttle(KEY_DELAY);

	private Shells() {
	}

	public static void register() {
		InteractionEvents.USE_ENTITY.register((player, level, hand, entity, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || !(entity instanceof Player soul) || !canEnter(player, soul)) {
				return inside(player) ? InteractionResult.FAIL : InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer occupant && soul instanceof ServerPlayer target) {
				enter(occupant, target);
			}
			return InteractionResult.SUCCESS;
		});
		// Dedans, on ne fait rien : on est porté, ou on porte l'armure qui, elle, ne tient rien.
		InteractionEvents.USE_ITEM.register((player, level, hand) -> busy(player));
		InteractionEvents.USE_BLOCK.register((player, level, hand, hit) -> busy(player));
		InteractionEvents.ATTACK_BLOCK.register((player, level, hand, pos, direction) -> busy(player));
		InteractionEvents.ATTACK_ENTITY.register((player, level, hand, entity, hit) -> busy(player));
		// Les coups qui visent celui qui est dedans frappent l'armure.
		LivingEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
				return true;
			}
			UUID soulId = FmabAttachments.INSIDE.get(player);
			Pair pair = soulId == null ? null : SHELLS.get(soulId);
			if (pair == null || pair.occupant != player) {
				return true;
			}
			if (source.getEntity() != pair.soul) {
				pair.soul.hurtServer(pair.soul.level(), source, amount);
			}
			return false;
		});
		ServerEvents.END_TICK.register(server -> {
			for (Pair pair : new ArrayList<>(SHELLS.values())) {
				tick(pair);
			}
		});
		// Qui part (déconnexion) libère la coquille : l'âme s'éveille, l'occupant ressort.
		PlayerEvents.DISCONNECT.register((player, server) -> {
			for (Pair pair : new ArrayList<>(SHELLS.values())) {
				if (pair.soul == player || pair.occupant == player) {
					release(pair);
				}
			}
		});
	}

	// ---------------------------------------------------------------- règles

	private static Shell shell(Player soul) {
		return FmabAttachments.SHELL.getOrElse(soul, Shell.NONE);
	}

	private static boolean inArmor(Player player) {
		GateState gate = FmabAttachments.GATE.get(player);
		return gate != null && gate.inArmor();
	}

	public static boolean inside(Player player) {
		return FmabAttachments.INSIDE.has(player);
	}

	/** Une âme habitée, ou celui qui l'habite : tous deux à la même place. */
	public static boolean paired(net.minecraft.world.entity.LivingEntity entity) {
		return entity instanceof Player player && (inside(player)
				|| FmabAttachments.SHELL.getOrElse(player, Shell.NONE).occupant().isPresent());
	}

	/** Un joueur de chair, main vide, devant une âme figée et vide. */
	public static boolean canEnter(Player player, Player soul) {
		GateState gate = FmabAttachments.GATE.get(player);
		return player != soul && inArmor(soul) && shell(soul).canEnter() && !inside(player)
				&& (gate == null || !gate.soulBound()) && player.getMainHandItem().isEmpty() && !player.isSpectator();
	}

	private static InteractionResult busy(Player player) {
		if (!inside(player)) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer server) {
			server.sendOverlayMessage(Component.translatable("shell.fmab.busy"));
		}
		return InteractionResult.FAIL;
	}

	/** L'âme a-t-elle voulu bouger : avancer, reculer, aller de côté, sauter. */
	public static boolean moving(Input input) {
		return input.forward() || input.backward() || input.left() || input.right() || input.jump();
	}

	// ---------------------------------------------------------------- actions

	/** La touche de l'âme : se figer, s'éveiller, ou relâcher celui qui est dedans. */
	public static void press(ServerPlayer soul) {
		if (!inArmor(soul)) {
			soul.sendOverlayMessage(Component.translatable("shell.fmab.not_a_soul"));
			return;
		}
		Shell before = shell(soul);
		Pair pair = SHELLS.get(soul.getUUID());
		if (before.occupant().isPresent() && pair != null && pair.occupant != null) {
			ServerPlayer occupant = pair.occupant;
			eject(pair, Shell.NONE);
			soul.sendOverlayMessage(Component.translatable("shell.fmab.released", occupant.getDisplayName()));
			occupant.sendOverlayMessage(Component.translatable("shell.fmab.set_free"));
			return;
		}
		Shell after = before.press();
		set(soul, after);
		soul.level().playSound(null, soul.blockPosition(), after.still() ? SoundEvents.ARMOR_EQUIP_IRON.value()
				: SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.8f, after.still() ? 0.7f : 1.1f);
		soul.sendOverlayMessage(Component.translatable(after.still() ? "shell.fmab.still" : "shell.fmab.awake"));
	}

	/** Le joueur entre dans l'armure figée. */
	public static void enter(ServerPlayer occupant, ServerPlayer soul) {
		Pair pair = SHELLS.computeIfAbsent(soul.getUUID(), id -> new Pair(soul));
		pair.occupant = occupant;
		set(soul, shell(soul).enter(occupant.getUUID()));
		FmabAttachments.INSIDE.set(occupant, soul.getUUID());
		occupant.stopRiding();
		follow(occupant, soul, false);
		soul.level().playSound(null, soul.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS,
				1, 0.6f);
		occupant.sendOverlayMessage(Component.translatable("shell.fmab.entered", soul.getDisplayName()));
		soul.sendOverlayMessage(Component.translatable("shell.fmab.worn", occupant.getDisplayName()));
	}

	private static void set(ServerPlayer soul, Shell value) {
		if (value.equals(Shell.NONE)) {
			FmabAttachments.SHELL.remove(soul);
			Pair pair = SHELLS.remove(soul.getUUID());
			if (pair != null && pair.occupant != null) {
				free(pair.occupant);
			}
			lock(soul, false);
			return;
		}
		FmabAttachments.SHELL.set(soul, value);
		SHELLS.computeIfAbsent(soul.getUUID(), id -> new Pair(soul));
	}

	/**
	 * Celui qui est dedans ressort, derrière l'armure. Elle devient {@code after} : figée s'il en est
	 * sorti de lui-même, éveillée si l'âme l'a relâché.
	 */
	private static void eject(Pair pair, Shell after) {
		ServerPlayer occupant = pair.occupant;
		pair.occupant = null;
		if (occupant == null) {
			return;
		}
		free(occupant);
		set(pair.soul, after);
		Vec3 back = Vec3.directionFromRotation(0, pair.soul.getYRot()).scale(-1.1);
		if (occupant.level() == pair.soul.level()) {
			occupant.teleportTo((ServerLevel) pair.soul.level(), pair.soul.getX() + back.x, pair.soul.getY(),
					pair.soul.getZ() + back.z, Set.of(Relative.Y_ROT, Relative.X_ROT), 0, 0, false);
		}
		pair.soul.level().playSound(null, pair.soul.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(),
				SoundSource.PLAYERS, 1, 0.9f);
	}

	/** Le joueur n'est plus dans aucune armure : il se revoit, se meut et agit. */
	private static void free(ServerPlayer occupant) {
		FmabAttachments.INSIDE.remove(occupant);
		lock(occupant, false);
	}

	/** Tout se défait : l'occupant ressort, l'âme s'éveille. */
	private static void release(Pair pair) {
		if (pair.occupant != null) {
			eject(pair, Shell.NONE);
		}
		set(pair.soul, Shell.NONE);
	}

	// ---------------------------------------------------------------- chaque tick

	private static void tick(Pair pair) {
		ServerPlayer soul = pair.soul;
		ServerPlayer occupant = pair.occupant;
		Shell shell = shell(soul);
		boolean gone = soul.isRemoved() || !soul.isAlive() || !inArmor(soul);
		boolean lost = occupant != null && (occupant.isRemoved() || !occupant.isAlive()
				|| occupant.level() != soul.level());
		if (gone || lost) {
			release(pair);
			return;
		}
		if (occupant == null) {
			// Figée et vide : l'armure ne bouge plus, jusqu'à ce que l'âme le veuille.
			if (shell.still() && moving(soul.getLastClientInput())) {
				set(soul, shell.move());
				soul.sendOverlayMessage(Component.translatable("shell.fmab.awake"));
				return;
			}
			lock(soul, shell.still());
			if (!shell.still()) {
				SHELLS.remove(soul.getUUID());
			}
			return;
		}
		if (shell.canLeave() && occupant.getLastClientInput().shift()) {
			eject(pair, shell.leave());
			occupant.sendOverlayMessage(Component.translatable("shell.fmab.left"));
			return;
		}
		if (shell.worn() && moving(soul.getLastClientInput())) {
			shell = shell.move();
			set(soul, shell);
			occupant.sendOverlayMessage(Component.translatable("shell.fmab.trapped", soul.getDisplayName()));
			soul.sendOverlayMessage(Component.translatable("shell.fmab.holding", occupant.getDisplayName()));
		}
		if (shell.worn()) {
			// L'occupant mène : l'armure le suit, tournée comme lui.
			lock(soul, true);
			lock(occupant, false);
			follow(soul, occupant, true);
		} else {
			// L'âme mène : l'occupant est emporté, sans rien pouvoir y faire.
			lock(occupant, true);
			lock(soul, false);
			follow(occupant, soul, false);
		}
	}

	/** Ramène {@code carried} sur {@code driver} ; l'armure portée prend aussi la direction de qui la porte. */
	private static void follow(ServerPlayer carried, ServerPlayer driver, boolean turn) {
		carried.resetFallDistance();
		carried.setDeltaMovement(Vec3.ZERO);
		if (turn) {
			carried.teleportTo((ServerLevel) driver.level(), driver.getX(), driver.getY(), driver.getZ(),
					Set.of(Relative.X_ROT), driver.getYRot(), 0, false);
			carried.setYHeadRot(driver.getYRot());
			carried.yBodyRot = driver.yBodyRot;
		} else {
			carried.teleportTo((ServerLevel) driver.level(), driver.getX(), driver.getY(), driver.getZ(),
					Set.of(Relative.Y_ROT, Relative.X_ROT), 0, 0, false);
		}
	}

	private static void lock(ServerPlayer player, boolean locked) {
		for (Holder<Attribute> attribute : LOCKED) {
			AttributeInstance instance = player.getAttribute(attribute);
			if (instance == null) {
				continue;
			}
			if (locked) {
				instance.addOrUpdateTransientModifier(new AttributeModifier(LOCK, -1,
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			} else {
				instance.removeModifier(LOCK);
			}
		}
	}
}
