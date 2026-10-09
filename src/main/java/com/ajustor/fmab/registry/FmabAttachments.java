package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.ExamProgress;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.data.Tattoos;
import com.ajustor.fmab.data.Training;
import com.ajustor.fmab.data.TransmutationPose;
import com.ajustor.fmab.homunculus.Belly;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.List;
import java.util.Set;

public final class FmabAttachments {
	/** Rang, savoir et concentration du joueur ; gardés à la mort, visibles de lui seul. */
	public static final AttachmentType<AlchemistData> ALCHEMIST = AttachmentRegistry.<AlchemistData>builder()
			.persistent(AlchemistData.CODEC)
			.initializer(() -> AlchemistData.NEW)
			.copyOnDeath()
			.syncWith(AlchemistData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Fmab.id("alchemist"));

	/** Les gants portés, à gauche et à droite. */
	public static final AttachmentType<Gloves> GLOVES = AttachmentRegistry.<Gloves>builder()
			.persistent(Gloves.CODEC)
			.initializer(() -> Gloves.NONE)
			.copyOnDeath()
			.syncWith(Gloves.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Fmab.id("gloves"));

	/** Les épreuves d'Izumi : gardées à la mort, inutiles au client. */
	public static final AttachmentType<Training> TRAINING = AttachmentRegistry.<Training>builder()
			.persistent(Training.CODEC)
			.initializer(() -> Training.NONE)
			.copyOnDeath()
			.buildAndRegister(Fmab.id("training"));

	/** L'examen d'Alchimiste d'État : gardé à la mort, inutile au client. */
	public static final AttachmentType<ExamProgress> EXAM = AttachmentRegistry.<ExamProgress>builder()
			.persistent(ExamProgress.CODEC)
			.initializer(() -> ExamProgress.NONE)
			.copyOnDeath()
			.buildAndRegister(Fmab.id("exam"));

	/** Cercles tatoués : permanents, gardés à la mort, visibles du joueur seul. */
	public static final AttachmentType<Tattoos> TATTOOS = AttachmentRegistry.<Tattoos>builder()
			.persistent(Tattoos.CODEC)
			.initializer(() -> Tattoos.NONE)
			.copyOnDeath()
			.syncWith(Tattoos.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Fmab.id("tattoos"));

	/** Ce que la Porte a pris, et la visite en cours ; le client voit ce qui manque (vue, mains). */
	public static final AttachmentType<GateState> GATE = AttachmentRegistry.<GateState>builder()
			.persistent(GateState.CODEC)
			.initializer(() -> GateState.NONE)
			.copyOnDeath()
			.syncWith(GateState.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Fmab.id("gate"));

	/** Les automails posés sur les membres perdus. */
	public static final AttachmentType<Automail> AUTOMAIL = AttachmentRegistry.<Automail>builder()
			.persistent(Automail.CODEC)
			.initializer(() -> Automail.NONE)
			.copyOnDeath()
			.syncWith(Automail.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Fmab.id("automail"));

	/** Le joueur est dans le Ventre de Gluttony : perdu à la mort, inutile au client. */
	public static final AttachmentType<Belly.Swallowed> SWALLOWED = AttachmentRegistry.<Belly.Swallowed>builder()
			.persistent(Belly.Swallowed.CODEC)
			.buildAndRegister(Fmab.id("swallowed"));

	/** Le karma, de −100 à +100 : gardé à la mort, visible du joueur. */
	public static final AttachmentType<Integer> KARMA = AttachmentRegistry.<Integer>builder()
			.persistent(Codec.INT)
			.initializer(() -> 0)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Fmab.id("karma"));

	/** Les âmes d'une Pierre philosophale vivante ; absent pour qui n'en est pas une. */
	public static final AttachmentType<Integer> LIVING_STONE = AttachmentRegistry.<Integer>builder()
			.persistent(Codec.INT)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Fmab.id("living_stone"));

	/** L'alkahestry de Xing, apprise de May Chang : gardée à la mort. */
	public static final AttachmentType<Boolean> ALKAHESTRY = AttachmentRegistry.<Boolean>builder()
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.buildAndRegister(Fmab.id("alkahestry"));

	/** Reconnu allié de Briggs par Olivier Armstrong : gardé à la mort. */
	public static final AttachmentType<Boolean> BRIGGS_ALLY = AttachmentRegistry.<Boolean>builder()
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.buildAndRegister(Fmab.id("briggs_ally"));

	/** Recherché par l'armée jusqu'à cette heure de jeu, pour avoir frappé un soldat. */
	public static final AttachmentType<Long> WANTED = AttachmentRegistry.<Long>builder()
			.persistent(Codec.LONG)
			.copyOnDeath()
			.buildAndRegister(Fmab.id("wanted"));

	/** Le bras droit de Scar, tatoué : gardé à la mort, connu du client (il ne mine plus à mains nues). */
	public static final AttachmentType<Boolean> SCAR_ARM = AttachmentRegistry.<Boolean>builder()
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Fmab.id("scar_arm"));

	/** Le temps passé d'affilée sur l'île de Yock, en ticks (l'épreuve d'Izumi). */
	public static final AttachmentType<Integer> ISLAND_TIME = AttachmentRegistry.<Integer>builder()
			.persistent(Codec.INT)
			.buildAndRegister(Fmab.id("island_time"));

	/** Les cadeaux uniques déjà reçus des PNJ (notes de Hohenheim, de Scar…) : gardés à la mort. */
	public static final AttachmentType<Set<String>> GIFTS = AttachmentRegistry.<Set<String>>builder()
			.persistent(Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf))
			.initializer(Set::of)
			.copyOnDeath()
			.buildAndRegister(Fmab.id("gifts"));

	/** Les homonculus qu'on a vus tomber : la clé du sceau de Père. Gardés à la mort. */
	public static final AttachmentType<Set<String>> SLAIN = AttachmentRegistry.<Set<String>>builder()
			.persistent(Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf))
			.initializer(Set::of)
			.copyOnDeath()
			.buildAndRegister(Fmab.id("slain"));

	/** Le geste de transmutation en cours : éphémère, visible de tous pour animer le joueur. */
	public static final AttachmentType<TransmutationPose> POSE = AttachmentRegistry.<TransmutationPose>builder()
			.syncWith(TransmutationPose.STREAM_CODEC, AttachmentSyncPredicate.all())
			.buildAndRegister(Fmab.id("pose"));

	/** Le joueur a reçu son matériel de départ. */
	public static final AttachmentType<Boolean> EQUIPPED = AttachmentRegistry.<Boolean>builder()
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.buildAndRegister(Fmab.id("equipped"));

	private FmabAttachments() {
	}

	public static void register() {
	}
}
