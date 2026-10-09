package com.ajustor.fmab.gate;

import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.item.AutomailItem;
import com.ajustor.fmab.item.GloveItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * La page du corps, ouverte depuis l'inventaire : les quatre membres (leur automail) et les deux
 * gants autour du joueur, l'inventaire en dessous. On y branche un automail sur un membre perdu
 * (comme à l'établi) et on y enfile ou ôte ses gants ; le retrait d'un automail reste l'affaire de
 * Winry.
 */
public class BodyMenu extends AbstractContainerMenu {
	/** Emplacements du corps : bras droit, bras gauche, jambe droite, jambe gauche, gant droit, gant gauche. */
	public static final int SLOTS = 6;
	public static final int RIGHT_GLOVE = 4;
	public static final int LEFT_GLOVE = 5;
	private static final BodyPart[] LIMBS = {BodyPart.RIGHT_ARM, BodyPart.LEFT_ARM, BodyPart.RIGHT_LEG,
			BodyPart.LEFT_LEG};
	/** Position des emplacements dans la page, dans l'ordre ci-dessus (vue de face : la droite à gauche). */
	private static final int[][] POSITIONS = {{43, 15}, {117, 15}, {43, 41}, {117, 41}, {21, 15}, {139, 15}};
	private static final int INVENTORY = SLOTS;
	private static final int HOTBAR = SLOTS + 27;
	private static final int END = SLOTS + 36;

	private final Player player;

	/** Côté client : les emplacements se remplissent de ce que le serveur envoie. */
	public BodyMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(SLOTS));
	}

	public BodyMenu(int containerId, Inventory inventory, Container body) {
		super(FmabMenus.BODY, containerId);
		this.player = inventory.player;
		for (int i = 0; i < SLOTS; i++) {
			addSlot(limb(i) != null ? new LimbSlot(body, i) : new GloveSlot(body, i));
		}
		addStandardInventorySlots(inventory, 8, 84);
	}

	/** Ouvre la page du corps de ce joueur. */
	public static void open(ServerPlayer player) {
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, p) -> new BodyMenu(id, inventory, new BodyContainer(player)),
				Component.translatable("body_page.fmab.title")));
	}

	/** Le membre de cet emplacement, ou null pour un gant. */
	public static @Nullable BodyPart limb(int index) {
		return index >= 0 && index < LIMBS.length ? LIMBS[index] : null;
	}

	public static boolean leftGlove(int index) {
		return index == LEFT_GLOVE;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem() || !slot.mayPickup(player)) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		boolean moved;
		if (index < SLOTS) {
			moved = moveItemStackTo(stack, INVENTORY, END, true);
		} else if (stack.getItem() instanceof AutomailItem) {
			moved = moveItemStackTo(stack, 0, LIMBS.length, false);
		} else if (stack.getItem() instanceof GloveItem) {
			moved = moveItemStackTo(stack, RIGHT_GLOVE, SLOTS, false);
		} else if (index < HOTBAR) {
			moved = moveItemStackTo(stack, HOTBAR, END, false);
		} else {
			moved = moveItemStackTo(stack, INVENTORY, HOTBAR, false);
		}
		if (!moved) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return player.isAlive();
	}

	/** Un membre : on y pose un automail s'il est perdu ; on ne l'en retire pas ici. */
	private final class LimbSlot extends Slot {
		private final BodyPart part;

		LimbSlot(Container container, int index) {
			super(container, index, POSITIONS[index][0], POSITIONS[index][1]);
			this.part = LIMBS[index];
		}

		public BodyPart part() {
			return part;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return !hasItem() && Automails.canFit(player, part, stack);
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	}

	/** Une main : le gant qu'elle porte, si le rang de l'alchimiste le permet. */
	private final class GloveSlot extends Slot {
		GloveSlot(Container container, int index) {
			super(container, index, POSITIONS[index][0], POSITIONS[index][1]);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			if (!(stack.getItem() instanceof GloveItem glove)) {
				return false;
			}
			AlchemistData me = player.getAttached(FmabAttachments.ALCHEMIST);
			return me != null && me.rank().atLeast(glove.kind().rank());
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	}
}
