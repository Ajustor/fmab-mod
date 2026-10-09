package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.ChimeraCrawlerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** La chimère rampante, sur le corps d'une araignée : un pelage de chien, des pattes d'insecte. */
public class ChimeraCrawlerRenderer extends SpiderRenderer<ChimeraCrawlerEntity> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/chimera_crawler.png");

	public ChimeraCrawlerRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}
}
