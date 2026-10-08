package com.ajustor.fmab.item;

import com.ajustor.fmab.Fmab;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Objet qui ouvre un écran côté client : le Carnet de cercles et le Traité d'alchimie. */
public class ScreenItem extends Item {
	public enum Kind {
		NOTEBOOK,
		TREATISE
	}

	private final Kind kind;

	public ScreenItem(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			Fmab.clientHooks().openScreen(kind, hand);
		}
		return InteractionResult.SUCCESS;
	}
}
