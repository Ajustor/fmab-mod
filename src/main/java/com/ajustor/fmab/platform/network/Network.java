package com.ajustor.fmab.platform.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Le port réseau, côté serveur : déclarer les paquets du mod (dans les deux sens), recevoir ceux du
 * client, envoyer les siens. Les récepteurs sont appelés sur le fil du serveur.
 */
public interface Network {
	/** Un paquet du serveur vers le client ; son récepteur se déclare côté client. */
	<T extends CustomPacketPayload> void clientbound(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec, int maxSize);

	default <T extends CustomPacketPayload> void clientbound(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
		clientbound(type, codec, DEFAULT_MAX_SIZE);
	}

	/** Un paquet du client vers le serveur. Son récepteur se déclare avec {@link #receive}. */
	<T extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec, int maxSize);

	default <T extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
		serverbound(type, codec, DEFAULT_MAX_SIZE);
	}

	<T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, Receiver<T> receiver);

	void send(ServerPlayer player, CustomPacketPayload payload);

	/** La taille ordinaire d'un paquet ; au-delà, on la donne à la déclaration. */
	int DEFAULT_MAX_SIZE = -1;

	@FunctionalInterface
	interface Receiver<T> {
		void receive(T payload, ServerPlayer player);
	}
}
