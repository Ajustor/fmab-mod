package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.ThrowingKnifeEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

/** Le couteau de lancer de Hughes, en vol ou planté. */
public class ThrowingKnifeRenderer extends ArrowRenderer<ThrowingKnifeEntity, ArrowRenderState> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/throwing_knife.png");

	public ThrowingKnifeRenderer(EntityRendererProvider.Context context) {
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
