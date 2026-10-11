package com.ajustor.fmab.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * Le rendu commun des personnages humanoïdes du mod : leur modèle joue, par-dessus l'animation
 * ordinaire, le {@link Temper tempérament} du personnage, et sait s'il se bat.
 */
public abstract class FigureRenderer<E extends Mob, S extends FigureRenderer.State, M extends FigureRenderer.Model<S>>
		extends HumanoidMobRenderer<E, S, M> {
	/** L'état de rendu d'un personnage : ce que l'animation humanoïde sait, et s'il se bat. */
	public static class State extends HumanoidRenderState {
		public boolean aggressive;
	}

	/** Le modèle humanoïde, et le tempérament qu'il joue. */
	public static class Model<S extends State> extends HumanoidModel<S> {
		private final Temper temper;

		public Model(ModelPart root, Temper temper) {
			super(root);
			this.temper = temper;
		}

		@Override
		public void setupAnim(S state) {
			super.setupAnim(state);
			temper.apply(this, state, state.aggressive);
		}
	}

	protected FigureRenderer(EntityRendererProvider.Context context, M model) {
		super(context, model, 0.5f);
	}

	@Override
	public void extractRenderState(E entity, S state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.aggressive = entity.isAggressive();
	}

	/** Un personnage sans rien d'autre : une peau, un modèle, un tempérament. */
	public static class Simple<E extends Mob> extends FigureRenderer<E, State, Model<State>> {
		private final Identifier texture;

		public Simple(EntityRendererProvider.Context context, ModelPart root, Identifier texture, Temper temper) {
			super(context, new Model<>(root, temper));
			this.texture = texture;
		}

		@Override
		public State createRenderState() {
			return new State();
		}

		@Override
		public Identifier getTextureLocation(State state) {
			return texture;
		}
	}
}
