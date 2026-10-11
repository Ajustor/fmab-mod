package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.AmestrianSoldierEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Les soldats d'Amestris : l'uniforme bleu de Central, ou le manteau d'hiver de Briggs. */
public class SoldierRenderer extends FigureRenderer.Simple<AmestrianSoldierEntity> {
	public SoldierRenderer(EntityRendererProvider.Context context, String skin) {
		super(context, skin.equals("amestrian_soldier") ? FigureModels.soldier() : FigureModels.briggs(),
				Fmab.id("textures/entity/" + skin + ".png"), Temper.SOLDIER);
	}
}
