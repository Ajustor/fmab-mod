package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.OlivierEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Olivier Armstrong : longs cheveux blonds, manteau d'hiver bleu de Briggs, sabre au poing. */
public class OlivierRenderer extends FigureRenderer.Simple<OlivierEntity> {
	public OlivierRenderer(EntityRendererProvider.Context context) {
		super(context, FigureModels.olivier(), Fmab.id("textures/entity/olivier.png"), Temper.OLIVIER);
	}
}
