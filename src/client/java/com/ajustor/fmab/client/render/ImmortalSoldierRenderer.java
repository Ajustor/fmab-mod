package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.ImmortalSoldierEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Les soldats immortels : une peau grise et recousue, un œil unique, des haillons. */
public class ImmortalSoldierRenderer extends FigureRenderer.Simple<ImmortalSoldierEntity> {
	public ImmortalSoldierRenderer(EntityRendererProvider.Context context) {
		super(context, context.bakeLayer(ModelLayers.PLAYER), Fmab.id("textures/entity/immortal_soldier.png"), Temper.IMMORTAL);
	}
}
