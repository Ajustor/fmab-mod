package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.ScarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Scar : peau mate, cheveux blancs, la cicatrice en X, le bras droit tatoué. */
public class ScarRenderer extends FigureRenderer.Simple<ScarEntity> {
	public ScarRenderer(EntityRendererProvider.Context context) {
		super(context, FigureModels.scar(), Fmab.id("textures/entity/scar.png"), Temper.SCAR);
	}
}
