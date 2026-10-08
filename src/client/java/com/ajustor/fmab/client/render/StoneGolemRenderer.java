package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.StoneGolemEntity;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;

/** Le golem de pierre de l'examen : la silhouette d'un golem, taillée dans la pierre. */
public class StoneGolemRenderer extends MobRenderer<StoneGolemEntity, IronGolemRenderState, IronGolemModel> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/stone_golem.png");

	public StoneGolemRenderer(EntityRendererProvider.Context context) {
		super(context, new IronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7f);
	}

	@Override
	public IronGolemRenderState createRenderState() {
		return new IronGolemRenderState();
	}

	@Override
	public void extractRenderState(StoneGolemEntity entity, IronGolemRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.attackTicksRemaining = entity.attackAnimation(partialTicks);
	}

	@Override
	public Identifier getTextureLocation(IronGolemRenderState state) {
		return TEXTURE;
	}
}
