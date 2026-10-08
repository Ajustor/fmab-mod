package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.GreedEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Greed : manteau à col de fourrure, lunettes noires ; bouclier levé, une peau de carbone noir. */
public class GreedRenderer extends HumanoidMobRenderer<GreedEntity, GreedRenderer.State, HumanoidModel<GreedRenderer.State>> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/greed.png");
	private static final Identifier SHIELD = Fmab.id("textures/entity/greed_shield.png");

	public static class State extends HumanoidRenderState {
		boolean shielded;
	}

	public GreedRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GreedEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.shielded = entity.shielded();
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return state.shielded ? SHIELD : TEXTURE;
	}
}
