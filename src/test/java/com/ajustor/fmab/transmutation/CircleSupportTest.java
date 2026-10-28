package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.circle.LinkKind;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.rules.Analysis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le bloc qui porte le cercle est de la matière comme une autre : les effets qui le couvrent le prennent. */
class CircleSupportTest {
	private static final BlockPos CIRCLE = new BlockPos(0, 64, 0);

	private static EffectContext context(CircleFrame frame) {
		Analysis.StageEffect stage = new Analysis.StageEffect(0, -1, null, 2, 0, Vec2.ZERO, Double.NaN, Set.of(),
				LinkKind.NONE, "line", 0, 0, Set.of());
		return new EffectContext(null, CIRCLE, frame, null, stage, List.of(), null, 0, 1, 0);
	}

	@Test
	void floorZonesTakeTheSupport() {
		EffectContext ctx = context(CircleFrame.forFace(Direction.UP, Direction.NORTH));
		for (String zone : List.of("below", "cube", "sphere")) {
			List<BlockPos> blocks = DataEffects.zone(ctx, zone);
			assertTrue(blocks.contains(ctx.support()), zone + " devrait prendre le support");
			assertFalse(blocks.contains(CIRCLE), zone + " ne vise pas le cercle lui-même");
		}
	}

	@Test
	void wallZonesTakeTheSupport() {
		EffectContext ctx = context(CircleFrame.forFace(Direction.EAST, Direction.EAST));
		for (String zone : List.of("cube", "sphere")) {
			assertTrue(DataEffects.zone(ctx, zone).contains(ctx.support()), zone + " devrait prendre le mur");
		}
	}
}
