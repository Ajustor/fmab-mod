package com.ajustor.fmab.item;

import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.network.OpenTattooPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.ajustor.fmab.tattoo.TattooRitual;
import com.ajustor.fmab.network.FmabNetwork;

/**
 * Encre alchimique : utilisée sur un cercle inscrit, elle ouvre le rituel du tatouage (choix de
 * l'emplacement) ; dans le vide, l'éditeur de dessins sur la peau. En main, elle sert aussi à effacer
 * un tatouage.
 */
public class AlchemicalInkItem extends Item {
	public AlchemicalInkItem(Properties properties) {
		super(properties);
	}

	/** Dans le vide : l'éditeur de dessins sur la peau. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		return TattooRitual.openEditor(player, false) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TransmutationCircleBlockEntity circle)
				|| circle.drawing().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (context.getPlayer() instanceof ServerPlayer player) {
			FmabNetwork.send(player, OpenTattooPayload.of(player, context.getClickedPos(), false));
		}
		return InteractionResult.SUCCESS;
	}
}
