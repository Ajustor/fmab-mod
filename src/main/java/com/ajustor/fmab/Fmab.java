package com.ajustor.fmab;

import com.ajustor.fmab.transmutation.CopperCircuits;
import com.ajustor.fmab.promised.PromisedDay;
import com.ajustor.fmab.arts.ScarArm;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.ajustor.fmab.block.AlchemistTableBlock;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.gate.GateOfTruth;
import com.ajustor.fmab.gate.HumanTransmutation;
import com.ajustor.fmab.gate.Shells;
import com.ajustor.fmab.gate.SoulBinding;
import com.ajustor.fmab.gate.SoulGifts;
import com.ajustor.fmab.gate.SoulOffering;
import com.ajustor.fmab.gate.Tolls;
import com.ajustor.fmab.gate.Wheelchairs;
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
import com.ajustor.fmab.stone.ProjectileTransmutation;
import com.ajustor.fmab.training.IslandTrial;
import com.ajustor.fmab.transmutation.ChimeraTransmutation;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.GloveCasting;
import com.ajustor.fmab.transmutation.GoldTransmutation;
import com.ajustor.fmab.transmutation.ItemCompositions;
import com.ajustor.fmab.transmutation.Passives;
import com.ajustor.fmab.transmutation.Transmutation;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import com.ajustor.fmab.util.PlayerMaps;
import com.ajustor.fmab.world.FmabStructures;
import com.ajustor.fmab.xing.Alkahestry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ajustor.fmab.platform.event.InteractionEvents;
import com.ajustor.fmab.platform.event.PlayerEvents;
import com.ajustor.fmab.compat.EntityCompat;

public final class Fmab {
	public static final String MOD_ID = "fmab";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ClientHooks clientHooks = ClientHooks.NONE;

	private Fmab() {
	}

	/** Lance le mod : appelé par le point d'entrée du loader, une fois ses événements branchés. */
	public static void init() {
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
		CopperCircuits.register();
		PromisedDay.register();
		Transmutation.register();
		ItemCompositions.register();
		ChimeraTransmutation.register();
		GateOfTruth.register();
		Tolls.register();
		Automails.register();
		Wheelchairs.register();
		SoulBinding.register();
		SoulOffering.register();
		SoulGifts.register();
		Shells.register();
		Belly.register();
		EnvySpawner.register();
		Karma.register();
		Eclipse.register();
		PhilosopherStones.register();
		LivingStone.register();
		ProjectileTransmutation.register();
		Alkahestry.register();
		Milestones.register();
		AlchemistTableBlock.registerSealing();
		ScarArm.register();
		PlayerMaps.register();
		IslandTrial.register();
		PlayerEvents.JOIN.register((player, server) -> {
			equip(player);
			Notebooks.ensure(player);
		});
		// Gantelets : frapper un bloc, main libre, y lance leur cercle au lieu de commencer à le casser.
		InteractionEvents.ATTACK_BLOCK.register((player, level, hand, pos, direction) -> {
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
		if (Boolean.TRUE.equals(FmabAttachments.EQUIPPED.get(player))) {
			return;
		}
		FmabAttachments.EQUIPPED.set(player, true);
		// Les rudiments sont acquis : les cercles du carnet de départ ne rebondissent pas pour un rien.
		AlchemistData data = FmabAttachments.ALCHEMIST.getOrCreate(player);
		for (String glyph : Tomes.RUDIMENTS.glyphs()) {
			data = data.learn(glyph);
		}
		FmabAttachments.ALCHEMIST.set(player, data);
		for (ItemStack stack : new ItemStack[]{new ItemStack(FmabItems.ALCHEMY_TREATISE),
				Tomes.stack(Tomes.RUDIMENTS), new ItemStack(FmabItems.CHALK)}) {
			if (!player.getInventory().add(stack)) {
				EntityCompat.drop(player, stack);
			}
		}
		player.sendSystemMessage(welcome());
	}

	/** Le mot d'accueil : où apprendre, et les deux touches qui guident. */
	public static Component welcome() {
		return Component.translatable("welcome.fmab.greeting", Component.keybind("key.fmab.notebook"),
				Component.keybind("key.advancements")).withStyle(ChatFormatting.GOLD);
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
