package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.FailedTransmutationEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Ce qui sort d'une transmutation humaine : la masse aux six bras (voir {@link FailedTransmutationModel}). */
public class FailedTransmutationRenderer
		extends MobRenderer<FailedTransmutationEntity, LivingEntityRenderState, FailedTransmutationModel> {
	/** La masse d'un corps d'adulte : plus grande que les morceaux du modèle ne le laissent croire. */
	private static final float SCALE = 1.5f;
	private static final Identifier TEXTURE = Fmab.id("textures/entity/failed_transmutation.png");

	public FailedTransmutationRenderer(EntityRendererProvider.Context context) {
		super(context, new FailedTransmutationModel(FailedTransmutationModel.bake()), 1.1f);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}
}
