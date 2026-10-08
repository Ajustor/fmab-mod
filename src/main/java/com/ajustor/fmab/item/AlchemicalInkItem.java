package com.ajustor.fmab.item;

import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.network.OpenTattooPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Encre alchimique : utilisée sur un cercle inscrit, elle ouvre le rituel du tatouage (choix de
 * l'emplacement). En main, elle sert aussi à effacer un tatouage.
 */
public class AlchemicalInkItem extends Item {
	public AlchemicalInkItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TransmutationCircleBlockEntity circle)
				|| circle.drawing().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (context.getPlayer() instanceof ServerPlayer player) {
			ServerPlayNetworking.send(player, OpenTattooPayload.of(player, context.getClickedPos()));
		}
		return InteractionResult.SUCCESS;
	}
}
