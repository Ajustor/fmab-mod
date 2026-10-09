package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.AmestrianSoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Les soldats d'Amestris : l'uniforme bleu de Central, ou le manteau d'hiver de Briggs. */
public class SoldierRenderer extends HumanoidMobRenderer<AmestrianSoldierEntity, HumanoidRenderState,
		HumanoidModel<HumanoidRenderState>> {
	private final Identifier texture;

	public SoldierRenderer(EntityRendererProvider.Context context, String skin) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.texture = Fmab.id("textures/entity/" + skin + ".png");
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return texture;
	}
}
