package com.ajustor.fmab.tattoo;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.data.Tattoos;
import com.ajustor.fmab.network.FmabNetwork;
import com.ajustor.fmab.network.OpenTattooEditorPayload;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Le rituel du tatouage : le cercle tracé au sol passe dans la peau. Il faut de l'encre
 * alchimique en main, et le rituel fait mal. Effacer un tatouage coûte davantage encore. Sur un bras
 * d'automail, on ne tatoue pas : le burin d'alchimiste grave le cercle dans l'acier
 * ({@link BodyCircles}).
 */
public final class TattooRitual {
	private static final float PAIN = 6;
	private static final float ERASE_PAIN = 8;
	/** Un dessin sans cercle nouveau : l'aiguille pique, sans plus. */
	private static final float ART_PAIN = 2;
	private static final int ERASE_INK = 2;
	/** L'usure du burin pour graver ou effacer une gravure : celle d'un gantelet gravé à la table. */
	private static final int CHISEL_WEAR = 4;
	private static final double REACH = 6;

	private TattooRitual() {
	}

	/** L'outil qui inscrit un cercle sur cette surface : l'encre pour la peau, le burin pour l'acier. */
	public static boolean rightTool(ItemStack held, BodyCircles.Surface surface) {
		return switch (surface) {
			case SKIN -> held.is(FmabItems.ALCHEMICAL_INK);
			case STEEL -> held.is(FmabItems.ALCHEMIST_CHISEL);
			case BROKEN, NONE -> false;
		};
	}

	/**
	 * Inscrire sur {@code slot} le cercle tracé en {@code circle} : tatoué dans la peau à l'encre, ou
	 * gravé au burin dans l'automail qui remplace un bras perdu. Un bras perdu sans automail, ou dont
	 * l'automail a lâché, ne reçoit rien.
	 */
	public static void tattoo(ServerPlayer player, BlockPos circle, TattooSlot slot) {
		ServerLevel level = player.level();
		ItemStack tool = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (player.distanceToSqr(Vec3.atCenterOf(circle)) > REACH * REACH
				|| !(level.getBlockEntity(circle) instanceof TransmutationCircleBlockEntity be)) {
			return;
		}
		BodyCircles.Surface surface = BodyCircles.surface(player, slot);
		if (surface == BodyCircles.Surface.NONE || surface == BodyCircles.Surface.BROKEN) {
			player.sendOverlayMessage(Component.translatable(surface == BodyCircles.Surface.NONE
					? "tattoo.fmab.no_limb" : "tattoo.fmab.automail_broken"));
			return;
		}
		if (!rightTool(tool, surface)) {
			player.sendOverlayMessage(Component.translatable(surface == BodyCircles.Surface.STEEL
					? "tattoo.fmab.needs_chisel" : "tattoo.fmab.needs_ink"));
			return;
		}
		Drawing drawing = be.drawing();
		int stages = AlchemyRules.of(level.registryAccess())
				.analyze(drawing, FmabAttachments.ALCHEMIST.getOrCreate(player)).parsed().stages().size();
		if (stages == 0 || stages > slot.stages()) {
			player.sendOverlayMessage(Component.translatable("transmutation.fmab.support_too_small", slot.stages()));
			return;
		}
		if (BodyCircles.full(player, slot)) {
			player.sendOverlayMessage(Component.translatable(surface == BodyCircles.Surface.STEEL
					? "tattoo.fmab.engraved_occupied" : "tattoo.fmab.occupied"));
			return;
		}
		spend(player, tool, surface);
		level.removeBlock(circle, false);
		TransmutationLightning.discharge(level, player.blockPosition(), 1.5, 1);
		// Le cercle se pose en tampon par-dessus les dessins de l'emplacement : toute la page, ou une
		// face libre du tour du bras.
		boolean steel = surface == BodyCircles.Surface.STEEL;
		BodyCircles.setArt(player, slot, slot.stamp(BodyCircles.art(player, slot).orElse(BodyArt.EMPTY), drawing,
				steel ? BodyArt.STEEL_INK : BodyArt.SKIN_INK));
		if (steel) {
			// L'acier ne sent rien : la gravure ne fait pas mal.
			level.playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1, 1.2f);
			player.sendSystemMessage(Component.translatable("tattoo.fmab.engraved",
					Component.translatable(slot.translationKey())));
			return;
		}
		player.hurtServer(level, level.damageSources().magic(), PAIN);
		level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1, 0.6f);
		player.sendSystemMessage(Component.translatable("tattoo.fmab.done", Component.translatable(slot.translationKey())));
	}

	/** L'outil paie : une encre se vide, le burin s'use. */
	private static void spend(ServerPlayer player, ItemStack tool, BodyCircles.Surface surface) {
		if (player.isCreative()) {
			return;
		}
		if (surface == BodyCircles.Surface.STEEL) {
			tool.hurtAndBreak(CHISEL_WEAR, player, InteractionHand.MAIN_HAND);
		} else {
			tool.shrink(1);
		}
	}

	/**
	 * Effacer ce que porte un emplacement : un tatouage (deux encres, et plus mal encore qu'à l'encrer),
	 * ou une gravure d'automail (le burin la lime ; l'acier ne sent rien).
	 */
	public static void erase(ServerPlayer player, TattooSlot slot) {
		BodyCircles.Surface surface = BodyCircles.surface(player, slot);
		if (BodyCircles.art(player, slot).isEmpty()) {
			return;
		}
		ItemStack tool = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (surface == BodyCircles.Surface.STEEL) {
			if (!player.isCreative()) {
				if (!rightTool(tool, surface)) {
					player.sendOverlayMessage(Component.translatable("tattoo.fmab.needs_chisel"));
					return;
				}
				tool.hurtAndBreak(CHISEL_WEAR, player, InteractionHand.MAIN_HAND);
			}
			BodyCircles.setArt(player, slot, BodyArt.EMPTY);
			player.sendSystemMessage(Component.translatable("tattoo.fmab.engraving_erased",
					Component.translatable(slot.translationKey())));
			return;
		}
		if (!player.isCreative()) {
			if (!tool.is(FmabItems.ALCHEMICAL_INK) || tool.getCount() < ERASE_INK) {
				player.sendOverlayMessage(Component.translatable("tattoo.fmab.erase_needs_ink", ERASE_INK));
				return;
			}
			tool.shrink(ERASE_INK);
		}
		BodyCircles.setArt(player, slot, BodyArt.EMPTY);
		player.hurtServer(player.level(), player.level().damageSources().magic(), ERASE_PAIN);
		player.sendSystemMessage(Component.translatable("tattoo.fmab.erased", Component.translatable(slot.translationKey())));
	}

	/**
	 * Un clic droit dans le vide, l'encre ou le burin en main : l'éditeur de dessins s'ouvre, s'il y a
	 * de quoi dessiner (de la peau pour l'encre, un bras d'automail en état pour le burin). Renvoie vrai
	 * si le clic est pris : visé sur un bloc à portée, il ne l'est pas.
	 */
	public static boolean openEditor(Player player, boolean engrave) {
		if (player.pick(player.blockInteractionRange(), 1, false).getType() == HitResult.Type.BLOCK) {
			return false;
		}
		if (!(player instanceof ServerPlayer server)) {
			return true;
		}
		BodyCircles.Surface wanted = engrave ? BodyCircles.Surface.STEEL : BodyCircles.Surface.SKIN;
		boolean any = false;
		for (TattooSlot slot : TattooSlot.values()) {
			any |= BodyCircles.surface(server, slot) == wanted;
		}
		if (!any) {
			server.sendOverlayMessage(Component.translatable("tattoo.fmab.nothing_to_engrave"));
			return true;
		}
		FmabNetwork.send(server, new OpenTattooEditorPayload(engrave));
		return true;
	}

	/** Pourquoi le dessin envoyé par l'éditeur est refusé, ou {@code null} s'il est accepté. */
	public enum Refusal {
		WRONG_SURFACE, WRONG_TOOL, INVALID, NOT_IN_NOTEBOOK, TOO_SMALL
	}

	/**
	 * Le dessin de l'éditeur passe dans la peau (à l'encre) ou dans l'acier (au burin). Il remplace ce
	 * que l'emplacement portait ; vide, il l'efface (au prix d'un effacement). Un tampon nouveau doit
	 * venir du carnet du joueur et tenir sur l'emplacement, comme un cercle tatoué au sol. Une encre
	 * ou quatre points de burin par passage ; la peau a mal, plus fort quand un cercle y entre.
	 */
	public static void applyArt(ServerPlayer player, TattooSlot slot, BodyArt art) {
		BodyCircles.Surface surface = BodyCircles.surface(player, slot);
		BodyArt before = BodyCircles.art(player, slot).orElse(BodyArt.EMPTY);
		if (art.equals(before)) {
			return;
		}
		if (art.isEmpty()) {
			erase(player, slot);
			return;
		}
		ItemStack tool = player.getItemInHand(InteractionHand.MAIN_HAND);
		Refusal refusal = check(player, slot, surface, tool, art, before);
		if (refusal != null) {
			player.sendOverlayMessage(Component.translatable(switch (refusal) {
				case WRONG_SURFACE -> surface == BodyCircles.Surface.BROKEN ? "tattoo.fmab.automail_broken"
						: "tattoo.fmab.no_limb";
				case WRONG_TOOL -> surface == BodyCircles.Surface.STEEL ? "tattoo.fmab.needs_chisel"
						: "tattoo.fmab.needs_ink";
				case INVALID -> "tattoo.fmab.art_invalid";
				case NOT_IN_NOTEBOOK -> "tattoo.fmab.stamp_not_in_notebook";
				case TOO_SMALL -> "tattoo.fmab.stamp_too_big";
			}, slot.stages()));
			return;
		}
		spend(player, tool, surface);
		BodyCircles.setArt(player, slot, art);
		ServerLevel level = player.level();
		boolean newCircle = !added(art, before).isEmpty();
		if (surface == BodyCircles.Surface.STEEL) {
			level.playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1, 1.2f);
			player.sendSystemMessage(Component.translatable("tattoo.fmab.art_engraved",
					Component.translatable(slot.translationKey())));
			return;
		}
		player.hurtServer(level, level.damageSources().magic(), newCircle ? PAIN : ART_PAIN);
		level.playSound(null, player.blockPosition(), newCircle ? SoundEvents.ILLUSIONER_CAST_SPELL
				: SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1, 0.6f);
		player.sendSystemMessage(Component.translatable("tattoo.fmab.art_tattooed",
				Component.translatable(slot.translationKey())));
	}

	private static Refusal check(ServerPlayer player, TattooSlot slot, BodyCircles.Surface surface, ItemStack tool,
			BodyArt art, BodyArt before) {
		if (surface != BodyCircles.Surface.SKIN && surface != BodyCircles.Surface.STEEL) {
			return Refusal.WRONG_SURFACE;
		}
		if (!rightTool(tool, surface)) {
			return Refusal.WRONG_TOOL;
		}
		if (!art.valid(slot.canvas()) || art.stamps().size() > slot.maxStamps()) {
			return Refusal.INVALID;
		}
		// Un cercle déjà porté peut rester, même effacé du carnet depuis ; un nouveau vient du carnet.
		for (Drawing circle : added(art, before)) {
			if (!Notebooks.contains(player, circle)) {
				return Refusal.NOT_IN_NOTEBOOK;
			}
			int stages = AlchemyRules.of(player.level().registryAccess())
					.analyze(circle, FmabAttachments.ALCHEMIST.getOrCreate(player)).parsed().stages().size();
			if (stages == 0 || stages > slot.stages()) {
				return Refusal.TOO_SMALL;
			}
		}
		return null;
	}

	/** Les cercles du nouveau dessin que l'emplacement ne portait pas encore. */
	static List<Drawing> added(BodyArt art, BodyArt before) {
		List<Drawing> left = new ArrayList<>(before.circles());
		List<Drawing> out = new ArrayList<>();
		for (Drawing circle : art.circles()) {
			if (!left.remove(circle)) {
				out.add(circle);
			}
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
