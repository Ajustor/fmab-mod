package com.ajustor.fmab;

import com.ajustor.fmab.network.FmabNetwork;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlockEntities;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.registry.FmabRegistries;
import com.ajustor.fmab.transmutation.Concentration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fmab implements ModInitializer {
	public static final String MOD_ID = "fmab";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ClientHooks clientHooks = ClientHooks.NONE;

	@Override
	public void onInitialize() {
		FmabRegistries.register();
		FmabComponents.register();
		FmabAttachments.register();
		FmabBlocks.register();
		FmabBlockEntities.register();
		FmabItems.register();
		FmabNetwork.register();
		FmabCommands.register();
		Concentration.register();
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> equip(handler.getPlayer()));
	}

	/**
	 * Comme les frères Elric à Resembool, chacun commence avec de quoi apprendre : le Traité, un
	 * carnet et de la craie. Une seule fois par joueur.
	 */
	private static void equip(ServerPlayer player) {
		if (Boolean.TRUE.equals(player.getAttached(FmabAttachments.EQUIPPED))) {
			return;
		}
		player.setAttached(FmabAttachments.EQUIPPED, true);
		for (Item item : new Item[]{FmabItems.ALCHEMY_TREATISE, FmabItems.CIRCLE_NOTEBOOK, FmabItems.CHALK}) {
			ItemStack stack = new ItemStack(item);
			if (!player.getInventory().add(stack)) {
				player.drop(stack, false);
			}
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static ClientHooks clientHooks() {
		return clientHooks;
	}

	public static void setClientHooks(ClientHooks hooks) {
		clientHooks = hooks;
	}
}
