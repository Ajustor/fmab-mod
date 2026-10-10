package com.ajustor.fmab.client.render;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.StateExaminerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** L'examinateur, en uniforme bleu de l'armée. */
public class StateExaminerRenderer extends FigureRenderer.Simple<StateExaminerEntity> {
	public StateExaminerRenderer(EntityRendererProvider.Context context) {
		super(context, FigureModels.soldier(), Fmab.id("textures/entity/state_examiner.png"), Temper.OFFICER);
	}
}
