package com.cassonade.client;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * With a piston, an observer, a dispenser, a dropper or a hopper in hand, a small dark cross framed by four corners floats on the
 * face of the block to come that does the work: the one a piston pushes through, a dispenser or a dropper ejects
 * through, a hopper pours through, an observer watches through. Asks the block itself what it would do, on the
 * client, so it works on any server. Drawn as the game's gizmos, like the sculk torch's faces (SpawnSpots).
 */
public final class FacingPreview {
	private static final Set<Block> BLOCKS = Set.of(Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.OBSERVER, Blocks.DISPENSER, Blocks.DROPPER, Blocks.HOPPER);
	/** The center mark, three of a block texture's pixels wide. */
	private static final String[] CROSS = {
			".X.",
			"XXX",
			".X.",
	};
	/** A corner of the frame, from its tip: the corners frame the cross, so that it doesn't read as a "forbidden" sign. */
	private static final int[][] CORNER = {{0, 0}, {1, 0}, {0, 1}};
	private static final int FRAME_PIXELS = 8;
	private static final double PIXEL = 1.0 / 16;
	/** Just inside the block, so as not to flicker against a wall. */
	private static final double INSET = 0.01;
	/** The shade of the sculk torch's faces. */
	private static final int COLOR = 0x66000000;

	/** A block about to be placed: where, and the way it would point. */
	public record Placement(BlockPos pos, Direction facing) {
	}

	private static Placement current;

	private FacingPreview() {
	}

	public static void tick(Minecraft minecraft) {
		current = minecraft.player == null || minecraft.level == null ? null : find(minecraft);
		if (current != null) {
			mark(current);
		}
	}

	private static void mark(Placement placement) {
		Direction facing = placement.facing();
		Direction first = facing.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP;
		Vec3 up = unit(first).scale(PIXEL);
		Vec3 right = unit(facing).cross(unit(first)).scale(PIXEL);
		Vec3 center = Vec3.atCenterOf(placement.pos()).add(unit(facing).scale(0.5 - INSET));
		for (int row = 0; row < CROSS.length; row++) {
			for (int column = 0; column < CROSS[row].length(); column++) {
				char pixel = CROSS[row].charAt(column);
				if (pixel != '.') {
					pixel(center, right, up, column - CROSS.length / 2.0, CROSS.length / 2.0 - row - 1, COLOR);
				}
			}
		}
		for (int horizontal : new int[] {-1, 1}) {
			for (int vertical : new int[] {-1, 1}) {
				for (int[] tip : CORNER) {
					// From the corner inward
					double x = horizontal < 0 ? -FRAME_PIXELS / 2.0 + tip[0] : FRAME_PIXELS / 2.0 - 1 - tip[0];
					double y = vertical < 0 ? -FRAME_PIXELS / 2.0 + tip[1] : FRAME_PIXELS / 2.0 - 1 - tip[1];
					pixel(center, right, up, x, y, COLOR);
				}
			}
		}
	}

	/** A texture pixel of the face, x and y counted in pixels from its center. */
	private static void pixel(Vec3 center, Vec3 right, Vec3 up, double x, double y, int color) {
		Vec3 a = center.add(right.scale(x)).add(up.scale(y));
		Gizmos.rect(a, a.add(right), a.add(right).add(up), a.add(up), GizmoStyle.fill(color));
	}

	private static Vec3 unit(Direction direction) {
		return Vec3.atLowerCornerOf(direction.getUnitVec3i());
	}

	/** What is previewed right now, null if nothing. */
	public static Placement current() {
		return current;
	}

	private static Placement find(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		InteractionHand hand = isPreviewed(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		ItemStack stack = player.getItemInHand(hand);
		if (!isPreviewed(stack)) {
			return null;
		}
		// A click on a chest opens it rather than placing anything, unless sneaking
		if (!player.isSecondaryUseActive() && minecraft.level.getBlockState(hit.getBlockPos()).getMenuProvider(minecraft.level, hit.getBlockPos()) != null) {
			return null;
		}
		BlockPlaceContext context = new BlockPlaceContext(player, hand, stack, hit);
		BlockState state = ((BlockItem) stack.getItem()).getBlock().getStateForPlacement(context);
		if (state == null || !context.canPlace() || !state.canSurvive(minecraft.level, context.getClickedPos())
				|| !minecraft.level.isUnobstructed(state, context.getClickedPos(), CollisionContext.placementContext(player))) {
			return null;
		}
		Property<Direction> facing = state.getBlock() instanceof HopperBlock ? HopperBlock.FACING : BlockStateProperties.FACING;
		return new Placement(context.getClickedPos(), state.getValue(facing));
	}

	private static boolean isPreviewed(ItemStack stack) {
		return stack.getItem() instanceof BlockItem item && BLOCKS.contains(item.getBlock());
	}
}
