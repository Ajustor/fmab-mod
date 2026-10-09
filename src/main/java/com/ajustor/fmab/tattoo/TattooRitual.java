package com.ajustor.fmab.tattoo;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.Tattoos;
import com.ajustor.fmab.registry.FmabAttachments;
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
 * alchimique en main, et le rituel fait mal. Effacer un tatouage coûte davantage encore.
 */
public final class TattooRitual {
	private static final float PAIN = 6;
	private static final float ERASE_PAIN = 8;
	private static final int ERASE_INK = 2;
	private static final double REACH = 6;

	private TattooRitual() {
	}

	/** Tatouer sur {@code slot} le cercle inscrit en {@code circle}. */
	public static void tattoo(ServerPlayer player, BlockPos circle, TattooSlot slot) {
		ServerLevel level = player.level();
		ItemStack ink = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (!ink.is(FmabItems.ALCHEMICAL_INK) || player.distanceToSqr(Vec3.atCenterOf(circle)) > REACH * REACH
				|| !(level.getBlockEntity(circle) instanceof TransmutationCircleBlockEntity be)) {
			return;
		}
		Drawing drawing = be.drawing();
		int stages = AlchemyRules.of(level.registryAccess())
				.analyze(drawing, player.getAttachedOrCreate(FmabAttachments.ALCHEMIST)).parsed().stages().size();
		if (stages == 0 || stages > slot.stages()) {
			player.sendOverlayMessage(Component.translatable("transmutation.fmab.support_too_small", slot.stages()));
			return;
		}
		Tattoos tattoos = player.getAttachedOrCreate(FmabAttachments.TATTOOS);
		if (tattoos.get(slot).isPresent()) {
			player.sendOverlayMessage(Component.translatable("tattoo.fmab.occupied"));
			return;
		}
		if (!player.isCreative()) {
			ink.shrink(1);
		}
		player.setAttached(FmabAttachments.TATTOOS, tattoos.with(slot, drawing));
		level.removeBlock(circle, false);
		player.hurtServer(level, level.damageSources().magic(), PAIN);
		TransmutationLightning.discharge(level, player.blockPosition(), 1.5, 1);
		level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1, 0.6f);
		player.sendSystemMessage(Component.translatable("tattoo.fmab.done", Component.translatable(slot.translationKey())));
	}

	/** Effacer un tatouage : deux encres, et une douleur plus vive encore. */
	public static void erase(ServerPlayer player, TattooSlot slot) {
		Tattoos tattoos = player.getAttachedOrCreate(FmabAttachments.TATTOOS);
		if (tattoos.get(slot).isEmpty()) {
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
		player.setAttached(FmabAttachments.TATTOOS, tattoos.without(slot));
		player.hurtServer(player.level(), player.level().damageSources().magic(), ERASE_PAIN);
		player.sendSystemMessage(Component.translatable("tattoo.fmab.erased", Component.translatable(slot.translationKey())));
	}
}
