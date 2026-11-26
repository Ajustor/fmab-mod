package com.ajustor.fmab.tattoo;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.Engravings;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.Tattoos;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Le rituel du tatouage : le cercle tracé au sol passe dans la peau. Il faut de l'encre
 * alchimique en main, et le rituel fait mal. Effacer un tatouage coûte davantage encore. Sur un bras
 * d'automail, le même rituel grave le cercle dans l'acier ({@link BodyCircles}).
 */
public final class TattooRitual {
	private static final float PAIN = 6;
	private static final float ERASE_PAIN = 8;
	private static final int ERASE_INK = 2;
	private static final double REACH = 6;

	private TattooRitual() {
	}

	/**
	 * Inscrire sur {@code slot} le cercle tracé en {@code circle} : tatoué dans la peau, ou gravé dans
	 * l'automail qui remplace un bras perdu. Un bras perdu sans automail, ou dont l'automail a lâché, ne
	 * reçoit rien.
	 */
	public static void tattoo(ServerPlayer player, BlockPos circle, TattooSlot slot) {
		ServerLevel level = player.level();
		ItemStack ink = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (!ink.is(FmabItems.ALCHEMICAL_INK) || player.distanceToSqr(Vec3.atCenterOf(circle)) > REACH * REACH
				|| !(level.getBlockEntity(circle) instanceof TransmutationCircleBlockEntity be)) {
			return;
		}
		BodyCircles.Surface surface = BodyCircles.surface(player, slot);
		if (surface == BodyCircles.Surface.NONE || surface == BodyCircles.Surface.BROKEN) {
			player.sendOverlayMessage(Component.translatable(surface == BodyCircles.Surface.NONE
					? "tattoo.fmab.no_limb" : "tattoo.fmab.automail_broken"));
			return;
		}
		Drawing drawing = be.drawing();
		int stages = AlchemyRules.of(level.registryAccess())
				.analyze(drawing, FmabAttachments.ALCHEMIST.getOrCreate(player)).parsed().stages().size();
		if (stages == 0 || stages > slot.stages()) {
			player.sendOverlayMessage(Component.translatable("transmutation.fmab.support_too_small", slot.stages()));
			return;
		}
		if (BodyCircles.circle(player, slot).isPresent()) {
			player.sendOverlayMessage(Component.translatable(surface == BodyCircles.Surface.STEEL
					? "tattoo.fmab.engraved_occupied" : "tattoo.fmab.occupied"));
			return;
		}
		if (!player.isCreative()) {
			ink.shrink(1);
		}
		level.removeBlock(circle, false);
		TransmutationLightning.discharge(level, player.blockPosition(), 1.5, 1);
		if (surface == BodyCircles.Surface.STEEL) {
			// L'acier ne sent rien : la gravure ne fait pas mal.
			BodyPart arm = slot.arm().orElseThrow();
			ItemStack piece = BodyCircles.piece(player, arm).copy();
			piece.set(FmabComponents.AUTOMAIL_ENGRAVINGS, BodyCircles.engravings(piece).with(slot, drawing));
			FmabAttachments.AUTOMAIL.set(player, FmabAttachments.AUTOMAIL.getOrCreate(player).with(arm, piece));
			level.playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1, 1.2f);
			player.sendSystemMessage(Component.translatable("tattoo.fmab.engraved",
					Component.translatable(slot.translationKey())));
			return;
		}
		Tattoos tattoos = FmabAttachments.TATTOOS.getOrCreate(player);
		FmabAttachments.TATTOOS.set(player, tattoos.with(slot, drawing));
		player.hurtServer(level, level.damageSources().magic(), PAIN);
		level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1, 0.6f);
		player.sendSystemMessage(Component.translatable("tattoo.fmab.done", Component.translatable(slot.translationKey())));
	}

	/**
	 * Effacer un tatouage, ou une gravure d'automail : deux encres. Le tatouage fait plus mal encore
	 * qu'à l'encrer ; l'acier, lui, ne sent rien.
	 */
	public static void erase(ServerPlayer player, TattooSlot slot) {
		BodyCircles.Surface surface = BodyCircles.surface(player, slot);
		if (BodyCircles.circle(player, slot).isEmpty()) {
			return;
		}
		ItemStack ink = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (!player.isCreative()) {
			if (!ink.is(FmabItems.ALCHEMICAL_INK) || ink.getCount() < ERASE_INK) {
				player.sendOverlayMessage(Component.translatable("tattoo.fmab.erase_needs_ink", ERASE_INK));
				return;
			}
			ink.shrink(ERASE_INK);
		}
		if (surface == BodyCircles.Surface.STEEL) {
			BodyPart arm = slot.arm().orElseThrow();
			FmabAttachments.AUTOMAIL.set(player, FmabAttachments.AUTOMAIL.getOrCreate(player)
					.with(arm, unengraved(BodyCircles.piece(player, arm), slot)));
			player.sendSystemMessage(Component.translatable("tattoo.fmab.engraving_erased",
					Component.translatable(slot.translationKey())));
			return;
		}
		FmabAttachments.TATTOOS.set(player, FmabAttachments.TATTOOS.getOrCreate(player).without(slot));
		player.hurtServer(player.level(), player.level().damageSources().magic(), ERASE_PAIN);
		player.sendSystemMessage(Component.translatable("tattoo.fmab.erased", Component.translatable(slot.translationKey())));
	}

	/** La pièce sans la gravure de cet emplacement. */
	private static ItemStack unengraved(ItemStack piece, TattooSlot slot) {
		ItemStack out = piece.copy();
		Engravings left = BodyCircles.engravings(out).without(slot);
		if (left.isEmpty()) {
			out.remove(FmabComponents.AUTOMAIL_ENGRAVINGS);
		} else {
			out.set(FmabComponents.AUTOMAIL_ENGRAVINGS, left);
		}
		return out;
	}

	/**
	 * La Vérité vient de prendre des bras : les tatouages qu'ils portaient partent avec eux. Les
	 * gravures, elles, sont dans l'acier des automails, qui ne craint rien.
	 */
	public static void forgetLostArms(ServerPlayer player) {
		GateState gate = FmabAttachments.GATE.get(player);
		Tattoos tattoos = FmabAttachments.TATTOOS.get(player);
		if (gate == null || tattoos == null) {
			return;
		}
		Tattoos kept = tattoos;
		for (TattooSlot slot : TattooSlot.values()) {
			if (slot.arm().filter(gate::lost).isPresent()) {
				kept = kept.without(slot);
			}
		}
		if (!kept.equals(tattoos)) {
			FmabAttachments.TATTOOS.set(player, kept);
		}
	}
}
