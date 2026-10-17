package com.ajustor.fmab.gate;

import com.ajustor.fmab.entity.WheelchairEntity;
import com.ajustor.fmab.registry.FmabAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import com.ajustor.fmab.platform.event.InteractionEvents;

/**
 * Les règles du fauteuil roulant côté joueur : il faut ses deux bras pour faire tourner les roues (ou
 * pour pousser), et des mains qui poussent ne font rien d'autre. Sans jambes et sans fauteuil, on
 * rampe. Un villageois pousse contre une émeraude.
 *
 * <p>Les règles elles-mêmes sont des fonctions pures ({@link #canPropel(boolean, boolean)},
 * {@link #mustCrawl}), testées à part ; le reste les applique au joueur.
 */
public final class Wheelchairs {
	private Wheelchairs() {
	}

	public static void register() {
		InteractionEvents.USE_ITEM.register((player, level, hand) -> busy(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		InteractionEvents.USE_BLOCK.register((player, level, hand, hit) -> busy(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		InteractionEvents.ATTACK_BLOCK.register((player, level, hand, pos, direction) ->
				busy(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		InteractionEvents.ATTACK_ENTITY.register((player, level, hand, entity, hit) ->
				busy(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		InteractionEvents.BEFORE_BREAK.register((level, player, pos, state, blockEntity) -> !busy(player));
		InteractionEvents.USE_ENTITY.register((player, level, hand, entity, hit) -> {
			if (entity instanceof WheelchairEntity) {
				// Prendre ou lâcher les poignées, s'asseoir : le fauteuil s'en occupe.
				return InteractionResult.PASS;
			}
			if (entity instanceof Villager villager && player.getVehicle() instanceof WheelchairEntity chair) {
				ItemStack held = player.getItemInHand(hand);
				if (held.is(Items.EMERALD) && !villager.isBaby() && !villager.isSleeping() && chair.pusher() == null) {
					if (player instanceof ServerPlayer server) {
						held.consume(1, player);
						chair.hire(villager);
						// Le villageois est content : des étincelles vertes, comme après un échange.
						level.broadcastEntityEvent(villager, (byte) 14);
						server.sendOverlayMessage(Component.translatable("wheelchair.fmab.hired"));
					}
					return InteractionResult.SUCCESS;
				}
			}
			return busy(player) ? InteractionResult.FAIL : InteractionResult.PASS;
		});
	}

	/** Les mains du joueur poussent des roues ou des poignées : il le dit, et ne fait rien d'autre. */
	private static boolean busy(Player player) {
		if (!handsBusy(player)) {
			return false;
		}
		if (player instanceof ServerPlayer server) {
			server.sendOverlayMessage(Component.translatable("wheelchair.fmab.hands_busy"));
		}
		return true;
	}

	/** Deux bras (de chair, ou d'automail en état) pour faire tourner deux roues. */
	public static boolean canPropel(boolean rightArm, boolean leftArm) {
		return rightArm && leftArm;
	}

	public static boolean canPropel(Player player) {
		return canPropel(!Tolls.disabled(player, BodyPart.RIGHT_ARM), !Tolls.disabled(player, BodyPart.LEFT_ARM));
	}

	/**
	 * Les mains sont-elles prises : on fait tourner ses roues (le fauteuil avance, personne ne
	 * pousse), ou l'on pousse le fauteuil de quelqu'un ?
	 */
	public static boolean handsBusy(Player player) {
		if (player.getVehicle() instanceof WheelchairEntity chair) {
			return chair.propelling();
		}
		return pushing(player) != null;
	}

	/**
	 * Le fauteuil dont ce joueur tient les poignées : celui que le joueur retient ({@link
	 * FmabAttachments#PUSHING}), s'il est toujours là et le reconnaît pour pousseur.
	 */
	public static @Nullable WheelchairEntity pushing(Player player) {
		Integer id = FmabAttachments.PUSHING.get(player);
		if (id == null) {
			return null;
		}
		return player.level().getEntity(id) instanceof WheelchairEntity chair && chair.pushedBy(player) ? chair : null;
	}

	/** Les deux jambes manquent, sans automail en état pour les remplacer. */
	public static boolean legless(Player player) {
		return Tolls.disabled(player, BodyPart.LEFT_LEG) && Tolls.disabled(player, BodyPart.RIGHT_LEG);
	}

	/**
	 * Sans jambes, hors de toute monture (fauteuil, cheval…), on rampe : sauf en volant en créatif, en
	 * planant, en dormant ou en spectateur.
	 */
	public static boolean mustCrawl(boolean legless, boolean riding, boolean flying, boolean sleeping) {
		return legless && !riding && !flying && !sleeping;
	}

	public static boolean mustCrawl(Player player) {
		return mustCrawl(legless(player), player.isPassenger(),
				player.getAbilities().flying || player.isFallFlying() || player.isSpectator(), player.isSleeping());
	}
}
