package com.ajustor.fmab.network;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.entity.IzumiEntity;
import com.ajustor.fmab.entity.StateExaminerEntity;
import com.ajustor.fmab.entity.TruthEntity;
import com.ajustor.fmab.entity.WinryEntity;
import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.gate.BodyMenu;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.Rebirth;
import com.ajustor.fmab.gate.SoulBinding;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.state.StateExam;
import com.ajustor.fmab.tattoo.TattooRitual;
import com.ajustor.fmab.tattoo.TattooSlot;
import com.ajustor.fmab.training.Trainings;
import com.ajustor.fmab.training.Trial;
import com.ajustor.fmab.transmutation.GloveCasting;
import com.ajustor.fmab.transmutation.Recomposition;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class FmabNetwork {
	private FmabNetwork() {
	}

	public static void register() {
		// Le carnet entier (seize pages) dépasse vite la taille d'un paquet ordinaire.
		PayloadTypeRegistry.serverboundPlay().registerLarge(SaveNotebookPayload.TYPE, SaveNotebookPayload.CODEC,
				1 << 20);
		PayloadTypeRegistry.serverboundPlay().register(CastGlovesPayload.TYPE, CastGlovesPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RemoveGlovePayload.TYPE, RemoveGlovePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(IzumiActionPayload.TYPE, IzumiActionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(WinryActionPayload.TYPE, WinryActionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TruthChoicePayload.TYPE, TruthChoicePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenTruthPayload.TYPE, OpenTruthPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CinematicPayload.TYPE, CinematicPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenWinryPayload.TYPE, OpenWinryPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenIzumiPayload.TYPE, OpenIzumiPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ExamActionPayload.TYPE, ExamActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenExamPayload.TYPE, OpenExamPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TattooPayload.TYPE, TattooPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AddToNotebookPayload.TYPE, AddToNotebookPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SelectCirclePayload.TYPE, SelectCirclePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(OpenBodyPayload.TYPE, OpenBodyPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenTattooPayload.TYPE, OpenTattooPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RequestDesignsPayload.TYPE, RequestDesignsPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ChooseDesignPayload.TYPE, ChooseDesignPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().registerLarge(OpenDesignsPayload.TYPE, OpenDesignsPayload.CODEC, 1 << 20);

		ServerPlayNetworking.registerGlobalReceiver(RequestDesignsPayload.TYPE,
				(payload, context) -> ServerPlayNetworking.send(context.player(), Recomposition.designs(context.player())));
		ServerPlayNetworking.registerGlobalReceiver(ChooseDesignPayload.TYPE,
				(payload, context) -> Recomposition.choose(context.player(), payload.item()));
		ServerPlayNetworking.registerGlobalReceiver(SaveNotebookPayload.TYPE,
				(payload, context) -> Notebooks.set(context.player(), payload.contents()));
		ServerPlayNetworking.registerGlobalReceiver(SelectCirclePayload.TYPE,
				(payload, context) -> selectCircle(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(OpenBodyPayload.TYPE,
				(payload, context) -> BodyMenu.open(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(CastGlovesPayload.TYPE,
				(payload, context) -> GloveCasting.cast(context.player(), payload.combine()));
		ServerPlayNetworking.registerGlobalReceiver(RemoveGlovePayload.TYPE,
				(payload, context) -> removeGlove(context.player(), payload.left()));
		ServerPlayNetworking.registerGlobalReceiver(TruthChoicePayload.TYPE,
				(payload, context) -> truth(context.player(), payload));
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

	/** L'âme errante répond à sa Vérité : il faut que ce soit bien la sienne, et qu'elle erre. */
	private static void truth(ServerPlayer player, TruthChoicePayload payload) {
		if (!(player.level().getEntity(payload.entityId()) instanceof TruthEntity truth)
				|| truth.owner().filter(player.getUUID()::equals).isEmpty() || player.distanceToSqr(truth) > 100
				|| !player.getAttachedOrCreate(FmabAttachments.GATE).adrift()) {
			return;
		}
		if (payload.restart()) {
			Rebirth.restart(player);
		} else {
			SoulBinding.recall(player);
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

	/** Le Traité recopie un cercle dans le carnet. */
	private static void addToNotebook(ServerPlayer player, String name, Drawing drawing) {
		player.sendOverlayMessage(Component.translatable(switch (Notebooks.add(player, name, drawing)) {
			case ADDED -> "treatise.fmab.added";
			case ALREADY_THERE -> "notebook.fmab.already";
			case FULL -> "notebook.fmab.full";
		}));
	}

	/** La roue des cercles : une page devient la sélection, et l'Initié peut joindre aussitôt les mains. */
	private static void selectCircle(ServerPlayer player, SelectCirclePayload payload) {
		if (!Notebooks.select(player, payload.index())) {
			return;
		}
		NotebookContents contents = Notebooks.of(player);
		player.sendOverlayMessage(Component.translatable("circle.fmab.selected",
				Notebooks.pageName(contents.pages().get(contents.selected()))));
		if (payload.clap() && player.getAttachedOrCreate(FmabAttachments.ALCHEMIST).rank().atLeast(Rank.GATE)) {
			GloveCasting.cast(player, true);
		}
	}
}
