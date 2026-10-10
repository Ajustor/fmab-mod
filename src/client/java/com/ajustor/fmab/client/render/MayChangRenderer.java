package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.MayChangEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** May Chang : deux macarons noirs, la tunique de Xing, rose et prune. */
public class MayChangRenderer extends FigureRenderer.Simple<MayChangEntity> {
	public MayChangRenderer(EntityRendererProvider.Context context) {
		super(context, FigureModels.mayChang(), Fmab.id("textures/entity/may_chang.png"), Temper.MAY);
	}
}
