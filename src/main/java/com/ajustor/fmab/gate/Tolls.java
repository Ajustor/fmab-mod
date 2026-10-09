package com.ajustor.fmab.gate;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Ce que coûte au quotidien ce que la Vérité a pris :
 * <ul>
 *   <li>un bras : la main de ce côté ne tient rien et ne frappe pas ;</li>
 *   <li>une jambe : 30 % de vitesse en moins, pas de course, saut réduit ;</li>
 *   <li>les organes : quatre cœurs en moins et une toux qui fait mal ;</li>
 *   <li>la vue : un voile sombre (dessiné par le client) ;</li>
 *   <li>le corps : l'âme vit dans une armure, sans faim ni souffle. Les coups usent les pièces ; une
 *   pièce qui cède se refait (sans heaume on voit mal, sans jambières ni solerets on avance mal). Si
 *   le plastron et son sceau cèdent, l'âme retourne errer devant la Porte.</li>
 * </ul>
 * Un automail en état remplace le membre perdu.
 */
public final class Tolls {
	private static final Identifier LEFT_LEG_SPEED = Fmab.id("toll_left_leg_speed");
	private static final Identifier RIGHT_LEG_SPEED = Fmab.id("toll_right_leg_speed");
	private static final Identifier LEFT_LEG_JUMP = Fmab.id("toll_left_leg_jump");
	private static final Identifier RIGHT_LEG_JUMP = Fmab.id("toll_right_leg_jump");
	private static final Identifier ORGANS_HEALTH = Fmab.id("toll_organs_health");
	private static final Identifier SOUL_NO_LEGGINGS = Fmab.id("soul_no_leggings");
	private static final Identifier SOUL_NO_BOOTS = Fmab.id("soul_no_boots");
	private static final double LEG_SPEED = -0.3;
	private static final double LEG_JUMP = -0.3;
	private static final double SOUL_NO_BOOTS_SPEED = -0.15;
	private static final double ORGANS_MAX_HEALTH = -8;
	/** En moyenne une quinte de toux toutes les deux minutes. */
	private static final int COUGH_CHANCE = 2400;
	/** Usure de chaque pièce d'une armure d'âme, par point de dégât encaissé. */
	private static final int SOUL_WEAR_PER_DAMAGE = 2;
	/** Une unité de matériau transmutée dans l'armure en répare un quart, comme à l'enclume. */
	private static final int REPAIR_STEPS = 4;
	/** L'invisibilité d'une âme en armure : on ne voit que l'armure, qui flotte, vide. */
	private static final int HOLLOW_DURATION = 220;

	private Tolls() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tick(player);
			}
		});
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				disabled(player, BodyPart.RIGHT_ARM) ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				disabled(player, BodyPart.RIGHT_ARM) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (disabled(player, armOf(hand))) {
				return InteractionResult.FAIL;
			}
			if (player.isShiftKeyDown() && player instanceof ServerPlayer server && mendWith(server, player.getItemInHand(hand))) {
				return InteractionResult.SUCCESS;
			}
			if (soulBound(player) && player.getItemInHand(hand).has(DataComponents.FOOD)) {
				// Une armure ne mange pas.
				if (player instanceof ServerPlayer server) {
					server.sendOverlayMessage(Component.translatable("gate.fmab.cannot_eat"));
				}
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		// Sans bras droit, on ouvre un coffre ou une porte de la main gauche : seul celui qui n'a plus
		// aucun bras ne peut plus rien manier.
		UseBlockCallback.EVENT.register((player, level, hand, hit) ->
				noHands(player) || hand == InteractionHand.OFF_HAND && disabled(player, BodyPart.LEFT_ARM)
						? InteractionResult.FAIL : InteractionResult.PASS);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
				return true;
			}
			GateState gate = player.getAttached(FmabAttachments.GATE);
			if (gate == null) {
				return true;
			}
			if (gate.visit().isPresent() || gate.adrift()) {
				// Dans l'Espace blanc, rien ne blesse : la Vérité prend ce qu'elle veut, elle.
				return false;
			}
			if (gate.inArmor()) {
				dent(player, amount);
				return false;
			}
			return true;
		});
		// Une âme ne meurt pas : quand son armure tombe, elle retourne devant la Porte.
		ServerPlayerEvents.ALLOW_DEATH.register((player, source, amount) -> {
			GateState gate = player.getAttached(FmabAttachments.GATE);
			if (gate == null || !gate.soulBound()) {
				return true;
			}
			player.setHealth(player.getMaxHealth());
			if (gate.inArmor()) {
				sealErased(player);
			}
			return false;
		});
	}

	/** Plus aucun bras pour manier quoi que ce soit. */
	private static boolean noHands(Player player) {
		return disabled(player, BodyPart.RIGHT_ARM) && disabled(player, BodyPart.LEFT_ARM);
	}

	/** La main droite tient l'objet principal ; la gauche, l'autre main. */
	private static BodyPart armOf(InteractionHand hand) {
		return hand == InteractionHand.MAIN_HAND ? BodyPart.RIGHT_ARM : BodyPart.LEFT_ARM;
	}

	private static boolean soulBound(Player player) {
		GateState gate = player.getAttached(FmabAttachments.GATE);
		return gate != null && gate.soulBound();
	}

	/**
	 * Le membre ou l'organe manque-t-il, sans rien pour le remplacer ? Une âme fixée dans une armure
	 * a des bras et des jambes d'acier.
	 */
	public static boolean disabled(Player player, BodyPart part) {
		GateState gate = player.getAttached(FmabAttachments.GATE);
		if (gate == null || gate.soulBound() || !gate.lost(part)) {
			return false;
		}
		return !part.limb() || !Automails.working(player, part);
	}

	private static void tick(ServerPlayer player) {
		GateState gate = player.getAttached(FmabAttachments.GATE);
		if (gate == null) {
			return;
		}
		dropFromLostHands(player, gate);
		legs(player);
		organs(player);
		soulLimbs(player, gate);
		if (gate.soulBound()) {
			soul(player, gate);
		} else {
			hollow(player, false);
		}
	}

	/** Une main absente ne tient rien : ce qu'elle tenait passe dans le sac, ou tombe. */
	public static void dropFromLostHands(ServerPlayer player, GateState gate) {
		if (disabled(player, BodyPart.RIGHT_ARM) && !player.getMainHandItem().isEmpty()) {
			stow(player, player.getMainHandItem().copy());
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
		if (disabled(player, BodyPart.LEFT_ARM) && !player.getOffhandItem().isEmpty()) {
			stow(player, player.getOffhandItem().copy());
			player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
		}
	}

	/** Range un objet ailleurs que dans la case tenue, ou le laisse tomber. */
	private static void stow(ServerPlayer player, ItemStack stack) {
		var inventory = player.getInventory();
		int selected = inventory.getSelectedSlot();
		for (int i = 0; i < inventory.getNonEquipmentItems().size(); i++) {
			if (i != selected && inventory.getItem(i).isEmpty()) {
				inventory.setItem(i, stack);
				return;
			}
		}
		player.drop(stack, false);
	}

	private static void legs(ServerPlayer player) {
		boolean left = disabled(player, BodyPart.LEFT_LEG);
		boolean right = disabled(player, BodyPart.RIGHT_LEG);
		modifier(player, Attributes.MOVEMENT_SPEED, LEFT_LEG_SPEED, left ? LEG_SPEED : 0);
		modifier(player, Attributes.MOVEMENT_SPEED, RIGHT_LEG_SPEED, right ? LEG_SPEED : 0);
		modifier(player, Attributes.JUMP_STRENGTH, LEFT_LEG_JUMP, left ? LEG_JUMP : 0);
		modifier(player, Attributes.JUMP_STRENGTH, RIGHT_LEG_JUMP, right ? LEG_JUMP : 0);
		if ((left || right) && player.isSprinting()) {
			player.setSprinting(false);
		}
	}

	private static void organs(ServerPlayer player) {
		boolean missing = disabled(player, BodyPart.ORGANS);
		modifier(player, Attributes.MAX_HEALTH, ORGANS_HEALTH, missing ? ORGANS_MAX_HEALTH : 0);
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		if (missing && player.getRandom().nextInt(COUGH_CHANCE) == 0) {
			player.hurtServer(player.level(), player.level().damageSources().generic(), 1);
			player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8f, 0.6f);
			player.sendOverlayMessage(Component.translatable("gate.fmab.cough"));
		}
	}

	/** Les jambes d'une armure d'âme sont ses jambières et ses solerets : sans eux, on avance mal. */
	private static void soulLimbs(ServerPlayer player, GateState gate) {
		boolean armored = gate.inArmor();
		boolean noLeggings = armored && player.getItemBySlot(EquipmentSlot.LEGS).isEmpty();
		boolean noBoots = armored && player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
		modifier(player, Attributes.MOVEMENT_SPEED, SOUL_NO_LEGGINGS, noLeggings ? LEG_SPEED : 0);
		modifier(player, Attributes.MOVEMENT_SPEED, SOUL_NO_BOOTS, noBoots ? SOUL_NO_BOOTS_SPEED : 0);
		if (noLeggings && player.isSprinting()) {
			player.setSprinting(false);
		}
	}

	/** Une armure n'a ni faim ni souffle ; et sans son sceau, l'âme s'en va. */
	private static void soul(ServerPlayer player, GateState gate) {
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(5);
		player.setAirSupply(player.getMaxAirSupply());
		hollow(player, gate.inArmor());
		if (gate.inArmor() && gate.visit().isEmpty() && !SoulArmor.sealIntact(player)) {
			sealErased(player);
		}
	}

	/**
	 * L'armure d'une âme est vide : le corps du joueur est invisible, seule l'armure se voit. On
	 * reconnaît cette invisibilité-là à ce qu'elle n'a ni particules ni icône.
	 */
	private static void hollow(ServerPlayer player, boolean armored) {
		MobEffectInstance current = player.getEffect(MobEffects.INVISIBILITY);
		boolean ours = current != null && current.isAmbient() && !current.isVisible();
		if (armored && (current == null || ours && current.getDuration() < HOLLOW_DURATION / 2)) {
			player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, HOLLOW_DURATION, 0, true, false, false));
		} else if (!armored && ours) {
			player.removeEffect(MobEffects.INVISIBILITY);
		}
	}

	/**
	 * L'âme transmute le matériau qu'elle tient dans son armure : un lingot de fer répare un quart
	 * d'une pièce de fer. Le plastron (le sceau) d'abord, puis la pièce la plus abîmée.
	 *
	 * @return vrai si une pièce a été réparée
	 */
	public static boolean mendWith(ServerPlayer player, ItemStack material) {
		GateState gate = player.getAttached(FmabAttachments.GATE);
		if (gate == null || !gate.inArmor() || material.isEmpty()) {
			return false;
		}
		ItemStack target = ItemStack.EMPTY;
		for (EquipmentSlot slot : SoulArmor.SLOTS) {
			ItemStack piece = player.getItemBySlot(slot);
			if (!piece.isDamaged() || !piece.isValidRepairItem(material)) {
				continue;
			}
			if (slot == EquipmentSlot.CHEST) {
				target = piece;
				break;
			}
			if (target.isEmpty() || piece.getDamageValue() * target.getMaxDamage() > target.getDamageValue() * piece.getMaxDamage()) {
				target = piece;
			}
		}
		if (target.isEmpty()) {
			return false;
		}
		target.setDamageValue(Math.max(0, target.getDamageValue() - Math.max(1, target.getMaxDamage() / REPAIR_STEPS)));
		if (!player.isCreative()) {
			material.shrink(1);
		}
		TransmutationLightning.discharge(player.level(), player.blockPosition(), 0.7, 0.5);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5f, 1.6f);
		return true;
	}

	/**
	 * Un coup use chaque pièce portée. Une pièce qui cède tombe : on en refera une. Si c'est le
	 * plastron, le sceau est effacé.
	 */
	private static void dent(ServerPlayer player, float amount) {
		if (!SoulArmor.sealIntact(player)) {
			sealErased(player);
			return;
		}
		int wear = Math.max(1, Math.round(amount * SOUL_WEAR_PER_DAMAGE));
		player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.4f);
		for (EquipmentSlot slot : SoulArmor.SLOTS) {
			ItemStack piece = player.getItemBySlot(slot);
			if (piece.isEmpty() || !piece.isDamageableItem()) {
				continue;
			}
			if (piece.getDamageValue() + wear < piece.getMaxDamage()) {
				piece.setDamageValue(piece.getDamageValue() + wear);
				continue;
			}
			player.setItemSlot(slot, ItemStack.EMPTY);
			player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1, 0.7f);
			if (slot == EquipmentSlot.CHEST) {
				sealErased(player);
				return;
			}
			player.sendOverlayMessage(Component.translatable("gate.fmab.soul.piece_broken"));
		}
	}

	/**
	 * Le sceau est effacé : l'armure s'effondre, ses pièces tombent, et l'âme retourne errer devant
	 * la Porte jusqu'à ce qu'un cercle d'âme l'appelle.
	 */
	private static void sealErased(ServerPlayer player) {
		GateState gate = player.getAttachedOrCreate(FmabAttachments.GATE);
		for (EquipmentSlot slot : SoulArmor.SLOTS) {
			ItemStack piece = player.getItemBySlot(slot);
			if (!piece.isEmpty() && slot != EquipmentSlot.CHEST) {
				player.drop(piece.copy(), false);
			}
			player.setItemSlot(slot, ItemStack.EMPTY);
		}
		player.setAttached(FmabAttachments.GATE, gate.withAdrift(true));
		player.sendSystemMessage(Component.translatable("gate.fmab.seal_erased").withStyle(s -> s.withColor(0xB0201A)));
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
					attribute == Attributes.MAX_HEALTH ? AttributeModifier.Operation.ADD_VALUE
							: AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}
}
