package com.ajustor.fmab.network;

import com.ajustor.fmab.progress.Milestones;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.entity.IzumiEntity;
import com.ajustor.fmab.entity.StateExaminerEntity;
import com.ajustor.fmab.entity.TruthEntity;
import com.ajustor.fmab.entity.WinryEntity;
import com.ajustor.fmab.gate.StoneBargain;
import com.ajustor.fmab.gate.SoulGifts;
import com.ajustor.fmab.gate.SoulOffering;
import com.ajustor.fmab.gate.StoneChoice;
import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.gate.BodyMenu;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.Rebirth;
import com.ajustor.fmab.gate.Shells;
import com.ajustor.fmab.gate.SoulBinding;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.state.StateExam;
import com.ajustor.fmab.tattoo.TattooRitual;
import com.ajustor.fmab.tattoo.TattooSlot;
import com.ajustor.fmab.training.Trainings;
import com.ajustor.fmab.training.Trial;
import com.ajustor.fmab.transmutation.GloveCasting;
import com.ajustor.fmab.transmutation.Recomposition;
import com.ajustor.fmab.util.Throttle;
import com.ajustor.fmab.platform.Platform;
import com.ajustor.fmab.platform.network.Network;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.ajustor.fmab.compat.EntityCompat;

public final class FmabNetwork {
	/** La page de conception parcourt tout ce que le joueur a compris : une par seconde suffit. */
	private static final Throttle DESIGNS = new Throttle(20);

	private FmabNetwork() {
	}

	/** Envoie un paquet du mod à un joueur. */
	public static void send(ServerPlayer player, CustomPacketPayload payload) {
		Platform.get().network().send(player, payload);
	}

	public static void register() {
		Network network = Platform.get().network();
		// Le carnet entier (seize pages) dépasse vite la taille d'un paquet ordinaire.
		network.serverbound(SaveNotebookPayload.TYPE, SaveNotebookPayload.CODEC,
				1 << 20);
		network.serverbound(CastGlovesPayload.TYPE, CastGlovesPayload.CODEC);
		network.serverbound(RemoveGlovePayload.TYPE, RemoveGlovePayload.CODEC);
		network.serverbound(ConsignPayload.TYPE, ConsignPayload.CODEC);
		network.serverbound(ShellKeyPayload.TYPE, ShellKeyPayload.CODEC);
		network.serverbound(IzumiActionPayload.TYPE, IzumiActionPayload.CODEC);
		network.serverbound(WinryActionPayload.TYPE, WinryActionPayload.CODEC);
		network.serverbound(TruthChoicePayload.TYPE, TruthChoicePayload.CODEC);
		network.clientbound(OpenTruthPayload.TYPE, OpenTruthPayload.CODEC);
		network.clientbound(StoneChoicePayload.TYPE, StoneChoicePayload.CODEC);
		network.serverbound(StoneChosenPayload.TYPE, StoneChosenPayload.CODEC);
		network.clientbound(StoneRadiusPayload.TYPE, StoneRadiusPayload.CODEC);
		network.serverbound(StoneRadiusChosenPayload.TYPE, StoneRadiusChosenPayload.CODEC);
		network.clientbound(SoulOfferPayload.TYPE, SoulOfferPayload.CODEC);
		network.serverbound(SoulOfferAnswerPayload.TYPE, SoulOfferAnswerPayload.CODEC);
		network.clientbound(SoulGiftPayload.TYPE, SoulGiftPayload.CODEC);
		network.serverbound(SoulGiftAnswerPayload.TYPE, SoulGiftAnswerPayload.CODEC);
		network.clientbound(BargainPayload.TYPE, BargainPayload.CODEC);
		network.serverbound(BargainAnswerPayload.TYPE, BargainAnswerPayload.CODEC);
		network.clientbound(CinematicPayload.TYPE, CinematicPayload.CODEC);
		network.clientbound(OpenWinryPayload.TYPE, OpenWinryPayload.CODEC);
		network.clientbound(OpenIzumiPayload.TYPE, OpenIzumiPayload.CODEC);
		network.serverbound(ExamActionPayload.TYPE, ExamActionPayload.CODEC);
		network.clientbound(OpenExamPayload.TYPE, OpenExamPayload.CODEC);
		network.serverbound(TattooPayload.TYPE, TattooPayload.CODEC);
		network.serverbound(AddToNotebookPayload.TYPE, AddToNotebookPayload.CODEC);
		network.serverbound(SelectCirclePayload.TYPE, SelectCirclePayload.CODEC);
		network.serverbound(OpenBodyPayload.TYPE, OpenBodyPayload.CODEC);
		network.clientbound(OpenTattooPayload.TYPE, OpenTattooPayload.CODEC);
		network.serverbound(RequestDesignsPayload.TYPE, RequestDesignsPayload.CODEC);
		network.serverbound(ChooseDesignPayload.TYPE, ChooseDesignPayload.CODEC);
		network.clientbound(OpenDesignsPayload.TYPE, OpenDesignsPayload.CODEC, 1 << 20);

		network.receive(RequestDesignsPayload.TYPE,
				(payload, player) -> designs(player));
		network.receive(ChooseDesignPayload.TYPE,
				(payload, player) -> Recomposition.choose(player, payload.item()));
		network.receive(SaveNotebookPayload.TYPE,
				(payload, player) -> {
					if (Notebooks.drewNew(Notebooks.of(player), payload.contents())) {
						Milestones.reach(player, "notebook_circle");
					}
					Notebooks.set(player, payload.contents());
				});
		network.receive(SelectCirclePayload.TYPE,
				(payload, player) -> selectCircle(player, payload));
		network.receive(OpenBodyPayload.TYPE,
				(payload, player) -> BodyMenu.open(player));
		network.receive(CastGlovesPayload.TYPE,
				(payload, player) -> GloveCasting.cast(player, payload.combine()));
		network.receive(RemoveGlovePayload.TYPE,
				(payload, player) -> removeGlove(player, payload.left()));
		network.receive(ConsignPayload.TYPE,
				(payload, player) -> com.ajustor.fmab.item.Notes.write(player, payload.glyphs()));
		network.receive(ShellKeyPayload.TYPE,
				(payload, player) -> {
					if (Shells.KEY.pass(player.getUUID(), player.level().getGameTime())) {
						Shells.press(player);
					}
				});
		network.receive(SoulOfferAnswerPayload.TYPE, (payload, player) -> SoulOffering.answer(player, payload));
		network.receive(SoulGiftAnswerPayload.TYPE, (payload, player) -> SoulGifts.answer(player, payload));
		network.receive(StoneChosenPayload.TYPE,
				(payload, player) -> StoneChoice.answer(player, payload.choice()));
		network.receive(StoneRadiusChosenPayload.TYPE,
				(payload, player) -> com.ajustor.fmab.stone.StoneCircle.answer(player, payload.radius()));
		network.receive(BargainAnswerPayload.TYPE,
				(payload, player) -> StoneBargain.answer(player, payload.part()));
		network.receive(TruthChoicePayload.TYPE,
				(payload, player) -> truth(player, payload));
		network.receive(WinryActionPayload.TYPE,
				(payload, player) -> winry(player, payload));
		network.receive(IzumiActionPayload.TYPE,
				(payload, player) -> izumi(player, payload.entityId(), payload.action()));
		network.receive(ExamActionPayload.TYPE,
				(payload, player) -> exam(player, payload.entityId(), payload.action()));
		network.receive(AddToNotebookPayload.TYPE,
				(payload, player) -> addToNotebook(player, payload.name(), payload.drawing()));
		network.receive(TattooPayload.TYPE, (payload, player) ->
				TattooSlot.byId(payload.slot()).ifPresent(slot -> {
					if (payload.erase()) {
						TattooRitual.erase(player, slot);
					} else {
						TattooRitual.tattoo(player, payload.circle(), slot);
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
					FmabNetwork.send(player, OpenExamPayload.of(examiner, player));
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
				|| !FmabAttachments.GATE.getOrCreate(player).adrift()) {
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
				FmabNetwork.send(player, OpenIzumiPayload.of(izumi, player));
			}
		} catch (IllegalArgumentException unknownTrial) {
			// Rien à rendre : action inconnue.
		}
	}

	private static void removeGlove(ServerPlayer player, boolean left) {
		Gloves gloves = FmabAttachments.GLOVES.getOrCreate(player);
		ItemStack glove = gloves.get(left);
		if (glove.isEmpty()) {
			return;
		}
		FmabAttachments.GLOVES.set(player, gloves.with(left, ItemStack.EMPTY));
		if (!player.getInventory().add(glove)) {
			EntityCompat.drop(player, glove);
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

	private static void designs(ServerPlayer player) {
		if (DESIGNS.pass(player.getUUID(), player.level().getGameTime())) {
			FmabNetwork.send(player, Recomposition.designs(player));
		}
	}

	/** La roue des cercles : une page devient la sélection, et l'Initié peut joindre aussitôt les mains. */
	private static void selectCircle(ServerPlayer player, SelectCirclePayload payload) {
		if (!Notebooks.select(player, payload.index())) {
			return;
		}
		NotebookContents contents = Notebooks.of(player);
		player.sendOverlayMessage(Component.translatable("circle.fmab.selected",
				Notebooks.pageName(contents.pages().get(contents.selected()))));
		if (payload.clap() && (FmabAttachments.ALCHEMIST.getOrCreate(player).rank().atLeast(Rank.GATE)
				|| com.ajustor.fmab.stone.LivingStone.is(player))) {
			GloveCasting.cast(player, true);
		}
	}
}
