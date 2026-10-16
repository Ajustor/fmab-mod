package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.MarcohEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Le docteur Marcoh : crâne dégarni, blouse claire, l'air las. */
public class MarcohRenderer extends FigureRenderer.Simple<MarcohEntity> {
	public MarcohRenderer(EntityRendererProvider.Context context) {
		super(context, FigureModels.marcoh(), Fmab.id("textures/entity/marcoh.png"), Temper.MARCOH);
	}
}
