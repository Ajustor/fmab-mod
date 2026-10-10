package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.WinryEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Winry, sur le modèle humanoïde aux bras fins : bleu de travail, bandana et clé à molette. */
public class WinryRenderer extends FigureRenderer.Simple<WinryEntity> {
	public WinryRenderer(EntityRendererProvider.Context context) {
		super(context, FigureModels.winry(), Fmab.id("textures/entity/winry.png"), Temper.WINRY);
	}
}
