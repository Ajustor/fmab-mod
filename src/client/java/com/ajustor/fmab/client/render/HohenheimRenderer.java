package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.HohenheimEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Van Hohenheim : cheveux blonds noués, barbe, lunettes et long manteau. */
public class HohenheimRenderer extends FigureRenderer.Simple<HohenheimEntity> {
	public HohenheimRenderer(EntityRendererProvider.Context context) {
		super(context, FigureModels.hohenheim(), Fmab.id("textures/entity/hohenheim.png"), Temper.HOHENHEIM);
	}
}
