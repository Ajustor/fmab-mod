package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.ExamProgress;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.state.StateExam;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Le serveur ouvre l'examen : le rang du candidat, son avancement et, pour chaque objet demandé,
 * combien il en porte.
 */
public record OpenExamPayload(int entityId, boolean eligible, boolean itemsGiven, boolean passed, List<Row> rows)
		implements CustomPacketPayload {
	public record Row(String key, int held, int count) {
		public static final StreamCodec<ByteBuf, Row> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Row::key,
				ByteBufCodecs.VAR_INT, Row::held,
				ByteBufCodecs.VAR_INT, Row::count,
				Row::new);
	}

	public static final Type<OpenExamPayload> TYPE = new Type<>(Fmab.id("open_exam"));

	public static final StreamCodec<ByteBuf, OpenExamPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, OpenExamPayload::entityId,
			ByteBufCodecs.BOOL, OpenExamPayload::eligible,
			ByteBufCodecs.BOOL, OpenExamPayload::itemsGiven,
			ByteBufCodecs.BOOL, OpenExamPayload::passed,
			Row.CODEC.apply(ByteBufCodecs.list()), OpenExamPayload::rows,
			OpenExamPayload::new);

	public static OpenExamPayload of(Entity examiner, ServerPlayer player) {
		ExamProgress exam = player.getAttachedOrCreate(FmabAttachments.EXAM);
		boolean eligible = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST).rank().atLeast(Rank.ALCHEMIST);
		List<Row> rows = StateExam.requirements().stream()
				.map(r -> new Row(r.key(), r.held(player), r.count()))
				.toList();
		return new OpenExamPayload(examiner.getId(), eligible, exam.itemsGiven(), exam.passed(), rows);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
