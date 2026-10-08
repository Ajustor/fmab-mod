package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.ExamProgress;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.data.Tattoos;
import com.ajustor.fmab.data.Training;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

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
