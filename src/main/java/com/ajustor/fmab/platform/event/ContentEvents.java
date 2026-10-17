package com.ajustor.fmab.platform.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/** Ce que le mod ajoute au contenu du jeu au chargement : commandes, butin des tables du jeu. */
public final class ContentEvents {
	/** Le moment d'enregistrer les commandes (à chaque chargement des packs de données). */
	public static final Event<RegisterCommands> COMMANDS = Event.create(listeners -> (dispatcher, context, selection) ->
			listeners.forEach(l -> l.register(dispatcher, context, selection)));
	/** Une table de butin vient d'être lue : le mod peut y ajouter des tirages. */
	public static final Event<ModifyLoot> MODIFY_LOOT = Event.create(listeners -> (key, table, builtin, registries) ->
			listeners.forEach(l -> l.modify(key, table, builtin, registries)));

	private ContentEvents() {
	}

	@FunctionalInterface
	public interface RegisterCommands {
		void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
				Commands.CommandSelection selection);
	}

	@FunctionalInterface
	public interface ModifyLoot {
		/**
		 * @param builtin la table vient du jeu ou d'un mod, pas d'un pack de données qui l'aurait remplacée
		 */
		void modify(ResourceKey<LootTable> key, LootTable.Builder table, boolean builtin, HolderLookup.Provider registries);
	}
}
