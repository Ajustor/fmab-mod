package com.ajustor.fmab.network;

import com.ajustor.fmab.Fmab;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Le candidat remet ses objets ({@code give}) ou demande l'épreuve en arène ({@code fight}). */
public record ExamActionPayload(int entityId, String action) implements CustomPacketPayload {
	public static final String GIVE = "give";
	public static final String FIGHT = "fight";
	public static final Type<ExamActionPayload> TYPE = new Type<>(Fmab.id("exam_action"));

	public static final StreamCodec<ByteBuf, ExamActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ExamActionPayload::entityId,
			ByteBufCodecs.stringUtf8(16), ExamActionPayload::action,
			ExamActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
