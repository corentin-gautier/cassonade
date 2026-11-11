package com.cassonade.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * Bridging without leaning over the edge: sneaking, aiming at the far side of the top of the block underfoot, a block
 * in hand goes in front of it, as if its far face had been clicked. A solid block could not go on top anyway, the player
 * being there; what has no collision (a torch, a carpet) stays where aimed. The server only checks the reach, not the
 * line of sight, so it works on any server.
 */
public final class SafeBridging {
	/** How far from the block's center the aim has to be, towards where the player looks. */
	private static final double FAR_SIDE = 0.2;
	// Low on the face, so that a slab or stairs stay at the bridge's level
	private static final double FACE_HEIGHT = 0.25;

	private SafeBridging() {
	}

	public static BlockHitResult redirect(LocalPlayer player, ItemStack stack, BlockHitResult hit) {
		if (!Settings.on(Setting.SAFE_BRIDGING) || !player.isShiftKeyDown() || !player.onGround() || hit.getDirection() != Direction.UP || !(stack.getItem() instanceof BlockItem item)) {
			return hit;
		}
		BlockPos pos = hit.getBlockPos();
		Level level = player.level();
		Direction forward = player.getDirection();
		Vec3 aim = hit.getLocation().subtract(Vec3.atCenterOf(pos));
		BlockPos target = pos.relative(forward);
		if (!pos.equals(player.getOnPos()) || aim.x * forward.getStepX() + aim.z * forward.getStepZ() < FAR_SIDE
				|| !level.getBlockState(target).canBeReplaced()
				|| item.getBlock().defaultBlockState().getCollisionShape(level, target).isEmpty()) {
			return hit;
		}
		Vec3 face = Vec3.atLowerCornerOf(pos).add(0.5 + 0.5 * forward.getStepX(), FACE_HEIGHT, 0.5 + 0.5 * forward.getStepZ());
		return new BlockHitResult(face, forward, pos, false);
	}
}
