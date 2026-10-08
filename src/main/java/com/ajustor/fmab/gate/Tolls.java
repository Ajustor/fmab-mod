package com.ajustor.fmab.gate;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.registry.FmabAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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
 *   <li>le corps : l'âme vit dans une armure, sans faim ni souffle ; les coups bosselent l'armure,
 *   et si le sceau du plastron est effacé, l'âme s'en va.</li>
 * </ul>
 * Un automail en état remplace le membre perdu.
 */
public final class Tolls {
	private static final Identifier LEFT_LEG_SPEED = Fmab.id("toll_left_leg_speed");
	private static final Identifier RIGHT_LEG_SPEED = Fmab.id("toll_right_leg_speed");
	private static final Identifier LEFT_LEG_JUMP = Fmab.id("toll_left_leg_jump");
	private static final Identifier RIGHT_LEG_JUMP = Fmab.id("toll_right_leg_jump");
	private static final Identifier ORGANS_HEALTH = Fmab.id("toll_organs_health");
	private static final double LEG_SPEED = -0.3;
	private static final double LEG_JUMP = -0.3;
	private static final double ORGANS_MAX_HEALTH = -8;
	/** En moyenne une quinte de toux toutes les deux minutes. */
	private static final int COUGH_CHANCE = 2400;
	/** Usure de l'armure d'âme par point de dégât encaissé. */
	private static final int SOUL_WEAR_PER_DAMAGE = 3;

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
			if (soulBound(player) && player.getItemInHand(hand).has(DataComponents.FOOD)) {
				// Une armure ne mange pas.
				if (player instanceof ServerPlayer server) {
					server.sendOverlayMessage(Component.translatable("gate.fmab.cannot_eat"));
				}
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) ->
				disabled(player, armOf(hand)) ? InteractionResult.FAIL : InteractionResult.PASS);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
				return true;
			}
			GateState gate = player.getAttached(FmabAttachments.GATE);
			if (gate == null) {
				return true;
			}
			if (gate.visit().isPresent()) {
				// Dans l'Espace blanc, rien ne blesse : la Vérité prend ce qu'elle veut, elle.
				return false;
			}
			if (gate.soulBound()) {
				dent(player, gate, amount);
				return false;
			}
			return true;
		});
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
		if (gate == null || !gate.lost(part) || gate.soulBound()) {
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
		organs(player, gate);
		if (gate.soulBound()) {
			soul(player);
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

	private static void organs(ServerPlayer player, GateState gate) {
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

	/** Une armure n'a ni faim ni souffle ; et sans son sceau, plus d'âme. */
	private static void soul(ServerPlayer player) {
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(5);
		player.setAirSupply(player.getMaxAirSupply());
		if (!SoulArmor.sealIntact(player)) {
			sealErased(player);
		}
	}

	/** Un coup bosselle l'armure ; si le plastron cède, le sceau est effacé. */
	private static void dent(ServerPlayer player, GateState gate, float amount) {
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		if (!SoulArmor.sealIntact(player)) {
			sealErased(player);
			return;
		}
		int wear = Math.max(1, Math.round(amount * SOUL_WEAR_PER_DAMAGE));
		player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.4f);
		if (chest.getDamageValue() + wear >= chest.getMaxDamage()) {
			player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			sealErased(player);
			return;
		}
		chest.setDamageValue(chest.getDamageValue() + wear);
	}

	/** Le sceau est effacé : l'âme quitte l'armure, et le joueur meurt. */
	private static void sealErased(ServerPlayer player) {
		GateState gate = player.getAttachedOrCreate(FmabAttachments.GATE);
		player.setAttached(FmabAttachments.GATE, gate.releaseSoul());
		player.sendSystemMessage(Component.translatable("gate.fmab.seal_erased").withStyle(s -> s.withColor(0xB0201A)));
		player.kill(player.level());
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
