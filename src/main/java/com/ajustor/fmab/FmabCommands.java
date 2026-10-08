package com.ajustor.fmab;

import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * {@code /fmab} : réglages de test réservés aux opérateurs (rang, glyphes, maîtrise, nœuds de savoir,
 * concentration). Le jeu normal passe par le Traité, la pratique et le repos.
 */
public final class FmabCommands {
	private FmabCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("fmab")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("rank")
								.then(Commands.argument("rank", StringArgumentType.word())
										.suggests((c, b) -> {
											Arrays.stream(Rank.values()).forEach(r -> b.suggest(r.serializedName()));
											return b.buildFuture();
										})
										.executes(c -> {
											ServerPlayer p = c.getSource().getPlayerOrException();
											Rank rank = Rank.fromSerializedName(StringArgumentType.getString(c, "rank"));
											update(p, data(p).withRank(rank));
											c.getSource().sendSuccess(() -> Component.translatable(rank.translationKey()), false);
											return 1;
										})))
						.then(Commands.literal("learn_all").executes(c -> {
							ServerPlayer p = c.getSource().getPlayerOrException();
							AlchemistData data = data(p);
							for (Glyph g : AlchemyRules.of(p.level().registryAccess()).glyphs()) {
								data = data.learn(g.id());
							}
							update(p, data);
							return 1;
						}))
						.then(Commands.literal("forget_all").executes(c -> {
							ServerPlayer p = c.getSource().getPlayerOrException();
							update(p, data(p).forgetAll());
							return 1;
						}))
						.then(Commands.literal("mastery")
								.then(Commands.argument("school", StringArgumentType.word())
										.then(Commands.argument("amount", IntegerArgumentType.integer(0))
												.executes(c -> {
													ServerPlayer p = c.getSource().getPlayerOrException();
													update(p, data(p).addMastery(StringArgumentType.getString(c, "school"),
															IntegerArgumentType.getInteger(c, "amount")));
													return 1;
												}))))
						.then(Commands.literal("grant")
								.then(Commands.argument("node", StringArgumentType.greedyString())
										.executes(c -> {
											ServerPlayer p = c.getSource().getPlayerOrException();
											update(p, data(p).grant(StringArgumentType.getString(c, "node")));
											return 1;
										})))
						.then(Commands.literal("rest").executes(c -> {
							ServerPlayer p = c.getSource().getPlayerOrException();
							update(p, data(p).withConcentration(AlchemistData.MAX_CONCENTRATION));
							return 1;
						}))));
	}

	private static AlchemistData data(ServerPlayer player) {
		return player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
	}

	private static void update(ServerPlayer player, AlchemistData data) {
		player.setAttached(FmabAttachments.ALCHEMIST, data);
	}
}
