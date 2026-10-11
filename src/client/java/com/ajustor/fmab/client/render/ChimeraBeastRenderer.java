package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.ChimeraBeastEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** La grande chimère : avant-train de lion, arrière-train de loup, une queue de serpent qui ondule. */
public class ChimeraBeastRenderer extends MobRenderer<ChimeraBeastEntity, ChimeraModels.BeastState, ChimeraModels.Beast> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/chimera_beast.png");

	public ChimeraBeastRenderer(EntityRendererProvider.Context context) {
		super(context, new ChimeraModels.Beast(ChimeraModels.Beast.bake()), 0.9f);
	}

	@Override
	public ChimeraModels.BeastState createRenderState() {
		return new ChimeraModels.BeastState();
	}

	@Override
	public void extractRenderState(ChimeraBeastEntity entity, ChimeraModels.BeastState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.rear = entity.rearAmount(partialTicks);
	}

	@Override
	public Identifier getTextureLocation(ChimeraModels.BeastState state) {
		return TEXTURE;
	}
}
