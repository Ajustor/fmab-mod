package com.ajustor.fmab.platform.client;

import com.ajustor.fmab.platform.event.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Les événements du client : le loader les déclenche, le mod les écoute. */
public final class ClientEvents {
	/** La fin de chaque tick du client. */
	public static final Event<Tick> END_TICK = Event.create(listeners -> client -> listeners.forEach(l -> l.tick(client)));
	/** Le client quitte le monde ou le serveur. */
	public static final Event<Tick> DISCONNECT = Event.create(listeners -> client -> listeners.forEach(l -> l.tick(client)));
	/** Un message du jeu arrive (pas celui d'un joueur) ; {@code overlay} : au-dessus de la barre d'objets. */
	public static final Event<GameMessage> GAME_MESSAGE = Event.create(listeners -> (message, overlay) ->
			listeners.forEach(l -> l.receive(message, overlay)));
	/** L'infobulle d'un objet se construit : on peut y ajouter des lignes. */
	public static final Event<Tooltip> ITEM_TOOLTIP = Event.create(listeners -> (stack, context, flag, lines) ->
			listeners.forEach(l -> l.append(stack, context, flag, lines)));

	private ClientEvents() {
	}

	@FunctionalInterface
	public interface Tick {
		void tick(Minecraft client);
	}

	@FunctionalInterface
	public interface GameMessage {
		void receive(Component message, boolean overlay);
	}

	@FunctionalInterface
	public interface Tooltip {
		void append(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines);
	}
}
