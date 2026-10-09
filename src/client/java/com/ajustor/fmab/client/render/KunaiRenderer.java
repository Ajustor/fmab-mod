package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.KunaiEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

/** Le kunaï planté ou en vol : une lame d'acier sombre, un anneau, un ruban rouge. */
public class KunaiRenderer extends ArrowRenderer<KunaiEntity, ArrowRenderState> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/kunai.png");

	public KunaiRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ArrowRenderState createRenderState() {
		return new ArrowRenderState();
	}

	@Override
	protected Identifier getTextureLocation(ArrowRenderState state) {
		return TEXTURE;
	}
}
