package com.ajustor.fmab.item;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import net.minecraft.network.chat.Component;
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
 * Gant d'alchimiste : il porte un cercle, brodé ou gravé à la table d'alchimiste, qu'on lance avec la
 * touche dédiée sans rien tracer. Utiliser un gant l'enfile (main droite d'abord).
 */
public class GloveItem extends Item {
	/**
	 * @param stages    étages que le gant peut porter
	 * @param reach     portée en blocs ; 0 pour la portée de la main
	 * @param striking  le cercle se déclenche aussi en frappant un bloc (gantelets)
	 * @param engraved  le cercle se grave au burin au lieu de se broder au fil
	 */
	public enum Kind {
		CLOTH(1, 0, false, false, Rank.APPRENTICE),
		LEATHER(2, 0, false, false, Rank.APPRENTICE),
		SPARK(2, 24, false, false, Rank.ALCHEMIST),
		GAUNTLET(2, 0, true, true, Rank.ALCHEMIST),
		STATE(5, 0, false, false, Rank.STATE);

		private final int stages;
		private final int reach;
		private final boolean striking;
		private final boolean engraved;
		private final Rank rank;

		Kind(int stages, int reach, boolean striking, boolean engraved, Rank rank) {
			this.stages = stages;
			this.reach = reach;
			this.striking = striking;
			this.engraved = engraved;
			this.rank = rank;
		}

		public int stages() {
			return stages;
		}

		public int reach() {
			return reach;
		}

		public boolean striking() {
			return striking;
		}

		public boolean engraved() {
			return engraved;
		}

		public Rank rank() {
			return rank;
		}
	}

	private final Kind kind;

	public GloveItem(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		AlchemistData me = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		if (!me.rank().atLeast(kind.rank)) {
			player.sendOverlayMessage(Component.translatable("item.fmab.glove.rank_too_low",
					Component.translatable(kind.rank.translationKey())));
			return InteractionResult.FAIL;
		}
		Gloves gloves = player.getAttachedOrCreate(FmabAttachments.GLOVES);
		// Main droite d'abord, puis la gauche ; si les deux sont prises, on échange la droite.
		boolean left = !gloves.right().isEmpty() && gloves.left().isEmpty();
		ItemStack previous = gloves.get(left);
		player.setAttached(FmabAttachments.GLOVES, gloves.with(left, stack.copyWithCount(1)));
		stack.shrink(1);
		if (!previous.isEmpty() && !player.getInventory().add(previous)) {
			player.drop(previous, false);
		}
		level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS,
				1, 1);
		player.sendOverlayMessage(Component.translatable(left ? "item.fmab.glove.equipped_left"
				: "item.fmab.glove.equipped_right"));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.glove.stages", kind.stages));
		tooltip.accept(Component.translatable(stack.has(FmabComponents.GLOVE_CIRCLE)
				? "item.fmab.glove.inscribed" : "item.fmab.glove.blank"));
	}
}
