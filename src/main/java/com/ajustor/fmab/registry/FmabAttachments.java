package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Gloves;
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
