package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.GreedEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/**
 * Greed : manteau à col de fourrure, lunettes noires, les mains dans les poches ; bouclier levé, une
 * peau de carbone noir.
 */
public class GreedRenderer extends FigureRenderer<GreedEntity, GreedRenderer.State, FigureRenderer.Model<GreedRenderer.State>> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/greed.png");
	private static final Identifier SHIELD = Fmab.id("textures/entity/greed_shield.png");

	public static class State extends FigureRenderer.State {
		boolean shielded;
	}

	public GreedRenderer(EntityRendererProvider.Context context) {
		super(context, new FigureRenderer.Model<>(BossModels.greed(), Temper.GREED));
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
