package com.ajustor.fmab.block;

import com.ajustor.fmab.transmutation.Transmutation;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Cercle de transmutation inscrit sur une face de bloc : sol, mur ou plafond. Il s'active paume
 * contre la surface (clic droit main vide), ou par son déclencheur ({@link CircleTrigger}, réglé
 * accroupi, main vide) : à retardement, en piège, ou sur un signal de redstone.
 *
 * <p>{@link #FACING} : au sol et au plafond, la direction où regardait le joueur en le traçant, qui
 * devient le haut de la page du carnet ; sur un mur, la direction vers laquelle le cercle fait
 * face (le haut de la page est alors le ciel).
 */
public class TransmutationCircleBlock extends BaseEntityBlock {
	public static final MapCodec<TransmutationCircleBlock> CODEC = simpleCodec(TransmutationCircleBlock::new);
	public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<CircleMedium> MEDIUM = EnumProperty.create("medium", CircleMedium.class);

	private static final double THICKNESS = 0.25;
	private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, THICKNESS, 16);
	private static final VoxelShape CEILING = Block.box(0, 16 - THICKNESS, 0, 16, 16, 16);
	private static final VoxelShape NORTH_WALL = Block.box(0, 0, 16 - THICKNESS, 16, 16, 16);
	private static final VoxelShape SOUTH_WALL = Block.box(0, 0, 0, 16, 16, THICKNESS);
	private static final VoxelShape WEST_WALL = Block.box(16 - THICKNESS, 0, 0, 16, 16, 16);
	private static final VoxelShape EAST_WALL = Block.box(0, 0, 0, THICKNESS, 16, 16);
	/** Une gravure se défait aussi lentement que la pierre qui la porte. */
	private static final float ENGRAVING_HARDNESS = 3;

	public TransmutationCircleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any()
				.setValue(FACE, AttachFace.FLOOR)
				.setValue(FACING, Direction.NORTH)
				.setValue(MEDIUM, CircleMedium.CHALK));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACE, FACING, MEDIUM);
	}

	/**
	 * L'état du cercle tracé sur la face {@code clicked} d'un bloc, ou null si rien ne peut y tenir.
	 *
	 * @param lookDirection direction horizontale où regarde le joueur
	 */
	public BlockState stateFor(Direction clicked, Direction lookDirection, CircleMedium medium) {
		BlockState state = defaultBlockState().setValue(MEDIUM, medium);
		return switch (clicked) {
			case UP -> state.setValue(FACE, AttachFace.FLOOR).setValue(FACING, lookDirection);
			case DOWN -> state.setValue(FACE, AttachFace.CEILING).setValue(FACING, lookDirection);
			default -> state.setValue(FACE, AttachFace.WALL).setValue(FACING, clicked);
		};
	}

	/** Direction qui sort de la surface, du support vers le cercle. */
	public static Direction normal(BlockState state) {
		return switch (state.getValue(FACE)) {
			case FLOOR -> Direction.UP;
			case CEILING -> Direction.DOWN;
			case WALL -> state.getValue(FACING);
		};
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TransmutationCircleBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		// Le tracé est dessiné par le renderer du bloc-entité ; le modèle n'est qu'une poussière.
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACE)) {
			case FLOOR -> FLOOR;
			case CEILING -> CEILING;
			case WALL -> switch (state.getValue(FACING)) {
				case SOUTH -> SOUTH_WALL;
				case WEST -> WEST_WALL;
				case EAST -> EAST_WALL;
				default -> NORTH_WALL;
			};
		};
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction normal = normal(state);
		BlockPos support = pos.relative(normal.getOpposite());
		return level.getBlockState(support).isFaceSturdy(level, support, normal);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		return directionToNeighbour == normal(state).getOpposite() && !state.canSurvive(level, pos)
				? Blocks.AIR.defaultBlockState()
				: super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (state.getValue(MEDIUM) == CircleMedium.ENGRAVING) {
			// Même progression qu'un bloc de dureté 3 cassé à la main.
			return player.getDestroySpeed(state) / ENGRAVING_HARDNESS / 100;
		}
		return super.getDestroyProgress(state, player, level, pos);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hitResult) {
		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
				&& level.getBlockEntity(pos) instanceof TransmutationCircleBlockEntity circle) {
			if (player.isShiftKeyDown()) {
				// Accroupi : on règle le déclencheur, et on en devient l'auteur.
				CircleTrigger next = circle.trigger().next();
				circle.setTrigger(next, player.getUUID());
				serverPlayer.sendOverlayMessage(Component.translatable("circle.fmab.trigger_set",
						Component.translatable(next.translationKey())));
				serverLevel.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4f, 1.4f);
				return InteractionResult.SUCCESS;
			}
			if (circle.trigger() == CircleTrigger.DELAY) {
				// La paume l'arme : il partira dans trois secondes, au compte de qui l'a touché.
				circle.setTrigger(CircleTrigger.DELAY, player.getUUID());
				serverLevel.scheduleTick(pos, this, CircleTrigger.DELAY_TICKS);
				serverPlayer.sendOverlayMessage(Component.translatable("circle.fmab.armed"));
				serverLevel.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.8f, 0.8f);
				return InteractionResult.SUCCESS;
			}
			// Garder la main sur le cercle prolonge la transmutation qu'on vient d'y lancer.
			if (!Transmutation.channel(serverLevel, pos, state, circle.drawing(), serverPlayer, circle.size())) {
				Transmutation.activate(serverLevel, pos, state, circle.drawing(), serverPlayer, circle.size());
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Le cercle part tout seul, au compte de son auteur, s'il est là pour le payer. */
	private static void fire(ServerLevel level, BlockPos pos, BlockState state, TransmutationCircleBlockEntity circle) {
		ServerPlayer author = circle.author().map(level.getServer().getPlayerList()::getPlayer).orElse(null);
		if (author == null || author.level() != level || !author.isAlive()) {
			// Personne pour nourrir le cercle : il grésille et s'éteint.
			level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 8, 0.3, 0.05,
					0.3, 0.01);
			return;
		}
		Transmutation.activate(level, pos, state, circle.drawing(), author, circle.size());
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof TransmutationCircleBlockEntity circle
				&& circle.trigger() == CircleTrigger.DELAY) {
			fire(level, pos, state, circle);
		}
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (!(level instanceof ServerLevel server) || !(entity instanceof LivingEntity) || entity.isSpectator()
				|| state.getValue(FACE) != AttachFace.FLOOR
				|| !(level.getBlockEntity(pos) instanceof TransmutationCircleBlockEntity circle)
				|| circle.trigger() != CircleTrigger.PRESSURE
				|| circle.author().map(entity.getUUID()::equals).orElse(false)) {
			return;
		}
		if (circle.rearm(server.getGameTime())) {
			fire(server, pos, state, circle);
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
			@Nullable Orientation orientation, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
		if (!(level instanceof ServerLevel server)
				|| !(level.getBlockEntity(pos) instanceof TransmutationCircleBlockEntity circle)
				|| circle.trigger() != CircleTrigger.REDSTONE) {
			return;
		}
		boolean signal = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.relative(normal(state).getOpposite()));
		if (circle.power(signal) && circle.rearm(server.getGameTime())) {
			fire(server, pos, state, circle);
		}
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(MEDIUM) == CircleMedium.CHALK;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		// La craie ne tient pas sous la pluie.
		if (level.isRainingAt(pos) && random.nextInt(3) == 0) {
			level.removeBlock(pos, false);
		}
	}
}
