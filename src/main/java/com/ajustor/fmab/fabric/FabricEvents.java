package com.ajustor.fmab.fabric;

import com.ajustor.fmab.platform.event.ContentEvents;
import com.ajustor.fmab.platform.event.InteractionEvents;
import com.ajustor.fmab.platform.event.LivingEvents;
import com.ajustor.fmab.platform.event.PlayerEvents;
import com.ajustor.fmab.platform.event.ServerEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Relaie les événements de Fabric API vers ceux du mod ({@code platform.event}). Installé une fois,
 * avant l'initialisation du mod : chaque événement du mod n'a ainsi qu'un abonné côté Fabric.
 */
final class FabricEvents {
	private FabricEvents() {
	}

	static void install() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> ServerEvents.STARTED.invoker().on(server));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerEvents.STOPPED.invoker().on(server));
		ServerTickEvents.END_SERVER_TICK.register(server -> ServerEvents.END_TICK.invoker().on(server));
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) ->
				ServerEvents.DATA_RELOADED.invoker().onReloaded(server, success));

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				PlayerEvents.JOIN.invoker().on(handler.getPlayer(), server));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				PlayerEvents.DISCONNECT.invoker().on(handler.getPlayer(), server));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
				PlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(oldPlayer, newPlayer, alive));
		ServerPlayerEvents.ALLOW_DEATH.register((player, source, amount) ->
				PlayerEvents.ALLOW_DEATH.invoker().allowDeath(player, source, amount));

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				LivingEvents.ALLOW_DAMAGE.invoker().allowDamage(entity, source, amount));
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) ->
				LivingEvents.AFTER_DAMAGE.invoker().afterDamage(entity, source, baseDamage, damage, blocked));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) ->
				LivingEvents.AFTER_DEATH.invoker().afterDeath(entity, source));

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				InteractionEvents.ATTACK_ENTITY.invoker().attack(player, level, hand, entity, hit));
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				InteractionEvents.ATTACK_BLOCK.invoker().attack(player, level, hand, pos, direction));
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				InteractionEvents.USE_ENTITY.invoker().use(player, level, hand, entity, hit));
		UseBlockCallback.EVENT.register((player, level, hand, hit) ->
				InteractionEvents.USE_BLOCK.invoker().use(player, level, hand, hit));
		UseItemCallback.EVENT.register((player, level, hand) -> InteractionEvents.USE_ITEM.invoker().use(player, level, hand));
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
				InteractionEvents.BEFORE_BREAK.invoker().beforeBreak(level, player, pos, state, blockEntity));
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) ->
				InteractionEvents.AFTER_BREAK.invoker().afterBreak(level, player, pos, state, blockEntity));

		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) ->
				ContentEvents.COMMANDS.invoker().register(dispatcher, context, selection));
		LootTableEvents.MODIFY.register((key, table, source, registries) ->
				ContentEvents.MODIFY_LOOT.invoker().modify(key, table, source.isBuiltin(), registries));
	}
}
