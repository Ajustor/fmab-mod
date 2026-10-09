package com.ajustor.fmab;

import com.ajustor.fmab.arts.ScarArm;
import com.ajustor.fmab.block.AlchemistTableBlock;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.gate.GateOfTruth;
import com.ajustor.fmab.gate.HumanTransmutation;
import com.ajustor.fmab.gate.SoulBinding;
import com.ajustor.fmab.gate.Tolls;
import com.ajustor.fmab.homunculus.Belly;
import com.ajustor.fmab.homunculus.EnvySpawner;
import com.ajustor.fmab.item.Tomes;
import com.ajustor.fmab.network.FmabNetwork;
import com.ajustor.fmab.progress.Milestones;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlockEntities;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.registry.FmabLoot;
import com.ajustor.fmab.registry.FmabMenus;
import com.ajustor.fmab.registry.FmabRegistries;
import com.ajustor.fmab.registry.FmabSounds;
import com.ajustor.fmab.stone.Eclipse;
import com.ajustor.fmab.stone.Karma;
import com.ajustor.fmab.stone.LivingStone;
import com.ajustor.fmab.stone.PhilosopherStones;
import com.ajustor.fmab.training.IslandTrial;
import com.ajustor.fmab.transmutation.ChimeraTransmutation;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.GloveCasting;
import com.ajustor.fmab.transmutation.GoldTransmutation;
import com.ajustor.fmab.transmutation.Passives;
import com.ajustor.fmab.transmutation.Transmutation;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import com.ajustor.fmab.world.FmabStructures;
import com.ajustor.fmab.xing.Alkahestry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fmab implements ModInitializer {
	public static final String MOD_ID = "fmab";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ClientHooks clientHooks = ClientHooks.NONE;

	@Override
	public void onInitialize() {
		FmabConfig.load();
		FmabRegistries.register();
		FmabComponents.register();
		FmabSounds.register();
		FmabAttachments.register();
		FmabBlocks.register();
		FmabBlockEntities.register();
		FmabEntities.register();
		FmabStructures.register();
		FmabItems.register();
		FmabMenus.register();
		FmabLoot.register();
		FmabNetwork.register();
		FmabCommands.register();
		Concentration.register();
		Passives.register();
		TransmutationLightning.register();
		HumanTransmutation.register();
		GoldTransmutation.register();
		Transmutation.register();
		ChimeraTransmutation.register();
		GateOfTruth.register();
		Tolls.register();
		Automails.register();
		SoulBinding.register();
		Belly.register();
		EnvySpawner.register();
		Karma.register();
		Eclipse.register();
		PhilosopherStones.register();
		LivingStone.register();
		Alkahestry.register();
		Milestones.register();
		AlchemistTableBlock.registerSealing();
		ScarArm.register();
		IslandTrial.register();
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			equip(handler.getPlayer());
			Notebooks.ensure(handler.getPlayer());
		});
		// Gantelets : frapper un bloc, main libre, y lance leur cercle au lieu de commencer à le casser.
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (!GloveCasting.canStrike(player)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer serverPlayer) {
				GloveCasting.strike(serverPlayer, pos, direction);
			}
			return InteractionResult.SUCCESS;
		});
	}

	/**
	 * Comme les frères Elric à Resembool, chacun commence avec de quoi apprendre : le Traité, les
	 * Rudiments (qui enseignent les glyphes des cercles du carnet) et de la craie ; le carnet, lui,
	 * l'alchimiste le garde en tête ({@link Notebooks#ensure}). Une seule fois par joueur.
	 */
	private static void equip(ServerPlayer player) {
		if (Boolean.TRUE.equals(player.getAttached(FmabAttachments.EQUIPPED))) {
			return;
		}
		player.setAttached(FmabAttachments.EQUIPPED, true);
		// Les rudiments sont acquis : les cercles du carnet de départ ne rebondissent pas pour un rien.
		AlchemistData data = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		for (String glyph : Tomes.RUDIMENTS.glyphs()) {
			data = data.learn(glyph);
		}
		player.setAttached(FmabAttachments.ALCHEMIST, data);
		for (ItemStack stack : new ItemStack[]{new ItemStack(FmabItems.ALCHEMY_TREATISE),
				Tomes.stack(Tomes.RUDIMENTS), new ItemStack(FmabItems.CHALK)}) {
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
