package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.IzumiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Izumi, sur le modèle humanoïde aux bras fins, avec sa propre texture. */
public class IzumiRenderer extends FigureRenderer.Simple<IzumiEntity> {
	public IzumiRenderer(EntityRendererProvider.Context context) {
		super(context, FigureModels.woman(true), Fmab.id("textures/entity/izumi.png"), Temper.IZUMI);
	}
}
