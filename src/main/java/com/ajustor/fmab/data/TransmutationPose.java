package com.ajustor.fmab.data;

import com.ajustor.fmab.registry.FmabAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

/**
 * Le geste d'un alchimiste qui transmute, vu de tous : le client le joue sur le modèle du joueur
 * de l'heure {@code start} jusqu'à l'heure {@code until} (temps de jeu). Le début lui sert à prendre
 * son élan ; une pose enregistrée avant qu'on le retienne commence à 0 et se joue sans élan.
 */
public record TransmutationPose(Kind kind, long start, long until) {
	public enum Kind {
		NONE,
		/** Les paumes plaquées sur le cercle, penché en avant. */
		PALM,
		/** Les mains tenues sur le cercle : l'énergie continue de couler. */
		HOLD,
		/** Les mains jointes, devant soi : la transmutation sans cercle. */
		CLAP,
		/** Le bras tendu vers un être dont on absorbe les âmes. */
		REACH
	}

	public static final TransmutationPose NONE = new TransmutationPose(Kind.NONE, 0, 0);

	private static final Codec<Kind> KIND = Codec.INT.xmap(i -> Kind.values()[Math.clamp(i, 0, Kind.values().length - 1)],
			Kind::ordinal);

	public static final Codec<TransmutationPose> CODEC = RecordCodecBuilder.create(i -> i.group(
			KIND.fieldOf("kind").forGetter(TransmutationPose::kind),
			Codec.LONG.optionalFieldOf("start", 0L).forGetter(TransmutationPose::start),
			Codec.LONG.fieldOf("until").forGetter(TransmutationPose::until)
	).apply(i, TransmutationPose::new));

	public static final StreamCodec<ByteBuf, TransmutationPose> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT.map(i -> Kind.values()[Math.clamp(i, 0, Kind.values().length - 1)], Kind::ordinal),
			TransmutationPose::kind,
			ByteBufCodecs.VAR_LONG, TransmutationPose::start,
			ByteBufCodecs.VAR_LONG, TransmutationPose::until,
			TransmutationPose::new);

	/** Le joueur prend la pose pendant {@code ticks} ticks. */
	public static void strike(ServerPlayer player, Kind kind, int ticks) {
		long now = player.level().getGameTime();
		player.setAttached(FmabAttachments.POSE, new TransmutationPose(kind, now, now + ticks));
	}

	public boolean active(long now) {
		return kind != Kind.NONE && now < until;
	}
}
