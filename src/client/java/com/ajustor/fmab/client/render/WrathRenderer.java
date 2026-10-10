package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.WrathEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Wrath : l'uniforme bleu du Généralissime, la moustache et le bandeau sur l'œil. */
public class WrathRenderer extends FigureRenderer.Simple<WrathEntity> {
	public WrathRenderer(EntityRendererProvider.Context context) {
		super(context, BossModels.wrath(), Fmab.id("textures/entity/wrath.png"), Temper.WRATH);
	}
}
