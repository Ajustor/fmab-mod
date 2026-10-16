package com.ajustor.fmab.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.List;

/**
 * Les abords d'une maison : son plan, entouré d'une marge d'un bloc où l'on pose ce qui déborde
 * des murs. Le toit dépasse en avant-toit et sur les pignons, des volets encadrent les fenêtres,
 * des jardinières fleurissent celles de l'étage ; au pied des murs, des massifs, un chemin jusqu'à
 * la porte et un réverbère à côté.
 */
final class Dressing implements Blueprint {
	/** La marge autour du plan d'origine, de chaque côté. */
	static final int MARGIN = 1;

	/**
	 * Le style des abords.
	 *
	 * @param soil    le sol au pied des murs
	 * @param path    le chemin devant la porte
	 * @param shutter les volets et les jardinières
	 * @param plants  ce qui pousse au pied des murs et dans les jardinières
	 */
	record Style(BlockState soil, BlockState path, Block shutter, List<BlockState> plants, List<BlockState> pots) {
	}

	private static BlockState leaves(Block block) {
		return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
	}

	static final Style TOWN = new Style(Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.POLISHED_ANDESITE.defaultBlockState(),
			Blocks.DARK_OAK_TRAPDOOR, List.of(leaves(Blocks.AZALEA_LEAVES), Blocks.ALLIUM.defaultBlockState(),
					Blocks.POPPY.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(), leaves(Blocks.OAK_LEAVES)),
			List.of(Blocks.POTTED_RED_TULIP.defaultBlockState(), Blocks.POTTED_WHITE_TULIP.defaultBlockState(),
					Blocks.POTTED_AZURE_BLUET.defaultBlockState()));
	static final Style RURAL = new Style(Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT_PATH.defaultBlockState(),
			Blocks.SPRUCE_TRAPDOOR, List.of(leaves(Blocks.OAK_LEAVES), Blocks.DANDELION.defaultBlockState(),
					Blocks.SHORT_GRASS.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(),
					leaves(Blocks.FLOWERING_AZALEA_LEAVES), Blocks.CORNFLOWER.defaultBlockState()),
			List.of(Blocks.POTTED_DANDELION.defaultBlockState(), Blocks.POTTED_POPPY.defaultBlockState(),
					Blocks.POTTED_OXEYE_DAISY.defaultBlockState()));
	static final Style DESERT = new Style(Blocks.COARSE_DIRT.defaultBlockState(), Blocks.SMOOTH_SANDSTONE.defaultBlockState(),
			Blocks.JUNGLE_TRAPDOOR, List.of(Blocks.DEAD_BUSH.defaultBlockState(), Blocks.BARREL.defaultBlockState(),
					Blocks.DECORATED_POT.defaultBlockState()),
			List.of(Blocks.POTTED_CACTUS.defaultBlockState(), Blocks.POTTED_DEAD_BUSH.defaultBlockState()));

	private final Blueprint inner;
	private final Style style;

	Dressing(Blueprint inner, Style style) {
		this.inner = inner;
		this.style = style;
	}

	private static Plot inner(Plot p) {
		return new Plot(p.sizeX() - 2 * MARGIN, p.sizeY(), p.sizeZ() - 2 * MARGIN, p.ground(), p.seed());
	}

	/** La première rangée du toit : là où le coin de la façade porte une marche. */
	private int roofStart(Plot in) {
		for (int y = 1; y < in.sizeY(); y++) {
			if (inner.at(0, y, 0, in) instanceof BlockState s && s.getBlock() instanceof StairBlock) {
				return y;
			}
		}
		return in.sizeY();
	}

	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		Plot in = inner(p);
		int ix = x - MARGIN, iz = z - MARGIN, sx = in.sizeX(), sz = in.sizeZ();
		boolean outX = ix < 0 || ix >= sx, outZ = iz < 0 || iz >= sz;
		if (!outX && !outZ) {
			return inner.at(ix, y, iz, in);
		}
		int cx = Math.clamp(ix, 0, sx - 1), cz = Math.clamp(iz, 0, sz - 1);
		Direction out = iz < 0 ? Direction.NORTH : iz >= sz ? Direction.SOUTH : ix < 0 ? Direction.WEST : Direction.EAST;
		int roof = roofStart(in);
		if (y >= roof - 1) {
			return overhang(cx, y, cz, outX, outZ, iz < 0, in, roof);
		}
		if (y > 0) {
			return facade(cx, y, cz, outX, outZ, out, in, p, x, z);
		}
		if (y == 0) {
			return yard(cx, cz, outX, outZ, iz < 0, in, p, x, z);
		}
		boolean path = iz < 0 && inner.at(cx, 1, 0, in) instanceof BlockState d && d.getBlock() instanceof DoorBlock;
		if (y == -1) {
			return path ? style.path() : style.soil();
		}
		return Blocks.DIRT.defaultBlockState();
	}

	/** Le toit qui déborde : l'avant-toit le long des façades, et le prolongement des pentes sur les pignons. */
	private BlockState overhang(int cx, int y, int cz, boolean outX, boolean outZ, boolean front, Plot in, int roof) {
		if (outZ) {
			// La marche la plus basse de la pente, un cran plus bas et un bloc plus loin.
			BlockState above = inner.at(cx, y + 1, cz, in);
			if (above != null && above.getBlock() instanceof StairBlock
					&& above.getValue(StairBlock.FACING) == (front ? Direction.SOUTH : Direction.NORTH)) {
				return above;
			}
			return y < roof ? AIR : null;
		}
		BlockState edge = inner.at(cx, y, cz, in);
		if (edge != null && edge.getBlock() instanceof StairBlock) {
			return edge;
		}
		// Le faîte, d'un seul bloc quand la maison a une largeur impaire.
		int ridge = (in.sizeZ() - 1) / 2;
		if (in.sizeZ() % 2 == 1 && cz == ridge && y == roof + ridge) {
			return edge;
		}
		return y < roof ? AIR : null;
	}

	/** Volets de part et d'autre des fenêtres, jardinière sous celles de l'étage. */
	private BlockState facade(int cx, int y, int cz, boolean outX, boolean outZ, Direction out, Plot in, Plot p, int x,
			int z) {
		if (outX && outZ) {
			return y <= 3 ? AIR : null;
		}
		// Le réverbère devant la façade, deux pas à côté de la porte.
		if (y == 1 && out == Direction.NORTH && cx == in.sizeX() / 2 + 2 && hasDoor(in)) {
			return Blocks.LANTERN.defaultBlockState();
		}
		BlockState wall = inner.at(cx, y, cz, in);
		if (isPane(wall)) {
			// Les fleurs de la jardinière, devant la vitre du bas.
			return y > 4 && y % 4 == 1 ? style.pots().get(p.noise(x, y, z, style.pots().size())) : AIR;
		}
		if (y > 0 && y % 4 == 0 && isPane(inner.at(cx, y + 1, cz, in))) {
			return style.shutter().defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP)
					.setValue(TrapDoorBlock.FACING, out);
		}
		int dx = outZ ? 1 : 0, dz = outZ ? 0 : 1;
		// Entre deux fenêtres rapprochées, le mur reste nu : les volets n'encadrent que les bouts de rangée.
		boolean beside = isPane(inner.at(Math.clamp(cx + dx, 0, in.sizeX() - 1), y, Math.clamp(cz + dz, 0, in.sizeZ() - 1), in))
				!= isPane(inner.at(Math.clamp(cx - dx, 0, in.sizeX() - 1), y, Math.clamp(cz - dz, 0, in.sizeZ() - 1), in));
		if (beside && y % 4 >= 1 && y % 4 <= 2) {
			return style.shutter().defaultBlockState().setValue(TrapDoorBlock.OPEN, true)
					.setValue(TrapDoorBlock.FACING, out).setValue(TrapDoorBlock.HALF, y % 4 == 1 ? Half.BOTTOM : Half.TOP);
		}
		return y <= 3 ? AIR : null;
	}

	/** Au pied des murs : le seuil devant la porte, un poteau de réverbère, des massifs çà et là. */
	private BlockState yard(int cx, int cz, boolean outX, boolean outZ, boolean front, Plot in, Plot p, int x, int z) {
		if (front && !outX && inner.at(cx, 1, 0, in) instanceof BlockState d && d.getBlock() instanceof DoorBlock) {
			return AIR;
		}
		if (front && !outX && cx == in.sizeX() / 2 + 2 && hasDoor(in)) {
			return Blocks.DARK_OAK_FENCE.defaultBlockState();
		}
		if (front && !outX && Math.abs(cx - in.sizeX() / 2) <= 1) {
			return AIR;
		}
		int pick = p.noise(x, 0, z, style.plants().size() * 2);
		return pick < style.plants().size() ? style.plants().get(pick) : AIR;
	}

	private boolean hasDoor(Plot in) {
		return inner.at(in.sizeX() / 2, 1, 0, in) instanceof BlockState d && d.getBlock() instanceof DoorBlock;
	}

	private static boolean isPane(BlockState state) {
		return state != null && state.is(Blocks.GLASS_PANE);
	}

	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	@Override
	public List<Chest> chests(Plot p) {
		return inner.chests(inner(p)).stream()
				.map(c -> new Chest(c.local().offset(MARGIN, 0, MARGIN), plot -> c.contents().apply(inner(plot))))
				.toList();
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		return inner.spawns(inner(p)).stream().map(s -> new Spawn(s.type(), s.local().offset(MARGIN, 0, MARGIN))).toList();
	}
}
