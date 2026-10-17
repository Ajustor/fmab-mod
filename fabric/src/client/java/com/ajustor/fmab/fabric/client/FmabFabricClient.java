package com.ajustor.fmab.fabric.client;

import com.ajustor.fmab.client.FmabClient;
import com.ajustor.fmab.platform.client.ClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** Le point d'entrée Fabric du client : relayer les événements du client, puis lancer le client du mod. */
public final class FmabFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> ClientEvents.END_TICK.invoker().tick(client));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientEvents.DISCONNECT.invoker().tick(client));
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> ClientEvents.GAME_MESSAGE.invoker().receive(message, overlay));
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) ->
				ClientEvents.ITEM_TOOLTIP.invoker().append(stack, context, flag, lines));
		FmabClient.init();
	}
}
