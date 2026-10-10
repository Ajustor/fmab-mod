package com.ajustor.fmab.client.mixin;

import com.ajustor.fmab.client.render.BodyHolder;
import com.ajustor.fmab.client.render.PoseHolder;
import com.ajustor.fmab.client.render.WheelchairPose;
import com.ajustor.fmab.data.TransmutationPose;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.item.AutomailItem;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.Set;

/**
 * L'état de rendu du joueur retient son geste de transmutation, ses membres perdus, ses automails et
 * la pose de ses membres.
 */
@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements PoseHolder, BodyHolder {
	@Unique
	private TransmutationPose.Kind fmab$pose = TransmutationPose.Kind.NONE;
	@Unique
	private float fmab$remaining;
	@Unique
	private Set<BodyPart> fmab$lostLimbs = Set.of();
	@Unique
	private Map<BodyPart, AutomailItem.Model> fmab$automails = Map.of();
	@Unique
	private Set<BodyPart> fmab$blades = Set.of();
	@Unique
	private Map<BodyPart, PartPose> fmab$skeleton = Map.of();
	@Unique
	private WheelchairPose fmab$wheelchair = WheelchairPose.NONE;

	@Override
	public TransmutationPose.Kind fmab$pose() {
		return fmab$pose;
	}

	@Override
	public float fmab$remaining() {
		return fmab$remaining;
	}

	@Override
	public void fmab$setPose(TransmutationPose.Kind pose, float remaining) {
		fmab$pose = pose;
		fmab$remaining = remaining;
	}

	@Override
	public Set<BodyPart> fmab$lostLimbs() {
		return fmab$lostLimbs;
	}

	@Override
	public Map<BodyPart, AutomailItem.Model> fmab$automails() {
		return fmab$automails;
	}

	@Override
	public void fmab$setBody(Set<BodyPart> lostLimbs, Map<BodyPart, AutomailItem.Model> automails) {
		fmab$lostLimbs = lostLimbs;
		fmab$automails = automails;
	}

	@Override
	public Set<BodyPart> fmab$blades() {
		return fmab$blades;
	}

	@Override
	public void fmab$setBlades(Set<BodyPart> blades) {
		fmab$blades = blades;
	}

	@Override
	public Map<BodyPart, PartPose> fmab$skeleton() {
		return fmab$skeleton;
	}

	@Override
	public void fmab$setSkeleton(Map<BodyPart, PartPose> skeleton) {
		fmab$skeleton = skeleton;
	}

	@Override
	public WheelchairPose fmab$wheelchair() {
		return fmab$wheelchair;
	}

	@Override
	public void fmab$setWheelchair(WheelchairPose pose) {
		fmab$wheelchair = pose;
	}
}
