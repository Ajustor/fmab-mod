package com.ajustor.fmab.fabric.client;

import com.ajustor.fmab.platform.client.ClientPlatform;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Le client du mod sur Fabric : Fabric API derrière les ports du client. */
public final class FabricClientPlatform implements ClientPlatform {
	@Override
	public void send(CustomPacketPayload payload) {
		ClientPlayNetworking.send(payload);
	}

	@Override
	public <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, Receiver<T> receiver) {
		ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> receiver.receive(payload, context.client()));
	}

	@Override
	public KeyMapping keyMapping(KeyMapping key) {
		return KeyMappingHelper.registerKeyMapping(key);
	}

	@Override
	public <E extends Entity> void entityRenderer(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
		EntityRendererRegistry.register(type, provider);
	}

	@Override
	public <E extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(BlockEntityType<E> type,
			BlockEntityRendererProvider<? super E, ? super S> provider) {
		BlockEntityRendererRegistry.register(type, provider);
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void renderLayers(RenderLayers layers) {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) ->
				layers.add(type, renderer, layer -> helper.register((RenderLayer) layer), context));
	}

	@Override
	public void hudElement(Identifier id, HudAnchor anchor, HudElement element) {
		switch (anchor) {
			case AFTER_FOOD_BAR -> HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, id, element::extract);
			case BEFORE_MISC_OVERLAYS ->
					HudElementRegistry.attachElementBefore(VanillaHudElements.MISC_OVERLAYS, id, element::extract);
		}
	}
}
