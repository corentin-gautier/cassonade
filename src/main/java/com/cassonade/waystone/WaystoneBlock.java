package com.cassonade.waystone;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A monument two blocks high, whose two halves hold together like a door's. The lower half is the waystone: written down
 * when placed and struck off when gone, whatever placed or removed it, a command or an explosion.
 */
public class WaystoneBlock extends Block implements EntityBlock {
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	/** Where the crystal floats, over the top half's bottom: it reaches above the block. */
	public static final float CRYSTAL_HEIGHT = 16.5F / 16;
	private static final double PARTICLE_REACH = 2.5;
	private static final int SPARK_INTERVAL = 5;
	private static final double SPARK_TURN = 0.08;
	private static final double SPARK_RADIUS = 0.6;
	private static final double SPARK_SLANT = 0.5;
	private static final double SPARK_RISE = 0.004;
	private static final VoxelShape LOWER_SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 3, 16), Block.box(2, 3, 2, 14, 6, 14), Block.box(4, 6, 4, 12, 16, 12));
	private static final VoxelShape UPPER_SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 8, 12), Block.box(3, 8, 3, 13, 10, 13), Block.box(5, 11, 5, 11, 16, 11));

	public WaystoneBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HALF);
	}

	/** The top half carries the crystal. */
	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? new WaystoneCrystalBlockEntity(pos, state) : null;
	}

	/** Particles drawn in to the crystal, as to a conduit: the portal's, as an ender pearl leaves, move back to where they are spawned, from an offset. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(HALF) == DoubleBlockHalf.UPPER && random.nextInt(2) == 0) {
			Vec3 crystal = Vec3.atBottomCenterOf(pos).add(0, CRYSTAL_HEIGHT, 0);
			level.addParticle(ParticleTypes.PORTAL, crystal.x, crystal.y, crystal.z, spread(random), spread(random), spread(random));
			// And now and then a letter, as to an enchanting table: they are the cube's alphabet
			if (random.nextInt(2) == 0) {
				level.addParticle(ParticleTypes.ENCHANT, crystal.x, crystal.y, crystal.z, spread(random), spread(random), spread(random));
			}
		}
	}

	/** On the client, dark purple motes trailing behind two points that circle the crystal on a slanted ring: a double spiral. */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() && type == Waystones.CRYSTAL ? (BlockEntityTicker<T>) (BlockEntityTicker<WaystoneCrystalBlockEntity>) WaystoneBlock::spark : null;
	}

	private static void spark(Level level, BlockPos pos, BlockState state, WaystoneCrystalBlockEntity entity) {
		long time = level.getGameTime();
		if (time % SPARK_INTERVAL != 0) {
			return;
		}
		Vec3 crystal = Vec3.atBottomCenterOf(pos).add(0, CRYSTAL_HEIGHT, 0);
		for (int point = 0; point < 2; point++) {
			double angle = time * SPARK_TURN + point * Math.PI;
			double x = Math.cos(angle) * SPARK_RADIUS;
			double z = Math.sin(angle) * SPARK_RADIUS;
			// The ring leans, and the sparks drift up off it slowly
			level.addParticle(ParticleTypes.REVERSE_PORTAL, crystal.x + x, crystal.y + x * SPARK_SLANT, crystal.z + z, 0, SPARK_RISE, 0);
		}
	}

	private static double spread(RandomSource random) {
		return (random.nextDouble() - 0.5) * PARTICLE_REACH;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		return pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context) ? defaultBlockState() : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
			BlockState neighbor, RandomSource random) {
		DoubleBlockHalf half = state.getValue(HALF);
		if (direction.getAxis() == Direction.Axis.Y && half == DoubleBlockHalf.LOWER == (direction == Direction.UP)
				&& !(neighbor.is(this) && neighbor.getValue(HALF) != half)) {
			return Blocks.AIR.defaultBlockState();
		}
		return state;
	}

	/** Broken by its top in Creative or without a pickaxe, the lower half must not drop the item when it follows, as with a door. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && state.getValue(HALF) == DoubleBlockHalf.UPPER && (player.preventsBlockDrops() || !player.hasCorrectToolForDrops(state))
				&& level.getBlockState(pos.below()).is(this)) {
			level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (level instanceof ServerLevel serverLevel && state.getValue(HALF) == DoubleBlockHalf.LOWER && !oldState.is(this)) {
			WaystoneData.of(serverLevel.getServer()).put(GlobalPos.of(level.dimension(), pos), "");
		}
	}

	/** Called after onPlace, with the item: raises the top half, and a name given at an anvil names the place. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
		Component name = stack.get(DataComponents.CUSTOM_NAME);
		if (level instanceof ServerLevel serverLevel && name != null) {
			WaystoneData.of(serverLevel.getServer()).put(GlobalPos.of(level.dimension(), pos), name.getString());
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
			WaystoneData.of(level.getServer()).remove(GlobalPos.of(level.dimension(), pos));
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) {
			Waystones.openList(serverPlayer, state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below());
		}
		return InteractionResult.SUCCESS;
	}
}
