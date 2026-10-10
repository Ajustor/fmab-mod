package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.CornelloEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Le père Cornello : robe de prêtre, crâne rasé, la pierre rouge au doigt. */
public class CornelloRenderer extends FigureRenderer.Simple<CornelloEntity> {
	public CornelloRenderer(EntityRendererProvider.Context context) {
		super(context, FigureModels.cornello(), Fmab.id("textures/entity/cornello.png"), Temper.CORNELLO);
	}
}
