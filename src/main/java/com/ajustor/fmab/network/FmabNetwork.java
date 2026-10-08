package com.ajustor.fmab.network;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.entity.IzumiEntity;
import com.ajustor.fmab.entity.WinryEntity;
import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.entity.StateExaminerEntity;
import com.ajustor.fmab.state.StateExam;
import com.ajustor.fmab.tattoo.TattooRitual;
import com.ajustor.fmab.tattoo.TattooSlot;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.training.Trainings;
import com.ajustor.fmab.training.Trial;
import com.ajustor.fmab.transmutation.GloveCasting;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class FmabNetwork {
	private FmabNetwork() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(SaveNotebookPayload.TYPE, SaveNotebookPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CastGlovesPayload.TYPE, CastGlovesPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RemoveGlovePayload.TYPE, RemoveGlovePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(IzumiActionPayload.TYPE, IzumiActionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(WinryActionPayload.TYPE, WinryActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenWinryPayload.TYPE, OpenWinryPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenIzumiPayload.TYPE, OpenIzumiPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ExamActionPayload.TYPE, ExamActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenExamPayload.TYPE, OpenExamPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TattooPayload.TYPE, TattooPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AddToNotebookPayload.TYPE, AddToNotebookPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenTattooPayload.TYPE, OpenTattooPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SaveNotebookPayload.TYPE,
				(payload, context) -> saveNotebook(context.player(), payload.hand(), payload.contents()));
		ServerPlayNetworking.registerGlobalReceiver(CastGlovesPayload.TYPE,
				(payload, context) -> GloveCasting.cast(context.player(), payload.combine()));
		ServerPlayNetworking.registerGlobalReceiver(RemoveGlovePayload.TYPE,
				(payload, context) -> removeGlove(context.player(), payload.left()));
		ServerPlayNetworking.registerGlobalReceiver(WinryActionPayload.TYPE,
				(payload, context) -> winry(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(IzumiActionPayload.TYPE,
				(payload, context) -> izumi(context.player(), payload.entityId(), payload.action()));
		ServerPlayNetworking.registerGlobalReceiver(ExamActionPayload.TYPE,
				(payload, context) -> exam(context.player(), payload.entityId(), payload.action()));
		ServerPlayNetworking.registerGlobalReceiver(AddToNotebookPayload.TYPE,
				(payload, context) -> addToNotebook(context.player(), payload.name(), payload.drawing()));
		ServerPlayNetworking.registerGlobalReceiver(TattooPayload.TYPE, (payload, context) ->
				TattooSlot.byId(payload.slot()).ifPresent(slot -> {
					if (payload.erase()) {
						TattooRitual.erase(context.player(), slot);
					} else {
						TattooRitual.tattoo(context.player(), payload.circle(), slot);
					}
				}));
	}

	/** Le candidat doit être face à l'examinateur. */
	private static void exam(ServerPlayer player, int entityId, String action) {
		if (!(player.level().getEntity(entityId) instanceof StateExaminerEntity examiner)
				|| player.distanceToSqr(examiner) > 64) {
			return;
		}
		switch (action) {
			case ExamActionPayload.GIVE -> {
				if (StateExam.giveItems(player)) {
					ServerPlayNetworking.send(player, OpenExamPayload.of(examiner, player));
				}
			}
			case ExamActionPayload.FIGHT -> StateExam.startFight(examiner, player);
			default -> {
			}
		}
	}

	/** Winry répare ou retire un automail ; il faut être dans son atelier. */
	private static void winry(ServerPlayer player, WinryActionPayload payload) {
		if (!(player.level().getEntity(payload.entityId()) instanceof WinryEntity winry)
				|| player.distanceToSqr(winry) > 64) {
			return;
		}
		BodyPart part;
		try {
			part = BodyPart.fromSerializedName(payload.part());
		} catch (IllegalArgumentException unknown) {
			return;
		}
		if (!part.limb()) {
			return;
		}
		if (payload.repair()) {
			Automails.repair(player, part);
		} else {
			Automails.remove(player, part);
		}
	}

	/** Le joueur doit être à portée de voix d'Izumi. */
	private static void izumi(ServerPlayer player, int entityId, String action) {
		if (!(player.level().getEntity(entityId) instanceof IzumiEntity izumi) || player.distanceToSqr(izumi) > 64) {
			return;
		}
		if (action.equals(IzumiActionPayload.SPAR)) {
			izumi.startSpar(player);
			return;
		}
		try {
			if (Trainings.claim(player, Trial.byId(action))) {
				ServerPlayNetworking.send(player, OpenIzumiPayload.of(izumi, player));
			}
		} catch (IllegalArgumentException unknownTrial) {
			// Rien à rendre : action inconnue.
		}
	}

	private static void removeGlove(ServerPlayer player, boolean left) {
		Gloves gloves = player.getAttachedOrCreate(FmabAttachments.GLOVES);
		ItemStack glove = gloves.get(left);
		if (glove.isEmpty()) {
			return;
		}
		player.setAttached(FmabAttachments.GLOVES, gloves.with(left, ItemStack.EMPTY));
		if (!player.getInventory().add(glove)) {
			player.drop(glove, false);
		}
	}

	/** Le carnet de la main secondaire, sinon le premier de l'inventaire, reçoit une page de plus. */
	private static void addToNotebook(ServerPlayer player, String name, Drawing drawing) {
		ItemStack notebook = player.getOffhandItem().is(FmabItems.CIRCLE_NOTEBOOK) ? player.getOffhandItem() : null;
		if (notebook == null) {
			for (ItemStack stack : player.getInventory()) {
				if (stack.is(FmabItems.CIRCLE_NOTEBOOK)) {
					notebook = stack;
					break;
				}
			}
		}
		if (notebook == null) {
			player.sendOverlayMessage(Component.translatable("treatise.fmab.no_notebook"));
			return;
		}
		NotebookContents contents = notebook.getOrDefault(FmabComponents.NOTEBOOK, NotebookContents.EMPTY);
		if (contents.pages().size() >= NotebookContents.MAX_PAGES) {
			player.sendOverlayMessage(Component.translatable("notebook.fmab.full"));
			return;
		}
		String page = name.length() > NotebookContents.MAX_NAME_LENGTH ? name.substring(0, NotebookContents.MAX_NAME_LENGTH)
				: name;
		notebook.set(FmabComponents.NOTEBOOK,
				contents.withPage(contents.pages().size(), new NotebookContents.Page(page, drawing)));
		player.sendOverlayMessage(Component.translatable("treatise.fmab.added"));
	}

	private static void saveNotebook(ServerPlayer player, InteractionHand hand, NotebookContents contents) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(FmabItems.CIRCLE_NOTEBOOK)) {
			stack.set(FmabComponents.NOTEBOOK, contents);
		}
	}
}
