package com.ajustor.fmab.fabric;

import com.ajustor.fmab.platform.network.Network;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Les paquets du mod, par Fabric Networking API. */
final class FabricNetwork implements Network {
	@Override
	public <T extends CustomPacketPayload> void clientbound(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec, int maxSize) {
		if (maxSize == DEFAULT_MAX_SIZE) {
			PayloadTypeRegistry.clientboundPlay().register(type, codec);
		} else {
			PayloadTypeRegistry.clientboundPlay().registerLarge(type, codec, maxSize);
		}
	}

	@Override
	public <T extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec, int maxSize) {
		if (maxSize == DEFAULT_MAX_SIZE) {
			PayloadTypeRegistry.serverboundPlay().register(type, codec);
		} else {
			PayloadTypeRegistry.serverboundPlay().registerLarge(type, codec, maxSize);
		}
	}

	@Override
	public <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, Receiver<T> receiver) {
		ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> receiver.receive(payload, context.player()));
	}

	@Override
	public void send(ServerPlayer player, CustomPacketPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}
}
