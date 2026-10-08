package com.ajustor.fmab;

import com.ajustor.fmab.item.ScreenItem;
import net.minecraft.world.InteractionHand;

/**
 * Ce que le code commun demande au client. Le point d'entrée client remplace l'implémentation
 * vide ; sur un serveur dédié, elle reste vide.
 */
public interface ClientHooks {
	ClientHooks NONE = (kind, hand) -> {
	};

	void openScreen(ScreenItem.Kind kind, InteractionHand hand);
}
