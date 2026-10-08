package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.block.TransmutationCircleBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

/**
 * Repère d'un cercle inscrit : la normale qui sort de la surface, et les directions du monde qui
 * correspondent au haut et à la droite de la page du carnet, vue de face.
 */
public record CircleFrame(AttachFace face, Direction normal, Direction pageUp, Direction pageRight) {
	public static CircleFrame of(BlockState state) {
		AttachFace face = state.getValue(TransmutationCircleBlock.FACE);
		Direction facing = state.getValue(TransmutationCircleBlock.FACING);
		return switch (face) {
			case FLOOR -> new CircleFrame(face, Direction.UP, facing, facing.getClockWise());
			// Vu d'en dessous, la droite de la page est à l'opposé de ce qu'elle serait vue d'en haut.
			case CEILING -> new CircleFrame(face, Direction.DOWN, facing, facing.getCounterClockWise());
			case WALL -> new CircleFrame(face, facing, Direction.UP, facing.getCounterClockWise());
		};
	}

	public boolean onFloor() {
		return face == AttachFace.FLOOR;
	}

	/** Angle de la page (radians, x à droite, y vers le bas) vers la direction du monde la plus proche. */
	public Direction toWorld(double angle) {
		Direction down = pageUp.getOpposite();
		double cos = Math.cos(angle), sin = Math.sin(angle);
		double x = pageRight.getStepX() * cos + down.getStepX() * sin;
		double y = pageRight.getStepY() * cos + down.getStepY() * sin;
		double z = pageRight.getStepZ() * cos + down.getStepZ() * sin;
		return Direction.getApproximateNearest(x, y, z);
	}
}
