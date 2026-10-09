package com.ajustor.fmab.gate;

import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.registry.FmabAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Les emplacements de la page du corps, côté serveur : ils lisent et écrivent directement les
 * automails et les gants du joueur. Un automail se branche en le posant, mais ne se retire que chez
 * Winry ; un gant s'enfile et s'ôte librement.
 */
public final class BodyContainer implements Container {
	private final ServerPlayer player;

	public BodyContainer(ServerPlayer player) {
		this.player = player;
	}

	@Override
	public int getContainerSize() {
		return BodyMenu.SLOTS;
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < BodyMenu.SLOTS; i++) {
			if (!getItem(i).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int index) {
		BodyPart limb = BodyMenu.limb(index);
		if (limb != null) {
			return player.getAttachedOrCreate(FmabAttachments.AUTOMAIL).get(limb);
		}
		return player.getAttachedOrCreate(FmabAttachments.GLOVES).get(BodyMenu.leftGlove(index));
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		return count > 0 ? removeItemNoUpdate(index) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		if (BodyMenu.limb(index) != null) {
			// L'automail reste branché : seule Winry le retire.
			return ItemStack.EMPTY;
		}
		ItemStack glove = getItem(index);
		if (!glove.isEmpty()) {
			setGlove(index, ItemStack.EMPTY);
		}
		return glove;
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		BodyPart limb = BodyMenu.limb(index);
		if (limb == null) {
			setGlove(index, stack);
			return;
		}
		Automail automail = player.getAttachedOrCreate(FmabAttachments.AUTOMAIL);
		if (!stack.isEmpty() && automail.get(limb).isEmpty()) {
			Automails.fitTo(player, limb, stack.copyWithCount(1));
		}
	}

	private void setGlove(int index, ItemStack stack) {
		Gloves gloves = player.getAttachedOrCreate(FmabAttachments.GLOVES);
		boolean left = BodyMenu.leftGlove(index);
		player.setAttached(FmabAttachments.GLOVES, gloves.with(left, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1)));
		if (!stack.isEmpty()) {
			player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(),
					SoundSource.PLAYERS, 1, 1);
		}
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return player == this.player && player.isAlive();
	}

	@Override
	public void clearContent() {
	}
}
