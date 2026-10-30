package com.cassonade.tweaks;

import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Where a player gets off a boat or a minecart: beside the vehicle, as close as possible to the side they look at. */
public final class VehicleExit {
	private static final List<int[]> SIDES = List.of(new int[] {1, 0}, new int[] {1, 1}, new int[] {0, 1}, new int[] {-1, 1},
			new int[] {-1, 0}, new int[] {-1, -1}, new int[] {0, -1}, new int[] {1, -1});
	private static final int[] HEIGHTS = {0, 1, -1};

	private VehicleExit() {
	}

	/** Null for anything but a player, or with no free spot around: vanilla then decides. */
	public static Vec3 find(Entity vehicle, LivingEntity passenger) {
		if (!(passenger instanceof Player)) {
			return null;
		}
		float yaw = passenger.getYRot() * Mth.DEG_TO_RAD;
		double lookX = -Mth.sin(yaw);
		double lookZ = Mth.cos(yaw);
		// Just clear of the vehicle's box, which the player would otherwise be pushed out of
		double reach = (vehicle.getBbWidth() + passenger.getBbWidth()) / 2 + 0.01;
		List<int[]> sides = SIDES.stream()
				.sorted(Comparator.comparingDouble(side -> -(side[0] * lookX + side[1] * lookZ) / Math.hypot(side[0], side[1])))
				.toList();
		for (Pose pose : passenger.getDismountPoses()) {
			for (int[] side : sides) {
				for (int height : HEIGHTS) {
					Vec3 spot = spot(vehicle, passenger, pose, vehicle.getX() + side[0] * reach, vehicle.blockPosition().getY() + height, vehicle.getZ() + side[1] * reach);
					if (spot != null) {
						passenger.setPose(pose);
						return spot;
					}
				}
			}
		}
		return null;
	}

	private static Vec3 spot(Entity vehicle, LivingEntity passenger, Pose pose, double x, int y, double z) {
		Level level = vehicle.level();
		BlockPos pos = BlockPos.containing(x, y, z);
		double floor = level.getBlockFloorHeight(DismountHelper.nonClimbableShape(level, pos), () -> DismountHelper.nonClimbableShape(level, pos.below()));
		if (!DismountHelper.isBlockFloorValid(floor) || !level.getFluidState(pos).isEmpty()
				|| passenger.getType().isBlockDangerous(level.getBlockState(floor > 0 ? pos : pos.below()))) {
			return null;
		}
		Vec3 spot = new Vec3(x, y + floor, z);
		return DismountHelper.canDismountTo(level, spot, passenger, pose) ? spot : null;
	}
}
