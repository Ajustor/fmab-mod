package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.ChimeraBeastEntity;
import net.minecraft.client.model.animal.polarbear.PolarBearModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import net.minecraft.resources.Identifier;

/** La chimère massive, sur le corps d'un ours : pelage fauve de lion, écailles de serpent sur le dos. */
public class ChimeraBeastRenderer extends MobRenderer<ChimeraBeastEntity, PolarBearRenderState, PolarBearModel> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/chimera_beast.png");

	public ChimeraBeastRenderer(EntityRendererProvider.Context context) {
		super(context, new PolarBearModel(context.bakeLayer(ModelLayers.POLAR_BEAR)), 0.9f);
	}

	@Override
	public PolarBearRenderState createRenderState() {
		return new PolarBearRenderState();
	}

	@Override
	public void extractRenderState(ChimeraBeastEntity entity, PolarBearRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.standScale = entity.rearAmount(partialTicks);
	}

	@Override
	public Identifier getTextureLocation(PolarBearRenderState state) {
		return TEXTURE;
	}
}
