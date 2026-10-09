package com.ajustor.fmab;

import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.gate.GateOfTruth;
import com.ajustor.fmab.gate.HumanTransmutation;
import com.ajustor.fmab.gate.SoulBinding;
import com.ajustor.fmab.gate.Tolls;
import com.ajustor.fmab.homunculus.Belly;
import com.ajustor.fmab.homunculus.EnvySpawner;
import com.ajustor.fmab.item.Tomes;
import com.ajustor.fmab.network.FmabNetwork;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlockEntities;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.registry.FmabRegistries;
import com.ajustor.fmab.stone.Eclipse;
import com.ajustor.fmab.stone.Karma;
import com.ajustor.fmab.stone.LivingStone;
import com.ajustor.fmab.stone.PhilosopherStones;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.GloveCasting;
import com.ajustor.fmab.transmutation.Passives;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import com.ajustor.fmab.world.FmabStructures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Fmab implements ModInitializer {
	public static final String MOD_ID = "fmab";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ClientHooks clientHooks = ClientHooks.NONE;

	@Override
	public void onInitialize() {
		FmabConfig.load();
		FmabRegistries.register();
		FmabComponents.register();
		FmabAttachments.register();
		FmabBlocks.register();
		FmabBlockEntities.register();
		FmabEntities.register();
		FmabStructures.register();
		FmabItems.register();
		FmabNetwork.register();
		FmabCommands.register();
		Concentration.register();
		Passives.register();
		TransmutationLightning.register();
		HumanTransmutation.register();
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
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> equip(handler.getPlayer()));
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
	 * Rudiments (qui enseignent les glyphes des cercles du carnet), un carnet et de la craie. Une
	 * seule fois par joueur.
	 */
	private static void equip(ServerPlayer player) {
		if (Boolean.TRUE.equals(player.getAttached(FmabAttachments.EQUIPPED))) {
			return;
		}
		player.setAttached(FmabAttachments.EQUIPPED, true);
		for (ItemStack stack : new ItemStack[]{new ItemStack(FmabItems.ALCHEMY_TREATISE),
				Tomes.stack(Tomes.RUDIMENTS), starterNotebook(player), new ItemStack(FmabItems.CHALK)}) {
			if (!player.getInventory().add(stack)) {
				player.drop(stack, false);
			}
		}
	}

	/** Effets des cercles simples recopiés dans le carnet de départ. */
	private static final List<String> STARTER_CIRCLES = List.of("fmab:wall", "fmab:spike", "fmab:ice_platform");

	/**
	 * Un carnet qui contient déjà quelques cercles simples, prêts à tracer à la craie. Les noms de
	 * page sont des clés de traduction ({@code @}), affichées dans la langue du joueur.
	 */
	private static ItemStack starterNotebook(ServerPlayer player) {
		AlchemyRules rules = AlchemyRules.of(player.level().registryAccess());
		List<NotebookContents.Page> pages = new ArrayList<>();
		for (String effect : STARTER_CIRCLES) {
			rules.combinations().stream()
					.filter(c -> c.effect().equals(effect))
					.findFirst()
					.flatMap(rules::simpleCircle)
					.ifPresent(d -> pages.add(new NotebookContents.Page("@effect." + effect.replace(':', '.'), d)));
		}
		ItemStack notebook = new ItemStack(FmabItems.CIRCLE_NOTEBOOK);
		notebook.set(FmabComponents.NOTEBOOK, new NotebookContents(pages, 0));
		return notebook;
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
