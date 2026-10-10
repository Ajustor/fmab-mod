package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.DrachmaSoldierEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Un soldat de Drachma : long manteau brun-vert, chapka de fourrure, baïonnette. */
public class DrachmaSoldierRenderer extends FigureRenderer.Simple<DrachmaSoldierEntity> {
	public DrachmaSoldierRenderer(EntityRendererProvider.Context context) {
		super(context, FigureModels.drachma(), Fmab.id("textures/entity/drachma_soldier.png"), Temper.SOLDIER);
	}
}
