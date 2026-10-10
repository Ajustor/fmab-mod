package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.GluttonyEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Gluttony : chauve, pâle, tout en rondeur ; l'Ouroboros sur la langue. Il se dandine, les bras
 * tendus vers ce qu'il va manger, et son ventre ballotte à chaque pas.
 */
public class GluttonyRenderer extends FigureRenderer<GluttonyEntity, FigureRenderer.State, GluttonyRenderer.Model> {
	private static final Identifier TEXTURE = Fmab.id("textures/entity/gluttony.png");

	public static class Model extends FigureRenderer.Model<FigureRenderer.State> {
		private final ModelPart belly;

		Model(ModelPart root) {
			super(root, Temper.GLUTTONY);
			belly = body.getChild("belly");
		}

		@Override
		public void setupAnim(FigureRenderer.State state) {
			super.setupAnim(state);
			// Le ventre qui ballotte : il s'écrase à chaque pas et respire au repos.
			float step = Math.abs(Mth.sin(state.walkAnimationPos * 0.6662f)) * Mth.clamp(state.walkAnimationSpeed, 0, 1);
			float breath = Mth.sin(state.ageInTicks * 0.084f) * 0.03f;
			belly.yScale = 1 - step * 0.08f + breath;
			belly.xScale = 1 + step * 0.06f - breath * 0.5f;
			belly.zScale = 1 + step * 0.05f + breath;
		}
	}

	public GluttonyRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(BossModels.gluttony()));
	}

	@Override
	public FigureRenderer.State createRenderState() {
		return new FigureRenderer.State();
	}

	@Override
	public Identifier getTextureLocation(FigureRenderer.State state) {
		return TEXTURE;
	}
}
