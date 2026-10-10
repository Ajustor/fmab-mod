package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.SlothEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Sloth : un géant chauve, torse nu, l'Ouroboros sur la poitrine. */
public class SlothRenderer extends FigureRenderer.Simple<SlothEntity> {
	public SlothRenderer(EntityRendererProvider.Context context) {
		super(context, BossModels.sloth(), Fmab.id("textures/entity/sloth.png"), Temper.SLOTH);
	}
}
