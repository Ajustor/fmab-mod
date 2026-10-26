package com.ajustor.fmab.data;

import com.ajustor.fmab.registry.FmabAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

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
		FmabAttachments.POSE.set(player, new TransmutationPose(kind, now, now + ticks));
	}

	/**
	 * Le joueur garde la pose {@code ticks} ticks de plus : un geste qu'on maintient (une absorption,
	 * un cercle qu'on tient) ne reprend pas son élan à chaque pulsation.
	 */
	public static void keep(ServerPlayer player, Kind kind, int ticks) {
		long now = player.level().getGameTime();
		FmabAttachments.POSE.set(player, kept(FmabAttachments.POSE.get(player), kind, now, ticks));
	}

	/** La pose prolongée : même geste encore en cours, il garde son début ; sinon il en prend un nouveau. */
	public static TransmutationPose kept(@Nullable TransmutationPose current, Kind kind, long now, int ticks) {
		long start = current != null && current.kind() == kind && current.active(now) ? current.start() : now;
		return new TransmutationPose(kind, start, now + ticks);
	}

	public boolean active(long now) {
		return kind != Kind.NONE && now < until;
	}
}
