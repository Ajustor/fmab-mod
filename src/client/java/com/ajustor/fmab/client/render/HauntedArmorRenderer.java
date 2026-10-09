package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.HauntedArmorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Les armures habitées (et Barry) : une armure d'acier vide. Effondrée, elle gît à terre, en morceaux
 * (la tête roule à côté du corps), jusqu'à ce qu'elle se reforme ou qu'on efface son sceau.
 */
public class HauntedArmorRenderer<T extends HauntedArmorEntity>
		extends HumanoidMobRenderer<T, HauntedArmorRenderer.State, HauntedArmorRenderer.Model> {
	private final Identifier texture;

	public static class State extends HumanoidRenderState {
		boolean collapsed;
	}

	public static class Model extends HumanoidModel<State> {
		Model(ModelPart root) {
			super(root);
		}

		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			if (state.collapsed) {
				// En morceaux : la tête décrochée, les bras de travers.
				head.x += 6;
				head.y += 2;
				rightArm.zRot = 1.2f;
				leftArm.zRot = -0.4f;
				rightLeg.xRot = 0.5f;
				leftLeg.xRot = -0.3f;
			}
		}
	}

	public HauntedArmorRenderer(EntityRendererProvider.Context context, String skin) {
		super(context, new Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.texture = Fmab.id("textures/entity/" + skin + ".png");
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(T entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.collapsed = entity.collapsed();
	}

	@Override
	protected void setupRotations(State state, PoseStack poseStack, float bodyRot, float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		if (state.collapsed) {
			// À plat, sur le dos.
			poseStack.mulPose(Axis.XP.rotationDegrees(-90));
			poseStack.translate(0, -0.9, 0.1);
		}
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return texture;
	}
}
