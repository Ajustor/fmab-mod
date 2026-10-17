package com.ajustor.fmab.platform.client;

import com.ajustor.fmab.platform.Services;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Ce que le mod attend du loader côté client : le pendant de {@link com.ajustor.fmab.platform.Platform}
 * ({@code META-INF/services/com.ajustor.fmab.platform.client.ClientPlatform}). Les événements du
 * client sont dans {@link ClientEvents}.
 */
public interface ClientPlatform {
	static ClientPlatform get() {
		return Holder.INSTANCE;
	}

	/** Envoie un paquet au serveur. */
	void send(CustomPacketPayload payload);

	/** Reçoit un paquet du serveur (déclaré par {@code Network#clientbound}), sur le fil du client. */
	<T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, Receiver<T> receiver);

	/** Une touche réglable, dans le menu des commandes du jeu ; rendue pour qu'on la lise. */
	KeyMapping keyMapping(KeyMapping key);

	<E extends Entity> void entityRenderer(EntityType<? extends E> type, EntityRendererProvider<E> provider);

	<E extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(BlockEntityType<E> type,
			BlockEntityRendererProvider<? super E, ? super S> provider);

	/** Ajoute des couches de rendu aux créatures, à la création de leur rendu. */
	void renderLayers(RenderLayers layers);

	/** Dessine quelque chose sur l'interface du jeu, placé par rapport à un élément du jeu. */
	void hudElement(Identifier id, HudAnchor anchor, HudElement element);

	@FunctionalInterface
	interface Receiver<T> {
		void receive(T payload, Minecraft client);
	}

	@FunctionalInterface
	interface RenderLayers {
		void add(EntityType<?> type, LivingEntityRenderer<?, ?, ?> renderer, LayerSink layers,
				EntityRendererProvider.Context context);
	}

	@FunctionalInterface
	interface LayerSink {
		void add(RenderLayer<?, ?> layer);
	}

	@FunctionalInterface
	interface HudElement {
		void extract(GuiGraphicsExtractor graphics, DeltaTracker delta);
	}

	/** Où placer un élément de l'interface du mod, par rapport à ceux du jeu. */
	enum HudAnchor {
		/** Juste après la barre de faim. */
		AFTER_FOOD_BAR,
		/** Juste avant les surimpressions (vignette, citrouille, neige…). */
		BEFORE_MISC_OVERLAYS
	}

	final class Holder {
		private static final ClientPlatform INSTANCE = Services.load(ClientPlatform.class);

		private Holder() {
		}
	}
}
