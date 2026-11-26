package com.ajustor.fmab.tattoo;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.Engravings;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.Tattoos;
import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.item.AutomailItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Ce qui porte le cercle d'un emplacement du corps. La peau se tatoue ; un bras perdu ne porte plus
 * rien, sauf l'automail qui le remplace : on ne le tatoue pas, on le grave au burin. La gravure
 * appartient à la pièce (elle la suit quand on la retire) et disparaît quand la pièce lâche.
 */
public final class BodyCircles {
	/** Ce qu'on trouve à un emplacement. */
	public enum Surface {
		/** La peau : on la tatoue. */
		SKIN,
		/** Un automail en état : on le grave. */
		STEEL,
		/** Un automail qui a lâché : sa gravure est partie, on n'en grave pas un nouveau. */
		BROKEN,
		/** Un bras perdu, sans rien à la place. */
		NONE
	}

	private BodyCircles() {
	}

	/**
	 * @param lost   le bras de l'emplacement a été pris par la Vérité
	 * @param fitted un automail est posé dessus
	 * @param broken cet automail a lâché
	 */
	public static Surface surface(boolean lost, boolean fitted, boolean broken) {
		if (!lost) {
			return Surface.SKIN;
		}
		if (!fitted) {
			return Surface.NONE;
		}
		return broken ? Surface.BROKEN : Surface.STEEL;
	}

	public static Surface surface(Entity entity, TattooSlot slot) {
		Optional<BodyPart> arm = slot.arm();
		GateState gate = FmabAttachments.GATE.get(entity);
		if (arm.isEmpty() || gate == null || !gate.lost(arm.get())) {
			return Surface.SKIN;
		}
		ItemStack piece = piece(entity, arm.get());
		return surface(true, !piece.isEmpty(), !piece.isEmpty() && Automails.broken(piece));
	}

	/** L'automail posé sur ce bras, ou rien. */
	public static ItemStack piece(Entity entity, BodyPart arm) {
		Automail automail = FmabAttachments.AUTOMAIL.get(entity);
		ItemStack stack = automail == null ? ItemStack.EMPTY : automail.get(arm);
		return stack.getItem() instanceof AutomailItem item && item.fits(arm) ? stack : ItemStack.EMPTY;
	}

	/** Les gravures d'une pièce. */
	public static Engravings engravings(ItemStack piece) {
		Engravings engravings = piece.get(FmabComponents.AUTOMAIL_ENGRAVINGS);
		return engravings == null ? Engravings.NONE : engravings;
	}

	/** Tout ce que porte cet emplacement : dessiné dans la peau, ou gravé dans l'automail en état. */
	public static Optional<BodyArt> art(Entity entity, TattooSlot slot) {
		return switch (surface(entity, slot)) {
			case SKIN -> {
				Tattoos tattoos = FmabAttachments.TATTOOS.get(entity);
				yield tattoos == null ? Optional.empty() : tattoos.art(slot);
			}
			case STEEL -> engravings(piece(entity, slot.arm().orElseThrow())).get(slot);
			case BROKEN, NONE -> Optional.empty();
		};
	}

	/** Le cercle alchimique que porte cet emplacement : le tampon de son dessin. */
	public static Optional<Drawing> circle(Entity entity, TattooSlot slot) {
		return art(entity, slot).flatMap(BodyArt::circle);
	}

	/**
	 * Remplace ce que porte l'emplacement (rien : l'efface) : dans la peau, ou dans l'automail en état.
	 * À l'appelant de vérifier la surface.
	 */
	public static void setArt(ServerPlayer player, TattooSlot slot, BodyArt art) {
		if (surface(player, slot) == Surface.STEEL) {
			BodyPart arm = slot.arm().orElseThrow();
			ItemStack piece = piece(player, arm).copy();
			Engravings engraved = art.isEmpty() ? engravings(piece).without(slot) : engravings(piece).with(slot, art);
			if (engraved.isEmpty()) {
				piece.remove(FmabComponents.AUTOMAIL_ENGRAVINGS);
			} else {
				piece.set(FmabComponents.AUTOMAIL_ENGRAVINGS, engraved);
			}
			FmabAttachments.AUTOMAIL.set(player, FmabAttachments.AUTOMAIL.getOrCreate(player).with(arm, piece));
			return;
		}
		Tattoos tattoos = FmabAttachments.TATTOOS.getOrCreate(player);
		FmabAttachments.TATTOOS.set(player, art.isEmpty() ? tattoos.without(slot) : tattoos.withArt(slot, art));
	}
}
