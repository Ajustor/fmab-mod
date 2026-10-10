package com.ajustor.fmab.gate;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.Transient;
import com.ajustor.fmab.item.AutomailItem;
import com.ajustor.fmab.registry.FmabAttachments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Les règles de l'automail : on ne le pose que sur un membre perdu ; il s'use (un coup ou un bloc
 * cassé pour un bras, la marche pour une jambe) ; usé jusqu'au bout, il ne répond plus jusqu'à ce
 * que Winry le répare.
 */
public final class Automails {
	private static final List<BodyPart> LIMBS = List.of(BodyPart.RIGHT_ARM, BodyPart.LEFT_ARM, BodyPart.RIGHT_LEG,
			BodyPart.LEFT_LEG);
	private static final Identifier RUSH_LEG_SPEED = Fmab.id("automail_rush_valley_speed");
	private static final Identifier RUSH_ARM_SPEED = Fmab.id("automail_rush_valley_attack");
	private static final double RUSH_SPEED = 0.08;
	private static final double RUSH_ATTACK = 0.15;
	/** Une jambe s'use d'un point tous les ce nombre de blocs parcourus. */
	private static final double BLOCKS_PER_WEAR = 24;
	/** Le prix de la réparation : un lingot de fer par quart de l'usure. */
	public static final int REPAIR_STEPS = 4;

	private static final Map<UUID, Vec3> LAST_POSITIONS = Transient.perPlayer(new HashMap<>());
	private static final Map<UUID, Double> WALKED = Transient.perPlayer(new HashMap<>());

	private Automails() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tick(player);
			}
		});
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (player instanceof ServerPlayer server) {
				wear(server, BodyPart.RIGHT_ARM, 1);
			}
			return InteractionResult.PASS;
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayer server) {
				wear(server, BodyPart.RIGHT_ARM, 1);
			}
		});
	}

	/** Usé jusqu'au bout : on garde la pièce, mais elle ne répond plus. */
	public static boolean broken(ItemStack stack) {
		return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
	}

	/** Un automail en état remplace-t-il ce membre ? */
	public static boolean working(Player player, BodyPart part) {
		Automail automail = player.getAttached(FmabAttachments.AUTOMAIL);
		if (automail == null) {
			return false;
		}
		ItemStack stack = automail.get(part);
		return stack.getItem() instanceof AutomailItem item && item.fits(part) && !broken(stack);
	}

	/** Le membre perdu que cette pièce peut remplacer, le côté droit d'abord. */
	public static Optional<BodyPart> slotFor(Player player, AutomailItem item) {
		GateState gate = player.getAttached(FmabAttachments.GATE);
		Automail automail = player.getAttachedOrCreate(FmabAttachments.AUTOMAIL);
		if (gate == null) {
			return Optional.empty();
		}
		return LIMBS.stream()
				.filter(item::fits)
				.filter(gate::lost)
				.filter(part -> automail.get(part).isEmpty())
				.findFirst();
	}

	/**
	 * Pose la pièce tenue sur un membre perdu. Renvoie vrai si elle a été posée (la pièce quitte
	 * alors la main).
	 */
	public static boolean fit(ServerPlayer player, ItemStack held) {
		if (!(held.getItem() instanceof AutomailItem item)) {
			return false;
		}
		Optional<BodyPart> part = slotFor(player, item);
		if (part.isEmpty()) {
			player.sendOverlayMessage(Component.translatable(item.arm() ? "automail.fmab.no_lost_arm"
					: "automail.fmab.no_lost_leg"));
			return false;
		}
		fitTo(player, part.get(), held.copyWithCount(1));
		held.shrink(1);
		return true;
	}

	/**
	 * Branche une pièce sur ce membre (la page du corps le fait aussi). À l'appelant de vérifier que
	 * le membre est perdu et libre, et que la pièce lui va.
	 */
	public static void fitTo(ServerPlayer player, BodyPart part, ItemStack piece) {
		Automail automail = player.getAttachedOrCreate(FmabAttachments.AUTOMAIL);
		player.setAttached(FmabAttachments.AUTOMAIL, automail.with(part, piece));
		// Brancher les nerfs fait mal : Ed s'en souvient.
		player.hurtServer(player.level(), player.level().damageSources().generic(), 2);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
		player.sendSystemMessage(Component.translatable("automail.fmab.fitted", Component.translatable(part.translationKey())));
	}

	/** Ce membre peut-il recevoir cette pièce : perdu, libre, et la pièce est faite pour lui ? */
	public static boolean canFit(Player player, BodyPart part, ItemStack piece) {
		GateState gate = player.getAttached(FmabAttachments.GATE);
		Automail automail = player.getAttached(FmabAttachments.AUTOMAIL);
		return piece.getItem() instanceof AutomailItem item && item.fits(part) && gate != null && !gate.soulBound()
				&& gate.lost(part) && (automail == null || automail.get(part).isEmpty());
	}

	/** Retire la pièce d'un membre ; elle revient dans l'inventaire. */
	public static void remove(ServerPlayer player, BodyPart part) {
		Automail automail = player.getAttachedOrCreate(FmabAttachments.AUTOMAIL);
		ItemStack stack = automail.get(part);
		if (stack.isEmpty()) {
			return;
		}
		player.setAttached(FmabAttachments.AUTOMAIL, automail.with(part, ItemStack.EMPTY));
		if (!player.getInventory().add(stack.copy())) {
			player.drop(stack.copy(), false);
		}
	}

	/** Lingots de fer que demande la réparation d'une pièce. */
	public static int repairCost(ItemStack stack) {
		if (!stack.isDamaged()) {
			return 0;
		}
		return (int) Math.ceil((double) stack.getDamageValue() * REPAIR_STEPS / stack.getMaxDamage());
	}

	/** Winry répare une pièce contre des lingots de fer pris dans l'inventaire. */
	public static boolean repair(ServerPlayer player, BodyPart part) {
		Automail automail = player.getAttachedOrCreate(FmabAttachments.AUTOMAIL);
		ItemStack stack = automail.get(part).copy();
		int cost = repairCost(stack);
		if (cost == 0) {
			return false;
		}
		if (!player.isCreative() && player.getInventory().countItem(Items.IRON_INGOT) < cost) {
			player.sendOverlayMessage(Component.translatable("automail.fmab.repair_cost", cost));
			return false;
		}
		if (!player.isCreative()) {
			int left = cost;
			for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
				ItemStack s = player.getInventory().getItem(i);
				if (s.is(Items.IRON_INGOT)) {
					int taken = Math.min(left, s.getCount());
					s.shrink(taken);
					left -= taken;
				}
			}
		}
		stack.setDamageValue(0);
		player.setAttached(FmabAttachments.AUTOMAIL, automail.with(part, stack));
		player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1, 1);
		return true;
	}

	/** L'usure d'une pièce ; à bout, elle lâche et le membre manque de nouveau. */
	public static void wear(ServerPlayer player, BodyPart part, int amount) {
		if (player.isCreative()) {
			return;
		}
		Automail automail = player.getAttached(FmabAttachments.AUTOMAIL);
		if (automail == null) {
			return;
		}
		ItemStack stack = automail.get(part);
		if (!(stack.getItem() instanceof AutomailItem) || broken(stack)) {
			return;
		}
		ItemStack worn = stack.copy();
		int damage = Math.min(worn.getMaxDamage() - 1, worn.getDamageValue() + amount);
		worn.setDamageValue(damage);
		player.setAttached(FmabAttachments.AUTOMAIL, automail.with(part, worn));
		if (broken(worn)) {
			player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1, 0.8f);
			player.sendSystemMessage(Component.translatable("automail.fmab.broke",
					Component.translatable(part.translationKey())));
		}
	}

	private static void tick(ServerPlayer player) {
		Automail automail = player.getAttached(FmabAttachments.AUTOMAIL);
		if (automail == null || automail.limbs().isEmpty()) {
			modifier(player, Attributes.MOVEMENT_SPEED, RUSH_LEG_SPEED, 0);
			modifier(player, Attributes.ATTACK_SPEED, RUSH_ARM_SPEED, 0);
			return;
		}
		int rushLegs = 0;
		boolean rushArm = false;
		boolean briggs = false;
		for (BodyPart part : LIMBS) {
			if (!working(player, part)) {
				continue;
			}
			AutomailItem item = (AutomailItem) automail.get(part).getItem();
			switch (item.model()) {
				case RUSH_VALLEY -> {
					if (part.leg()) {
						rushLegs++;
					} else if (part == BodyPart.RIGHT_ARM) {
						rushArm = true;
					}
				}
				case BRIGGS -> briggs = true;
				default -> {
				}
			}
		}
		modifier(player, Attributes.MOVEMENT_SPEED, RUSH_LEG_SPEED, rushLegs * RUSH_SPEED);
		modifier(player, Attributes.ATTACK_SPEED, RUSH_ARM_SPEED, rushArm ? RUSH_ATTACK : 0);
		if (briggs && player.getTicksFrozen() > 0) {
			player.setTicksFrozen(0);
		}
		walk(player);
	}

	/** La marche use les jambes d'automail ; celles de Rush Valley plus vite, celles de Briggs moins. */
	private static void walk(ServerPlayer player) {
		UUID id = player.getUUID();
		Vec3 now = player.position();
		Vec3 last = LAST_POSITIONS.put(id, now);
		if (last == null || !player.onGround()) {
			return;
		}
		double step = Math.min(1, Math.hypot(now.x - last.x, now.z - last.z));
		double walked = WALKED.merge(id, step, Double::sum);
		if (walked < BLOCKS_PER_WEAR) {
			return;
		}
		WALKED.put(id, 0.0);
		Automail automail = player.getAttachedOrCreate(FmabAttachments.AUTOMAIL);
		for (BodyPart leg : new BodyPart[]{BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG}) {
			if (automail.get(leg).getItem() instanceof AutomailItem item) {
				int wear = switch (item.model()) {
					case RUSH_VALLEY -> 2;
					case BRIGGS -> player.getRandom().nextBoolean() ? 1 : 0;
					default -> 1;
				};
				if (wear > 0) {
					wear(player, leg, wear);
				}
			}
		}
	}

	private static void modifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		if (amount == 0) {
			if (instance.getModifier(id) != null) {
				instance.removeModifier(id);
			}
		} else {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}
}
