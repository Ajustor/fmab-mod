package com.ajustor.fmab.item;

import com.ajustor.fmab.progress.Milestones;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Tome;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.transmutation.AlchemyRules;
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
 * Tome d'alchimie : le lire enseigne ses glyphes, une fois. Le savoir passe dans la tête du lecteur et
 * le tome tombe en poussière ; un tome qui n'apprend rien de neuf reste intact, et peut se donner. Un
 * alchimiste peut consigner une part de son savoir dans des notes, pour un autre (voir
 * {@link com.ajustor.fmab.network.ConsignPayload}).
 */
public class TomeItem extends Item {
	public TomeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		Tome tome = player.getItemInHand(hand).get(FmabComponents.TOME);
		if (tome == null) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer server && read(server, tome) && !server.isCreative()) {
			player.getItemInHand(hand).shrink(1);
			server.sendSystemMessage(Component.translatable("tome.fmab.spent").withStyle(ChatFormatting.GRAY));
		}
		return InteractionResult.SUCCESS;
	}

	/** @return vrai si le lecteur a appris quelque chose : le tome a servi */
	private static boolean read(ServerPlayer player, Tome tome) {
		AlchemistData data = FmabAttachments.ALCHEMIST.getOrCreate(player);
		AlchemyRules rules = AlchemyRules.of(player.level().registryAccess());
		int learned = 0;
		for (String id : tome.glyphs()) {
			Glyph glyph = rules.glyph(id).orElse(null);
			if (glyph == null || data.known().contains(id)) {
				continue;
			}
			data = data.learn(id);
			learned++;
			player.sendSystemMessage(Component.translatable("tome.fmab.learned",
					Component.translatable(glyph.nameKey())));
		}
		if (learned > 0 && player instanceof ServerPlayer reader) {
			Milestones.reach(reader, "tome_read");
		}
		if (learned == 0) {
			player.sendOverlayMessage(Component.translatable("tome.fmab.nothing_new"));
			return false;
		}
		FmabAttachments.ALCHEMIST.set(player, data);
		player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1, 1);
		return true;
	}

	@Override
	public Component getName(ItemStack stack) {
		Tome tome = stack.get(FmabComponents.TOME);
		if (tome == null) {
			return super.getName(stack);
		}
		return tome.author().map(a -> Component.translatable(tome.titleKey(), a)).orElseGet(() -> Component.translatable(tome.titleKey()));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		Tome tome = stack.get(FmabComponents.TOME);
		if (tome == null) {
			return;
		}
		tooltip.accept(Component.translatable("tome.fmab.teaches").withStyle(ChatFormatting.GRAY));
		for (String id : tome.glyphs()) {
			tooltip.accept(Component.literal(" • ").append(Component.translatable("glyph." + id.replace(':', '.')))
					.withStyle(ChatFormatting.GRAY));
		}
	}
}
