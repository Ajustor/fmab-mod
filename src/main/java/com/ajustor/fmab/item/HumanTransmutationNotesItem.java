package com.ajustor.fmab.item;

import com.ajustor.fmab.alchemy.ForbiddenCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.registry.FmabAttachments;
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
 * Des notes de recherche interdites sur la transmutation humaine, comme celles que les Elric ont
 * trouvées chez leur père : elles enseignent le glyphe Humain et donnent le cercle, trait pour
 * trait. Très rares, on ne les trouve que dans des archives oubliées. Elles ne se consument pas.
 */
public class HumanTransmutationNotesItem extends Item {
	private static final String HUMAN_GLYPH = "fmab:humain";

	public HumanTransmutationNotesItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server) {
			read(server);
		}
		return InteractionResult.SUCCESS;
	}

	private static void read(ServerPlayer player) {
		boolean news = false;
		AlchemistData data = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		Glyph glyph = AlchemyRules.of(player.level().registryAccess()).glyph(HUMAN_GLYPH).orElse(null);
		if (glyph != null && !data.known().contains(HUMAN_GLYPH)) {
			player.setAttached(FmabAttachments.ALCHEMIST, data.learn(HUMAN_GLYPH));
			player.sendSystemMessage(Component.translatable("tome.fmab.learned", Component.translatable(glyph.nameKey())));
			news = true;
		}
		String page = "@effect." + ForbiddenCircles.HUMAN_TRANSMUTATION_EFFECT.replace(':', '.');
		switch (Notebooks.add(player, page, ForbiddenCircles.HUMAN_TRANSMUTATION)) {
			case ADDED -> {
				player.sendSystemMessage(Component.translatable("item.fmab.human_transmutation_notes.copied")
						.withStyle(ChatFormatting.DARK_RED));
				news = true;
			}
			case FULL -> player.sendOverlayMessage(Component.translatable("notebook.fmab.full"));
			case ALREADY_THERE -> {
			}
		}
		if (!news) {
			player.sendOverlayMessage(Component.translatable("tome.fmab.nothing_new"));
			return;
		}
		player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1, 0.7f);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.human_transmutation_notes.tooltip")
				.withStyle(ChatFormatting.GRAY));
	}
}
