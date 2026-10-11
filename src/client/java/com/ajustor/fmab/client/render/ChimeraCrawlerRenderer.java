package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.ChimeraCrawlerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** La chimère rampante : un tronc de chien cousu sur six membres dépareillés. */
public class ChimeraCrawlerRenderer extends MobRenderer<ChimeraCrawlerEntity, LivingEntityRenderState, ChimeraModels.Crawler> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/chimera_crawler.png");

	public ChimeraCrawlerRenderer(EntityRendererProvider.Context context) {
		super(context, new ChimeraModels.Crawler(ChimeraModels.Crawler.bake()), 0.7f);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}
}
