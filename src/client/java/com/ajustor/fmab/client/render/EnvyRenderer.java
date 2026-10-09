package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.EnvyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.silverfish.SilverfishModel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * Envy sous toutes ses formes : les traits d'un joueur ou d'un habitant, sa vraie apparence, le
 * monstre géant (le même corps, à une autre échelle et couvert de visages), et le petit lézard.
 */
public class EnvyRenderer extends HumanoidMobRenderer<EnvyEntity, EnvyRenderer.State, EnvyRenderer.Model> {
	private static final Identifier HUMAN = Fmab.id("textures/entity/envy.png");
	private static final Identifier GIANT = Fmab.id("textures/entity/envy_giant.png");
	private static final Identifier LIZARD = Fmab.id("textures/entity/envy_lizard.png");

	public static class State extends HumanoidRenderState {
		EnvyEntity.Form form = EnvyEntity.Form.HUMAN;
		Identifier texture = HUMAN;
	}

	/** Le corps humain d'Envy ; invisible quand il n'est plus qu'un lézard. */
	public static class Model extends HumanoidModel<State> {
		/** Sa touffe en palmier, sur le modèle de sa vraie forme seulement. */
		private final ModelPart spikes;

		Model(ModelPart root) {
			super(root);
			spikes = head.hasChild("spikes") ? head.getChild("spikes") : null;
		}

		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			boolean human = state.form != EnvyEntity.Form.LIZARD;
			// Sa touffe en palmier n'appartient qu'à sa vraie forme : déguisé, il a la tête d'un autre.
			if (spikes != null) {
				spikes.visible = state.form == EnvyEntity.Form.HUMAN;
			}
			head.visible = human;
			hat.visible = human;
			body.visible = human;
			leftArm.visible = human;
			rightArm.visible = human;
			leftLeg.visible = human;
			rightLeg.visible = human;
		}
	}

	/** Sa vraie forme (et le géant), avec sa touffe. */
	private final Model own;
	/** Déguisé : le modèle complet d'un joueur, seconde couche de peau comprise. */
	private final Model disguise;

	public EnvyRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(BossModels.envy()), 0.5f);
		own = model;
		disguise = new Model(context.bakeLayer(ModelLayers.PLAYER));
		addLayer(new LizardLayer(this, new SilverfishModel(context.bakeLayer(ModelLayers.SILVERFISH))));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		boolean disguised = state.form == EnvyEntity.Form.DISGUISED_PLAYER
				|| state.form == EnvyEntity.Form.DISGUISED_CITIZEN;
		model = disguised ? disguise : own;
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	public void extractRenderState(EnvyEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.form = entity.form();
		state.texture = switch (entity.form()) {
			case DISGUISED_PLAYER -> entity.copiedPlayer().map(EnvyRenderer::skinOf).orElse(HUMAN);
			case DISGUISED_CITIZEN -> Fmab.id("textures/entity/" + entity.disguise() + ".png");
			case GIANT -> GIANT;
			default -> HUMAN;
		};
	}

	private static Identifier skinOf(UUID player) {
		var connection = Minecraft.getInstance().getConnection();
		PlayerInfo info = connection == null ? null : connection.getPlayerInfo(player);
		return (info != null ? info.getSkin() : DefaultPlayerSkin.get(player)).body().texturePath();
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return state.texture;
	}

	/** Sa vraie forme : un petit lézard vert. */
	private static class LizardLayer extends RenderLayer<State, Model> {
		private final SilverfishModel model;

		LizardLayer(RenderLayerParent<State, Model> parent, SilverfishModel model) {
			super(parent);
			this.model = model;
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, State state, float yRot,
				float xRot) {
			if (state.form != EnvyEntity.Form.LIZARD) {
				return;
			}
			collector.order(1).submitModel(model, state, poseStack, RenderTypes.entityCutout(LIZARD), light,
					LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor, null);
		}
	}
}
