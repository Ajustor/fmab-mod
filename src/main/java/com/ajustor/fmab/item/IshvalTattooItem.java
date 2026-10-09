package com.ajustor.fmab.item;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.registry.FmabAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Les notes du frère de Scar : le motif du tatouage qui unit l'alchimie et l'alkahestry. S'en
 * servir le grave sur le bras droit (voir {@code ScarArm}) ; il faut être au moins Alchimiste pour
 * en comprendre le tracé.
 */
public class IshvalTattooItem extends Item {
	public IshvalTattooItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer p)) {
			return InteractionResult.SUCCESS;
		}
		if (Boolean.TRUE.equals(p.getAttached(FmabAttachments.SCAR_ARM))) {
			p.sendOverlayMessage(Component.translatable("item.fmab.ishval_tattoo.already"));
			return InteractionResult.SUCCESS;
		}
		if (!p.getAttachedOrCreate(FmabAttachments.ALCHEMIST).rank().atLeast(Rank.ALCHEMIST) && !p.isCreative()) {
			p.sendOverlayMessage(Component.translatable("item.fmab.ishval_tattoo.too_soon"));
			return InteractionResult.SUCCESS;
		}
		p.setAttached(FmabAttachments.SCAR_ARM, true);
		p.getItemInHand(hand).consume(1, p);
		p.hurtServer(p.level(), p.level().damageSources().magic(), 4);
		p.level().playSound(null, p.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1, 0.7f);
		p.sendSystemMessage(Component.translatable("item.fmab.ishval_tattoo.engraved").withStyle(ChatFormatting.DARK_RED));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.ishval_tattoo.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
